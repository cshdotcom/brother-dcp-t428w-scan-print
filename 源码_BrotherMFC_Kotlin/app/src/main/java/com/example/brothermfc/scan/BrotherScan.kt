package com.example.brothermfc.scan

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Brother 网络扫描（TCP 54921）—— 按原厂 SDK 字节码实现（v0.2）。
 *
 * 协议来源：iPrint&Scan 6.14.2（com.brother.mfc.brprint）内
 *   com.brother.sdk.common.socket.scan.scancommand.* 的 dalvik 字节码。
 *
 * 命令序列（**不是** ESC K，K 只适用于 2010 年前后的老机型如 DCP-J105）：
 *   ESC 'D' 0A "AUTO\n"                        80   设置纸源        回 1 字节，0x80 = OK
 *   ESC 'E' 0A "R=..\nM=..\n"                   80   初始探测        回 [0x00][len u16 LE][键值文本]
 *   ESC 'I' 0A "R=..\nM=..\nD=SIN\n"            80   下发参数        回 [0x00][len u16 LE][7 个整数]
 *   ESC 'X' 0A R/M/C/J/B/N/A/D/G/L              80   执行扫描        之后进图像流
 *   ESC 'Q' 0A ...                              80   查询能力（可选，解析 ProtocolGeneration 等）
 *
 * 图像流：1 字节头 h
 *   h & 0x80 == 0 → 数据块：压缩 = h & 3（0 NONE / 1 RLENGTH / 2 MH / 3 JPEG），
 *                            色彩 = (h >> 2) & 7，bit5 置位表示 JPEG
 *                   随后 2 字节小端块长 + 负载
 *   h & 0x80 != 0 → 控制头（0x80..0x87：EdgeInformation / 页结束 / 全部结束等），
 *                   随后 2 字节小端长度 + 数据（长度含义按类型不同）
 */
object BrotherScan {

    private const val PORT = 54921
    private const val ESC = 0x1B
    private const val END = 0x80

    const val MODE_TEXT = "TEXT"
    const val MODE_CGRAY = "CGRAY"
    const val MODE_GRAY256 = "GRAY256"
    const val MODE_C256 = "C256"
    const val MODE_ERRDIF = "ERRDIF"

    data class Params(
        val resolution: Int = 200,          // 100 / 200 / 300 / 600
        val mode: String = MODE_CGRAY,      // 彩色用 C256
        val brightness: Int = 50,           // 0..100
        val contrast: Int = 50,             // 0..100
        val compression: String = "JPEG",   // NONE / RLENGTH / MH / JPEG
        val quality: String = "MID",
        val paperSource: String = "AUTO",   // AUTO / ADF
        val duplex: String = "SIN",         // SIN / DUP
        val area: String? = null            // "0,0,宽,高"；null 用设备回报的尺寸
    )

    data class Page(val index: Int, val bytes: ByteArray, val format: String)

    data class Result(
        val ok: Boolean,
        val pages: List<Page>,
        val dpi: Pair<Int, Int>?,
        val size: Pair<Int, Int>?,
        val message: String,
        val savedFiles: List<String> = emptyList(),
        val trace: List<String> = emptyList()
    )

    fun scan(host: String, params: Params = Params(), timeoutMs: Int = 30000, outDir: File? = null): Result {
        var sock: Socket? = null
        val trace = ArrayList<String>()
        try {
            sock = Socket().apply { connect(InetSocketAddress(host, PORT), 6000); soTimeout = timeoutMs }
            val ins = sock.getInputStream()
            val outs = sock.getOutputStream()

            val welcome = readLine(ins)
            trace.add("welcome: $welcome")
            if (!welcome.startsWith("+")) {
                return Result(false, emptyList(), null, null, "设备未就绪：$welcome", trace = trace)
            }

            // 实测验证过的流程（2026-11-14）：welcome → 直接 ESC X → 图流
            val dpi = params.resolution
            val w = params.area?.split(",")?.getOrNull(2)?.toIntOrNull() ?: mm2px(210, dpi)
            val h = params.area?.split(",")?.getOrNull(3)?.toIntOrNull() ?: mm2px(297, dpi)
            val area = params.area ?: "0,0,$w,$h"

            val xb = StringBuilder()
            xb.append("R=").append(dpi).append(',').append(dpi).append('\n')
            xb.append("M=").append(params.mode).append('\n')
            xb.append("C=JPEG\n")
            xb.append("J=").append(params.quality).append('\n')
            xb.append("B=").append(params.brightness).append('\n')
            xb.append("N=").append(params.contrast).append('\n')
            xb.append("A=").append(area).append('\n')
            xb.append("D=").append(params.duplex).append('\n')
            xb.append("G=1\n")
            xb.append("L=128\n")
            sendFrame(outs, 'X'.code, xb.toString())
            trace.add("ESC X sent, A=$area")

            // 图流：10 字节头（0x64 JPEG / magic 0x0007 / pageId / 0x84 / progress / 0,0）
            //       + 2 字节小端长度 + 数据；FFD9 结束 JPEG；0x82 本页结束
            val pages = ArrayList<Page>()
            val saved = ArrayList<String>()
            var pageIndex = 0
            var cur = ByteArrayOutputStream()
            val curFormat = "jpg"
            var guard = 0

            stream@ while (guard++ < 100000) {
                val hdr = readN(ins, 10) ?: break
                if (hdr.size < 10) break
                val id = hdr[0].toInt() and 0xFF
                if (id == 0x80) break
                val magic = (hdr[1].toInt() and 0xFF) or ((hdr[2].toInt() and 0xFF) shl 8)
                val pageId = (hdr[3].toInt() and 0xFF) or ((hdr[4].toInt() and 0xFF) shl 8)
                val progress = (hdr[6].toInt() and 0xFF) or ((hdr[7].toInt() and 0xFF) shl 8)
                if (magic != 0x0007) {
                    trace.add("包 magic 异常 $magic")
                    break
                }
                if (id == 0x82) {
                    if (cur.size() > 0) {
                        val page = Page(pageIndex, cur.toByteArray(), curFormat)
                        pages.add(page)
                        outDir?.let {
                            it.mkdirs()
                            val f = File(it, "scan_$pageIndex.$curFormat")
                            f.writeBytes(page.bytes)
                            saved.add(f.absolutePath)
                        }
                        pageIndex++
                    }
                    cur = ByteArrayOutputStream()
                    continue
                }
                val lenB = readN(ins, 2) ?: break
                if (lenB.size < 2) break
                val len = (lenB[0].toInt() and 0xFF) or ((lenB[1].toInt() and 0xFF) shl 8)
                trace.add("块 id=0x${id.toString(16)} page=$pageId 进度=$progress len=$len")
                var left = len
                while (left > 0) {
                    val chunk = readN(ins, minOf(left, 4096)) ?: break@stream
                    if (chunk.isEmpty()) break@stream
                    cur.write(chunk)
                    left -= chunk.size
                }
            }
            if (cur.size() > 0) {
                val page = Page(pageIndex, cur.toByteArray(), curFormat)
                pages.add(page)
                outDir?.let {
                    it.mkdirs()
                    val f = File(it, "scan_$pageIndex.$curFormat")
                    f.writeBytes(page.bytes)
                    saved.add(f.absolutePath)
                }
            }

            val ok = pages.isNotEmpty()
            return Result(
                ok, pages, dpi to dpi, w to h,
                if (ok) "完成 ${pages.size} 页" else "未收到图像数据（平板未放稿 / 会话被占用 / 命令被拒）",
                saved, trace
            )
        } catch (e: Exception) {
            return Result(false, emptyList(), null, null,
                "扫描异常：${e.javaClass.simpleName} ${e.message}", trace = trace)
        } finally {
            runCatching { sock?.close() }
        }
    }

    /** 只看握手 + ESC D + ESC E，用于排障 */
    fun probe(host: String, timeoutMs: Int = 6000): Result {
        var sock: Socket? = null
        val trace = ArrayList<String>()
        try {
            sock = Socket().apply { connect(InetSocketAddress(host, PORT), 6000); soTimeout = timeoutMs }
            val ins = sock.getInputStream()
            val outs = sock.getOutputStream()
            val welcome = readLine(ins)
            trace.add("welcome: $welcome")
            if (!welcome.startsWith("+")) return Result(false, emptyList(), null, null, "设备未就绪：$welcome", trace = trace)

            sendFrame(outs, 'D'.code, "AUTO\n")
            trace.add("ESC D -> 0x${readStatus(ins).toString(16)}")

            sendFrame(outs, 'E'.code, "R=200,200\nM=CGRAY\n")
            val st = readStatus(ins)
            trace.add("ESC E -> 0x${st.toString(16)}")
            if (st == 0) trace.add("ESC E payload: ${String(readPayload(ins), Charsets.US_ASCII)}")
            return Result(true, emptyList(), null, null, "探测完成", trace = trace)
        } catch (e: Exception) {
            return Result(false, emptyList(), null, null, "探测异常：${e.javaClass.simpleName} ${e.message}", trace = trace)
        } finally {
            runCatching { sock?.close() }
        }
    }

    // ---------------- 底层 ----------------

    /**
     * ESC I 回包：代次 >= 2009 是 [长度 u16 小端][csv]，无状态字节；老代次是 [0x00][长度][csv]。
     */
    private fun readParamReply(ins: InputStream, trace: MutableList<String>): ByteArray {
        val b1 = ins.read()
        if (b1 < 0) return ByteArray(0)
        if (b1 == 0x00) {
            trace.add("ESC I: 状态字节分支（老代次）")
            val lb = readN(ins, 2) ?: return ByteArray(0)
            val len = (lb[0].toInt() and 0xFF) or ((lb[1].toInt() and 0xFF) shl 8)
            return readN(ins, len) ?: ByteArray(0)
        }
        val b2 = ins.read()
        if (b2 < 0) return ByteArray(0)
        val len = (b1 and 0xFF) or ((b2 and 0xFF) shl 8)
        trace.add("ESC I: 长度优先分支 len=$len")
        if (len < 3 || len > 4096) {
            val lb = readN(ins, 2) ?: return ByteArray(0)
            val l2 = (lb[0].toInt() and 0xFF) or ((lb[1].toInt() and 0xFF) shl 8)
            return readN(ins, l2) ?: ByteArray(0)
        }
        return readN(ins, len) ?: ByteArray(0)
    }

    private fun sendFrame(outs: OutputStream, cmd: Int, paramText: String) {
        outs.write(byteArrayOf(ESC.toByte(), cmd.toByte(), 0x0A.toByte()))
        outs.write(paramText.toByteArray(Charsets.US_ASCII))
        outs.write(byteArrayOf(END.toByte()))
        outs.flush()
    }

    /** 读应答首字节；-1 表示流结束 */
    private fun readStatus(ins: InputStream): Int {
        val b = ins.read()
        return if (b < 0) -1 else b and 0xFF
    }

    /** 状态字节之后：长度 u16 小端 + 数据 */
    private fun readPayload(ins: InputStream): ByteArray {
        val lb = readN(ins, 2) ?: return ByteArray(0)
        val len = (lb[0].toInt() and 0xFF) or ((lb[1].toInt() and 0xFF) shl 8)
        return readN(ins, len) ?: ByteArray(0)
    }

    private fun mm2px(mm: Double, dpi: Int): Int = Math.round(mm / 25.4 * dpi).toInt()

    private fun readN(ins: InputStream, n: Int): ByteArray? {
        if (n <= 0) return ByteArray(0)
        val out = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = ins.read(out, read, n - read)
            if (r < 0) return if (read == 0) null else out.copyOf(read)
            read += r
        }
        return out
    }

    private fun readLine(ins: InputStream): String {
        val sb = StringBuilder()
        while (true) {
            val b = ins.read()
            if (b < 0 || b == 0x0A) break
            if (b != 0x0D) sb.append(b.toInt().toChar())
        }
        return sb.toString()
    }
}

package com.example.brothermfc.print

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * 打印客户端。依据 iPrint&Scan 6.14.2 内 SDK 的传输枚举（SocketClient$ProtocolType）：
 *   Port9100 / IPP / LPR / WSPrint / PRINT_iPSv1
 * 打印 PDL（PrinterPDL 枚举实测）：
 *   PWGRaster、BrJpc_PWGRaster、PCL_BrotherMonochrome、PostScript、Jpeg_BH9/BH11/BHS13/BHmini11
 * 喷墨一体机（DCP-T428W 属 PRINT_INKJET）标准走 PWG Raster；不支持时回落 JPEG 直打。
 */
object PrintClient {

    // ---------------- 打印状态（PrintState 枚举实测） ----------------

    enum class PrintState {
        SUCCESS,
        ERROR_PRINT,
        ERROR_PRINT_CANCEL_JOB,
        ERROR_PRINT_CONNECTION_FAILURE,
        ERROR_PRINT_CREATE_PDL_FAILURE,
        ERROR_PRINT_INVALID_ARGUMENT,
        ERROR_PRINT_NO_RESPONSE,
        ERROR_PRINT_NO_SUPPORTED_PARAMETER,
        ERROR_PRINT_NO_SUPPORTED_PROTOCOL,
        ERROR_ES_CONNECTION_FAILURE,
        ERROR_ES_NO_DEFINE,
        ERROR_ES_NO_SD_CARD,
        ERROR_ES_OUT_OF_MEMORY,
        ERROR_ES_OVER_HEAT,
        ERROR_ES_PAPER_EMPTY,
        ERROR_GCP_CONNECTION_FAILURE,
        ERROR_GCP_NOT_EXIST,
        ERROR_GCP_OFFLINE;

        companion object {
            fun from(code: String): PrintState = entries.find { it.name.equals(code, true) } ?: ERROR_PRINT
        }
    }

    /** 文档格式：image/pwg-raster（推荐）/ image/jpeg / application/pdf */
    data class Doc(val data: ByteArray, val format: String, val name: String = "AndroidJob")

    data class PrintResult(val transport: String, val code: Int, val state: PrintState, val message: String, val jobUri: String?)

    // ---------------- 通道一：9100 RAW（JetDirect） ----------------

    fun printVia9100(host: String, doc: Doc, port: Int = 9100, wrapPjl: Boolean = false): PrintResult = try {
        val body = if (wrapPjl) Raw9100.wrapPjl("PWG", doc.data) else doc.data
        Socket().use { s ->
            s.connect(InetSocketAddress(host, port), 8000)
            s.getOutputStream().use { out ->
                out.write(body); out.flush()
            }
        }
        PrintResult("Port9100", 0, PrintState.SUCCESS, "RAW 已发送 ${doc.data.size} bytes", null)
    } catch (e: Exception) {
        PrintResult("Port9100", -1, PrintState.ERROR_PRINT_CONNECTION_FAILURE, e.message ?: "", null)
    }

    // ---------------- 通道二：IPP Print-Job（端口 631，也可走 80） ----------------

    fun printViaIpp(host: String, doc: Doc, port: Int = 631, path: String = "/ipp/print"): PrintResult {
        val head = buildIppRequest("ipp://$host:$port$path", doc)
        val conn = (URL("http://$host:$port$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true
            connectTimeout = 5000; readTimeout = 25000
            setRequestProperty("Content-Type", "application/ipp")
            setRequestProperty("Accept", "application/ipp")
            setChunkedStreamingMode(4096)
        }
        DataOutputStream(conn.outputStream).use { out -> out.write(head); out.write(doc.data) }
        val http = conn.responseCode
        val body = try { conn.inputStream.readBytes() } catch (_: Exception) { conn.errorStream?.readBytes() ?: ByteArray(0) }
        val (status, jobUri) = parseIpp(body)
        val state = if (status == 0x0000) PrintState.SUCCESS else PrintState.ERROR_PRINT
        return PrintResult("IPP", http, state, Ipp.statusName(status), jobUri)
    }

    // ---------------- IPP 报文构造 ----------------

    private fun buildIppRequest(printerUri: String, doc: Doc): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(0x02); out.write(0x00)
        out.write(0x00); out.write(0x02)          // Print-Job
        out.write(intBE(1))                        // request-id
        out.write(0x01)                            // operation-attributes-tag
        Ipp.attr(out, 0x47, "attributes-charset", "utf-8")
        Ipp.attr(out, 0x48, "attributes-natural-language", "en")
        Ipp.attr(out, 0x45, "printer-uri", printerUri)
        Ipp.attr(out, 0x42, "requesting-user-name", "android")
        Ipp.attr(out, 0x42, "job-name", doc.name)
        Ipp.attr(out, 0x49, "document-format", doc.format)
        out.write(0x03)
        return out.toByteArray()
    }

    private fun parseIpp(resp: ByteArray): Pair<Int, String?> {
        if (resp.size < 8) return -1 to null
        val status = ((resp[2].toInt() and 0xFF) shl 8) or (resp[3].toInt() and 0xFF)
        var jobUri: String? = null
        var i = 8
        while (i + 2 < resp.size) {
            val tag = resp[i].toInt() and 0xFF
            if (tag == 0x03) break
            if (tag < 0x10) { i++; continue }
            val nl = ((resp[i + 1].toInt() and 0xFF) shl 8) or (resp[i + 2].toInt() and 0xFF)
            i += 3
            if (i + nl > resp.size) break
            val name = String(resp, i, nl); i += nl
            if (i + 2 > resp.size) break
            val vl = ((resp[i].toInt() and 0xFF) shl 8) or (resp[i + 1].toInt() and 0xFF)
            i += 2
            val value = if (i + vl <= resp.size) String(resp, i, vl) else ""
            i += vl
            if (name == "job-uri") jobUri = value
        }
        return status to jobUri
    }

    private fun intBE(v: Int): ByteArray = byteArrayOf(
        ((v shr 24) and 0xFF).toByte(), ((v shr 16) and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(), (v and 0xFF).toByte()
    )

    /** 9100 PJL 包裹（语言名按 PDL 替换） */
    object Raw9100 {
        fun wrapPjl(language: String, data: ByteArray): ByteArray {
            val head = ("\u001B%-12345X@PJL\n@PJL JOB NAME=\"AndroidJob\"\n" +
                    "@PJL ENTER LANGUAGE=$language\n").toByteArray()
            return head + data + "\u001B%-12345X".toByteArray()
        }
    }

    object Ipp {
        fun attr(out: ByteArrayOutputStream, valueTag: Int, name: String, value: String) {
            val n = name.toByteArray(); val v = value.toByteArray()
            out.write(valueTag)
            out.write(n.size shr 8); out.write(n.size and 0xFF); out.write(n)
            out.write(v.size shr 8); out.write(v.size and 0xFF); out.write(v)
        }
        fun statusName(s: Int): String = when (s) {
            0x0000 -> "successful-ok"
            0x0405 -> "document-format-not-supported"
            0x0406 -> "attributes-or-values-not-supported"
            0x0500 -> "server-error-internal"
            0x0507 -> "busy"
            else -> "0x%04x".format(s)
        }
    }
}

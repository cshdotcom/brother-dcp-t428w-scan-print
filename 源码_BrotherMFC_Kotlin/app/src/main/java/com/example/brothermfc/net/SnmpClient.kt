package com.example.brothermfc.net

import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * 极简 SNMP v1/v2c GET 客户端（仅标准库，无第三方依赖）。
 * 用途：读 Brother 一体机状态 / 耗材 / 型号，对应 APK 里 com.brother.mfc.mfcpcontrol.mib.brim.*
 */
object SnmpClient {

    private const val TAG_SEQ = 0x30
    private const val TAG_INT = 0x02
    private const val TAG_OCT = 0x04
    private const val TAG_OID = 0x06
    private const val TAG_NULL = 0x05
    private const val TAG_PDU_GET = 0xA0

    /** 常用的几个查询 OID（Brother 私有 MIB，命中率最高的一组） */
    val DEFAULT_OIDS = listOf(
        "1.3.6.1.2.1.1.1.0",           // sysDescr：型号/固件串
        "1.3.6.1.2.1.1.5.0",           // sysName
        "1.3.6.1.4.1.2435.2.3.9.1.0",  // Brother 通用状态
        "1.3.6.1.2.1.43.11.1.1.9.1",   // 耗材剩余量（prtMarkerSuppliesLevel）
        "1.3.6.1.2.1.43.11.1.1.6.1"    // 耗材描述
    )

    fun get(host: String, community: String = "public", oids: List<String> = DEFAULT_OIDS, timeoutMs: Int = 1500): String {
        val req = buildRequest(community, oids)
        DatagramSocket().use { sock ->
            sock.soTimeout = timeoutMs
            val addr = InetAddress.getByName(host)
            sock.send(DatagramPacket(req, req.size, addr, 161))
            val buf = ByteArray(4096)
            val pkt = DatagramPacket(buf, buf.size)
            sock.receive(pkt)
            val resp = pkt.data.copyOf(pkt.length)
            return decode(resp)
        }
    }

    // ---------------- 编码 ----------------

    private fun buildRequest(community: String, oids: List<String>): ByteArray {
        val varbinds = ByteArrayOutputStream()
        for (oid in oids) {
            val vb = ByteArrayOutputStream()
            vb.write(tlv(TAG_OID, oidBytes(oid)))
            vb.write(tlv(TAG_NULL, ByteArray(0)))
            varbinds.write(tlv(TAG_SEQ, vb.toByteArray()))
        }
        val pdu = ByteArrayOutputStream()
        pdu.write(tlv(TAG_INT, intBytes(1)))        // request-id
        pdu.write(tlv(TAG_INT, intBytes(0)))        // error-status
        pdu.write(tlv(TAG_INT, intBytes(0)))        // error-index
        pdu.write(tlv(TAG_SEQ, varbinds.toByteArray()))
        val pduTlv = tlv(TAG_PDU_GET, pdu.toByteArray())

        val msg = ByteArrayOutputStream()
        msg.write(tlv(TAG_INT, intBytes(0)))         // version: v1
        msg.write(tlv(TAG_OCT, community.toByteArray()))
        msg.write(pduTlv)
        return tlv(TAG_SEQ, msg.toByteArray())
    }

    private fun oidBytes(oid: String): ByteArray {
        val parts = oid.split(".").map { it.toInt() }
        val out = ByteArrayOutputStream()
        out.write(parts[0] * 40 + parts[1])
        for (p in parts.drop(2)) {
            if (p < 0x80) out.write(p)
            else {
                val stack = ArrayList<Int>()
                var v = p
                stack.add(v and 0x7F)
                v = v shr 7
                while (v > 0) { stack.add((v and 0x7F) or 0x80); v = v shr 7 }
                for (i in stack.indices.reversed()) out.write(stack[i])
            }
        }
        return out.toByteArray()
    }

    private fun intBytes(v: Int): ByteArray {
        val b = ByteArrayOutputStream()
        if (v < 256) b.write(v) else { b.write((v shr 8) and 0xFF); b.write(v and 0xFF) }
        return b.toByteArray()
    }

    private fun tlv(tag: Int, content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(tag)
        val len = content.size
        when {
            len < 0x80 -> out.write(len)
            len < 0x100 -> { out.write(0x81); out.write(len) }
            else -> { out.write(0x82); out.write((len shr 8) and 0xFF); out.write(len and 0xFF) }
        }
        out.write(content)
        return out.toByteArray()
    }

    // ---------------- 解码 ----------------

    private fun decode(data: ByteArray): String {
        val sb = StringBuilder()
        // 粗暴但有效的做法：顺序扫描 TLV，收集 OID 与其后的值
        var i = 0
        var pendingOid: String? = null
        while (i < data.size - 1) {
            val tag = data[i].toInt() and 0xFF
            val lenB = data[i + 1].toInt() and 0xFF
            val (len, hdr) = when {
                lenB < 0x80 -> lenB to 2
                lenB == 0x81 -> (data[i + 2].toInt() and 0xFF) to 3
                lenB == 0x82 -> ((data[i + 2].toInt() and 0xFF shl 8) or (data[i + 3].toInt() and 0xFF)) to 4
                else -> return "解码失败：长度域异常"
            }
            val start = i + hdr
            val end = minOf(start + len, data.size)
            if (start >= end) break
            when (tag) {
                0x30, 0xA0, 0xA1, 0xA2 -> { i += hdr; continue }   // 结构体：进入内层
                TAG_OID -> pendingOid = decodeOid(data.copyOfRange(start, end))
                TAG_OCT -> {
                    val v = String(data.copyOfRange(start, end))
                    if (pendingOid != null) { sb.append("$pendingOid = $v\n"); pendingOid = null }
                }
                TAG_INT -> {
                    var v = 0
                    for (b in data.copyOfRange(start, end)) v = (v shl 8) or (b.toInt() and 0xFF)
                    if (pendingOid != null) { sb.append("$pendingOid = $v\n"); pendingOid = null }
                }
                0x41, 0x42, 0x43, 0x44 -> { // Counter32/Gauge32/TimeTicks
                    var v = 0L
                    for (b in data.copyOfRange(start, end)) v = (v shl 8) or (b.toLong() and 0xFF)
                    if (pendingOid != null) { sb.append("$pendingOid = $v\n"); pendingOid = null }
                }
            }
            i = end
        }
        return if (sb.isEmpty()) "SNMP 响应解析为空，原始长度 ${data.size}" else sb.toString().trim()
    }

    private fun decodeOid(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append(bytes[0] / 40).append(".").append(bytes[0] % 40)
        var v = 0
        for (b in bytes.drop(1)) {
            v = (v shl 7) or (b.toInt() and 0x7F)
            if ((b.toInt() and 0x80) == 0) { sb.append(".").append(v); v = 0 }
        }
        return sb.toString()
    }
}

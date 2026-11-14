package com.example.brothermfc

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.brothermfc.net.MdnsDiscovery
import com.example.brothermfc.net.SnmpClient
import com.example.brothermfc.print.AndroidPrint
import com.example.brothermfc.print.PrintClient
import com.example.brothermfc.scan.BrotherScan
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 调试入口，不是成品 APP。
 * 只保留两条链路：扫描（54921 原生协议，已按真实帧实现）、打印（系统打印框架 + 兄弟插件）。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var etIp: EditText
    private val ui = Handler(Looper.getMainLooper())
    private val mdns by lazy { MdnsDiscovery(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvLog = findViewById(R.id.tvLog)
        etIp = findViewById(R.id.etIp)

        findViewById<Button>(R.id.btnDiscover).setOnClickListener { discover() }
        findViewById<Button>(R.id.btnSnmp).setOnClickListener { bg { snmp(ip()) } }
        findViewById<Button>(R.id.btnPrintRaw).setOnClickListener { printSystem() }
        findViewById<Button>(R.id.btnPrintIpp).setOnClickListener { bg { printRawExperimental(ip()) } }
        findViewById<Button>(R.id.btnScan).setOnClickListener { bg { scan(ip()) } }

        log("就绪：扫描走 54921 私有协议；打印走系统打印框架（需装兄弟打印服务插件）")
    }

    private fun ip() = etIp.text.toString().trim()
    private fun bg(block: () -> Unit) =
        Thread { runCatching(block).onFailure { log("异常：${it.message}") } }.start()

    private fun log(msg: String) {
        val t = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        ui.post { tvLog.append("[$t] $msg\n") }
    }

    private fun discover() {
        log("mDNS 发现中…")
        mdns.start(
            onFound = { info -> mdns.resolve(info) { r -> log("发现 ${info.serviceName} → $r") } },
            onEnd = { e -> log(e) }
        )
    }

    private fun snmp(host: String) = log(SnmpClient.get(host))

    /** 打印主通道：系统打印框架 + 兄弟打印服务插件（PDL 由插件负责） */
    private fun printSystem() {
        val installed = AndroidPrint.isBrotherPluginInstalled(this)
        log(if (installed) "兄弟打印服务插件已安装，拉起系统打印面板" else "未检测到 com.brother.mfc.brprint.plugin，面板里可能看不到打印机")
        val pdf = AndroidPrint.textPdf("DCP-T428W 测试页\n由 Android 打印框架提交\n" + Date())
        AndroidPrint.printPdf(this, pdf, "BrotherMFC 测试页")
    }

    /** 实验通道：JPEG 直接打到 9100（需真机验证是否接受裸 JPEG） */
    private fun printRawExperimental(host: String) {
        val doc = prepareDoc() ?: run { log("未准备打印数据（JPEG/PDF 字节）"); return }
        val r = PrintClient.printVia9100(host, doc)
        log("9100: ${r.state} ${r.message}")
    }

    /** 扫描主通道：54921 */
    private fun scan(host: String) {
        val outDir = File(getExternalFilesDir(null), "scans")
        val params = BrotherScan.Params(resolution = 200, mode = BrotherScan.MODE_CGRAY)
        val r = BrotherScan.scan(host, params, outDir = outDir)
        val sb = StringBuilder("扫描：${if (r.ok) "成功" else "失败"} ${r.message}")
        r.dpi?.let { sb.append("，DPI=${it.first}x${it.second}") }
        r.size?.let { if (it.first > 0) sb.append("，像素=${it.first}x${it.second}") }
        r.savedFiles.forEach { sb.append("\n  → ").append(it) }
        log(sb.toString())
    }

    private fun prepareDoc(): PrintClient.Doc? = null
}

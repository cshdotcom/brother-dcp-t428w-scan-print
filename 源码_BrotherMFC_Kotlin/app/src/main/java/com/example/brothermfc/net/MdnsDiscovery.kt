package com.example.brothermfc.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

/**
 * mDNS 发现：兄弟一体机通常广播这几个服务
 *   _ipp._tcp          → IPP 打印
 *   _pdl-datastream._tcp → 9100 RAW
 *   _uscan._tcp / _uscans._tcp → 扫描服务（ScanCommand/WSD）
 *   _http._tcp         → 内嵌 Web（能力页、固件页）
 *   _printer._tcp
 */
class MdnsDiscovery(private val ctx: Context) {

    private val nsd = ctx.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val listeners = ArrayList<NsdManager.DiscoveryListener>()
    val found = LinkedHashMap<String, NsdServiceInfo>()

    fun start(onFound: (NsdServiceInfo) -> Unit, onEnd: (String) -> Unit) {
        val types = listOf("_ipp._tcp", "_pdl-datastream._tcp", "_uscan._tcp", "_http._tcp", "_printer._tcp")
        for (type in types) {
            val l = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    // 不过滤品牌：兄弟机器的 mDNS 名字常被用户在 EWS 里改过
                    found[info.serviceName] = info
                    onFound(info)
                }
                override fun onServiceLost(info: NsdServiceInfo) { found.remove(info.serviceName) }
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    onEnd("启动失败 $serviceType code=$errorCode")
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            }
            listeners.add(l)
            runCatching { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, l) }
        }
    }

    fun stop() {
        listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
        listeners.clear()
    }

    /** 解析 host/port（异步，回调返回 ip:port） */
    fun resolve(info: NsdServiceInfo, cb: (String) -> Unit) {
        nsd.resolveService(info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = cb("解析失败 code=$errorCode")
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress ?: "?"
                cb("$host:${serviceInfo.port}  (${serviceInfo.serviceType})")
            }
        })
    }
}

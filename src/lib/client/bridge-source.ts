'use client'

/**
 * 桥接程序下载源（可自定义的「桥接源」）：
 *
 * 桥接程序（Windows exe / Linux 二进制 / Android APK）默认从本站 public 目录下发。
 * 用户可在「设置 → 桥接服务」里把下载源改成任意 http(s) 基址（例如：
 *   - GitHub Release：https://github.com/cshdotcom/brother-dcp-t428w-scan-print/releases/download/v2.0
 *   - 局域网文件服务器：http://192.168.1.10/bridge
 *   - 本机静态目录：http://localhost:8000
 * ），所有下载按钮将自动指向 `${源}/文件名`。
 *
 * 存储策略：localStorage 立即生效；登录后由服务端 config 同步（跨设备一致）。
 */

const KEY = 'pm_bridge_source'

/** 桥接程序统一文件名（下载源目录下按这些名字查找） */
export const BRIDGE_FILES = {
  exe: 'papermarker-bridge.exe',
  linux: 'papermarker-bridge-linux',
  /** Android 桥接服务 APP */
  bridgeApk: 'PaperMarker-Bridge.apk',
  /** Android 主客户端 APP */
  appApk: 'papermarker.apk',
} as const

/** 读取自定义桥接下载源（空串 = 本站默认） */
export function getBridgeSource(): string {
  if (typeof window === 'undefined') return ''
  try {
    const v = localStorage.getItem(KEY)
    if (v && /^https?:\/\/.+/i.test(v.trim())) return v.trim().replace(/\/+$/, '')
  } catch {}
  return ''
}

/** 写入自定义桥接下载源（空串/非法 = 恢复本站默认） */
export function setBridgeSource(url: string): void {
  if (typeof window === 'undefined') return
  try {
    const clean = url.trim().replace(/\/+$/, '')
    if (!clean || !/^https?:\/\/.+/i.test(clean)) {
      localStorage.removeItem(KEY)
      return
    }
    localStorage.setItem(KEY, clean)
  } catch {}
}

/**
 * 拼接桥接文件下载地址：
 *  - 配置了下载源 → `${源}/${file}`
 *  - 未配置 → `/${file}`（本站 public 目录）
 */
export function bridgeFileUrl(file: string): string {
  const base = getBridgeSource()
  if (!base) return `/${file}`
  return `${base}/${file}`
}

import { NextRequest, NextResponse } from 'next/server'
import fs from 'fs'
import path from 'path'

export const runtime = 'nodejs'

/**
 * 桥接二进制下载（流式）：
 *   GET /api/download/papermarker-bridge.exe   → storage/bridges/papermarker-bridge.exe
 *   GET /api/download/papermarker-bridge-linux → storage/bridges/papermarker-bridge-linux
 *   GET /api/download/PaperMarker-Bridge.apk   → public/PaperMarker-Bridge.apk
 *   GET /api/download/papermarker.apk          → public/papermarker.apk
 *
 * 说明：大体积 exe/linux 不放在 public/（190MB 静态资产会拖慢/撑爆部署包），
 * 改为存储在 storage/bridges/ 由本路由流式下发；文件名白名单防路径穿越。
 * 注：不使用 node:stream/web 的 toWeb()（Turbopack 外部化后导入不稳定），
 * 改为手工 ReadableStream 适配 fs 流。
 */

const WHITELIST: Record<string, { dirs: string[]; mime: string }> = {
  'papermarker-bridge.exe': {
    dirs: ['storage/bridges', 'public'],
    mime: 'application/vnd.microsoft.portable-executable',
  },
  'papermarker-bridge-linux': {
    dirs: ['storage/bridges', 'public'],
    mime: 'application/octet-stream',
  },
  'PaperMarker-Bridge.apk': {
    dirs: ['public', 'storage/bridges'],
    mime: 'application/vnd.android.package-archive',
  },
  'papermarker.apk': {
    dirs: ['public', 'storage/bridges'],
    mime: 'application/vnd.android.package-archive',
  },
}

function safeStat(p: string): fs.Stats | null {
  try {
    const st = fs.statSync(p)
    return st.isFile() ? st : null
  } catch {
    return null
  }
}

/** fs.ReadStream → 标准 Web ReadableStream（含取消与错误传播） */
function fsToWeb(rs: fs.ReadStream): ReadableStream<Uint8Array> {
  let src = rs
  return new ReadableStream<Uint8Array>({
    start(controller) {
      src.on('data', (chunk: Buffer | string) => {
        controller.enqueue(typeof chunk === 'string' ? new TextEncoder().encode(chunk) : new Uint8Array(chunk))
      })
      src.on('end', () => {
        try {
          controller.close()
        } catch {}
      })
      src.on('error', (err) => {
        try {
          controller.error(err)
        } catch {}
      })
    },
    cancel() {
      try {
        src.destroy()
      } catch {}
    },
  })
}

function resolveFile(name: string): { filePath: string; size: number } | null {
  const entry = WHITELIST[name]
  if (!entry) return null
  const root = process.cwd()
  for (const dir of entry.dirs) {
    const p = path.join(root, dir, name)
    const st = safeStat(p)
    if (st) return { filePath: p, size: st.size }
  }
  return null
}

export async function GET(
  req: NextRequest,
  ctx: { params: Promise<{ file: string }> }
) {
  const { file } = await ctx.params

  // URL 编码还原 + 白名单校验（防路径穿越）
  let name = file
  try {
    name = decodeURIComponent(file)
  } catch {}
  const found = resolveFile(name)
  if (!found) {
    return NextResponse.json({ code: 404, msg: '文件不存在' }, { status: 404 })
  }
  const { filePath, size } = found

  const headersBase: Record<string, string> = {
    'Content-Type': WHITELIST[name].mime,
    'Content-Disposition': `attachment; filename="${name}"`,
    'Accept-Ranges': 'bytes',
    'Cache-Control': 'no-store',
  }

  // Range 支持（断点续传 / 下载管理器）
  const range = req.headers.get('range')
  if (range) {
    const m = /^bytes=(\d+)-(\d*)$/.exec(range.trim())
    if (m) {
      const start = parseInt(m[1], 10)
      const end = m[2] ? Math.min(parseInt(m[2], 10), size - 1) : size - 1
      if (start <= end && start < size) {
        return new Response(fsToWeb(fs.createReadStream(filePath, { start, end })), {
          status: 206,
          headers: {
            ...headersBase,
            'Content-Range': `bytes ${start}-${end}/${size}`,
            'Content-Length': String(end - start + 1),
          },
        })
      }
    }
    return new Response(null, { status: 416, headers: { 'Content-Range': `bytes */${size}` } })
  }

  return new Response(fsToWeb(fs.createReadStream(filePath)), {
    status: 200,
    headers: { ...headersBase, 'Content-Length': String(size) },
  })
}

// 支持 HEAD（设置页「测试」按钮用 HEAD 探测下载源可用性）
export async function HEAD(
  req: NextRequest,
  ctx: { params: Promise<{ file: string }> }
) {
  const { file } = await ctx.params
  const found = resolveFile(file)
  if (!found) return new Response(null, { status: 404 })
  return new Response(null, {
    status: 200,
    headers: {
      'Content-Type': WHITELIST[file].mime,
      'Content-Length': String(found.size),
      'Accept-Ranges': 'bytes',
    },
  })
}

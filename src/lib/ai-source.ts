/**
 * 自定义 AI 源（OpenAI 兼容接口）的解析 / 校验 / 连通性测试
 *
 * 配置存储于 User.configJson.aiSource：
 *   { enabled, baseUrl, apiKey, model, temperature? }
 *
 * 供 /api/ai/test 使用：
 *   - sanitizeAiSource(input)   校验并规范用户提交的配置
 *   - parseStoredAiSource(json) 从数据库 configJson 解析已保存配置
 *   - testAiSource(cfg)         发起最小连通性测试（文本 1-token 补全）
 */

export interface AiSourceValid {
  provider: 'openai'
  baseURL: string
  apiKey: string
  model: string
  temperature?: number
}

/** 规范 baseUrl：去尾部斜杠与空白 */
function cleanBaseUrl(raw: unknown): string {
  return String(raw ?? '')
    .trim()
    .replace(/\/+$/, '')
}

/**
 * 校验用户提交的 AI 源配置。
 * 合法 → 返回规范后的对象；不合法（缺 baseURL/model/key 或 URL 非法）→ 返回 null。
 */
export function sanitizeAiSource(input: unknown): AiSourceValid | null {
  if (!input || typeof input !== 'object') return null
  const s = input as Record<string, unknown>

  // 兼容两种字段命名：baseUrl（存储格式）/ baseURL（OpenAI 风格）
  const baseURL = cleanBaseUrl(s.baseUrl ?? s.baseURL)
  const apiKey = String(s.apiKey ?? '').trim()
  const model = String(s.model ?? '').trim()

  if (!baseURL || !apiKey || !model) return null
  if (!/^https?:\/\/.+/i.test(baseURL)) return null

  const out: AiSourceValid = { provider: 'openai', baseURL, apiKey, model }
  if (typeof s.temperature === 'number' && s.temperature >= 0 && s.temperature <= 2) {
    out.temperature = s.temperature
  }
  return out
}

/**
 * 从数据库 User.configJson 解析已保存的 AI 源。
 * 未开启 / 缺字段 / JSON 损坏 → 返回 null（回退内置模型）。
 */
export function parseStoredAiSource(configJson?: string | null): AiSourceValid | null {
  try {
    const cfg = JSON.parse(configJson || '{}')
    const s = cfg?.aiSource
    if (!s || typeof s !== 'object' || s.enabled !== true) return null
    return sanitizeAiSource(s)
  } catch {
    return null
  }
}

/** 从用户输入构造一个可展示的端点地址（日志/错误提示用，不带 key） */
function endpointOf(baseURL: string): string {
  return `${baseURL}/chat/completions`
}

/**
 * 连通性测试：向 `${baseURL}/chat/completions` 发起最小补全请求。
 * 纯文本、max_tokens=1，尽量少消耗额度。
 *
 * 返回 { ok, msg, latencyMs?, reply? }
 */
export async function testAiSource(
  cfg: AiSourceValid,
  timeoutMs = 20000
): Promise<{ ok: boolean; msg: string; latencyMs?: number; reply?: string }> {
  const url = endpointOf(cfg.baseURL)
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), timeoutMs)
  const t0 = Date.now()

  try {
    const res = await fetch(url, {
      method: 'POST',
      signal: ctrl.signal,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${cfg.apiKey}`,
      },
      body: JSON.stringify({
        model: cfg.model,
        messages: [{ role: 'user', content: '连接测试，请回复 OK' }],
        max_tokens: 5,
        temperature: 0,
        stream: false,
      }),
    })

    const latencyMs = Date.now() - t0
    const text = await res.text().catch(() => '')

    if (!res.ok) {
      // 常见错误给出可读说明
      let hint = ''
      if (res.status === 401 || res.status === 403) hint = '（API Key 无效或无权限）'
      else if (res.status === 404) hint = '（接口地址不对：应填到 /v1 这一级，不含 /chat/completions）'
      else if (res.status === 429) hint = '（额度不足或限流）'
      else if (res.status >= 500) hint = '（服务端错误，稍后再试）'
      const detail = text.slice(0, 300).replace(/\s+/g, ' ')
      return {
        ok: false,
        msg: `HTTP ${res.status}${hint}${detail ? `：${detail}` : ''}`,
        latencyMs,
      }
    }

    // 2xx：尽力解析出模型回复内容
    let reply = ''
    try {
      const data = JSON.parse(text)
      reply = String(data?.choices?.[0]?.message?.content ?? '').trim()
    } catch {
      reply = ''
    }

    if (!reply) {
      // 有些网关 2xx 但返回空体 —— 仍视为可达，但提示注意
      return {
        ok: true,
        msg: `连接成功（HTTP ${res.status}，${latencyMs}ms）。注意：未解析到回复内容，请确认该模型支持 Chat Completions。`,
        latencyMs,
      }
    }
    return {
      ok: true,
      msg: `连接成功（${latencyMs}ms）：${reply.slice(0, 60)}`,
      latencyMs,
      reply,
    }
  } catch (e: unknown) {
    const err = e as Error & { name?: string }
    const latencyMs = Date.now() - t0
    if (err?.name === 'AbortError') {
      return { ok: false, msg: `连接超时（${timeoutMs}ms）——检查接口地址与网络`, latencyMs }
    }
    return { ok: false, msg: `无法连接：${err?.message || err}`, latencyMs }
  } finally {
    clearTimeout(timer)
  }
}

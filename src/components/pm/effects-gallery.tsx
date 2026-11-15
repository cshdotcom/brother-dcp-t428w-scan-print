'use client'

/**
 * CSSFX 特效库展示页 —— 100 个纯 CSS 特效实时预览。
 *
 * 性能策略（避免「特效积累」卡顿）：
 *  - 每张卡片的两个演示窗用 IntersectionObserver 监视，
 *    只有进入视口才开始播放动画（视口外加 .fx-paused 暂停）。
 *  - 分页每次仅渲染 24 张，滚动再加载。
 *  - prefers-reduced-motion 用户自动暂停全部演示。
 */
import { useEffect, useMemo, useRef, useState } from 'react'
import { Search, Sparkles, Shuffle, Copy, Check, X } from 'lucide-react'
import { FX_EFFECTS, FX_CATEGORIES, FX_CATEGORY_CN, type FxEffect } from '@/lib/fx-effects'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/hooks/use-toast'

const PAGE_SIZE = 24

export function EffectsGallery({ onClose }: { onClose: () => void }) {
  const [cat, setCat] = useState<string>('All')
  const [q, setQ] = useState('')
  const [copied, setCopied] = useState<string | null>(null)
  const [cssMap, setCssMap] = useState<Record<string, string> | null>(null)
  const [visible, setVisible] = useState(PAGE_SIZE)
  const [randomId, setRandomId] = useState<string | null>(null)
  const { toast } = useToast()

  // 懒加载 CSS 源码（复制功能用）
  useEffect(() => {
    let cancelled = false
    fetch('/fx-css.json')
      .then((r) => r.json())
      .then((d) => {
        if (!cancelled) setCssMap(d)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  // 分类 / 搜索变化时重置分页（在事件回调中处理，避免 effect 内 setState）
  const switchCat = (c: string) => {
    setCat(c)
    setRandomId(null)
    setVisible(PAGE_SIZE)
  }
  const search = (v: string) => {
    setQ(v)
    setRandomId(null)
    setVisible(PAGE_SIZE)
  }

  const filtered = useMemo(() => {
    const kw = q.trim().toLowerCase()
    return FX_EFFECTS.filter((e) => {
      if (cat !== 'All' && e.category !== cat) return false
      if (randomId && e.id !== randomId) return false
      if (kw) {
        const hay = `${e.title} ${e.description} ${e.tags.join(' ')} ${e.id}`.toLowerCase()
        if (!hay.includes(kw)) return false
      }
      return true
    })
  }, [cat, q, randomId])

  const copyCss = async (e: FxEffect) => {
    const css = cssMap?.[e.id] || ''
    if (!css) {
      toast({ title: 'CSS 源码加载中，稍后再试', variant: 'destructive' })
      return
    }
    try {
      await navigator.clipboard.writeText(css)
      setCopied(e.id)
      setTimeout(() => setCopied((c) => (c === e.id ? null : c)), 1600)
      toast({ title: `已复制「${e.title}」CSS`, description: `${css.length} 字节 · 纯 CSS，无依赖` })
    } catch {
      toast({ title: '复制失败（浏览器限制）', variant: 'destructive' })
    }
  }

  const surprise = () => {
    const pick = FX_EFFECTS[Math.floor(Math.random() * FX_EFFECTS.length)]
    setRandomId(pick.id)
    setCat('All')
    setQ('')
    toast({ title: `手气不错：${FX_CATEGORY_CN[pick.category]} · ${pick.title}` })
  }

  const clearRandom = () => setRandomId(null)

  return (
    <div className="space-y-4">
      {/* 顶部横幅（深色 + 浮光球背景） */}
      <div className="fx-bg-orbs relative overflow-hidden rounded-2xl border p-6 sm:p-8">
        <div className="relative">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h1 className="fx-text-gradient !my-0 !text-3xl font-extrabold tracking-tight sm:!text-4xl">
                CSS 特效库
              </h1>
              <p className="mt-2 max-w-xl text-sm text-white/75">
                <span className="fx-text-shimmer fx-on-dark fx-text-sm !font-semibold">
                  100 个纯 CSS 特效 · 零依赖 · 实时预览
                </span>
                <br />
                全部来自 CSSFX 特效库并换肤为本站翠绿主题：按钮 / 加载动画 / 卡片 / 文字 / 背景 /
                悬停 / 边框 / 表单 / 杂项，点「复制 CSS」即可用到任何网页。
              </p>
            </div>
            <Button
              variant="ghost"
              size="icon"
              onClick={onClose}
              aria-label="关闭特效库"
              className="text-white/70 hover:bg-white/15 hover:text-white"
            >
              <X className="h-5 w-5" />
            </Button>
          </div>

          {/* 搜索 + 随机 */}
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <div className="relative min-w-[220px] flex-1 sm:max-w-xs">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-emerald-300" />
              <Input
                value={q}
                onChange={(e) => search(e.target.value)}
                placeholder="搜索特效、标签…"
                className="h-10 border-white/20 bg-white/10 pl-9 text-white placeholder:text-white/50"
                aria-label="搜索特效"
              />
            </div>
            <Button variant="outline" onClick={surprise} className="gap-1.5 border-white/25 bg-white/10 text-white hover:bg-white/20 hover:text-white">
              <Shuffle className="h-4 w-4" />
              手气不错
            </Button>
          </div>

          {/* 分类 chips（滑动下划线风格） */}
          <div className="mt-4 flex flex-wrap gap-1.5">
            <button
              onClick={() => switchCat('All')}
              className={`rounded-full px-3 py-1 text-xs font-semibold transition ${
                cat === 'All'
                ? 'bg-emerald-500 text-white shadow-sm'
                : 'bg-white/10 text-white/85 ring-1 ring-white/15 backdrop-blur hover:bg-white/25'
              }`}
            >
              全部 {FX_EFFECTS.length}
            </button>
            {FX_CATEGORIES.map((c) => {
              const n = FX_EFFECTS.filter((e) => e.category === c).length
              return (
                <button
                  key={c}
                  onClick={() => switchCat(c)}
                  className={`rounded-full px-3 py-1 text-xs font-semibold transition ${
                    cat === c
                    ? 'bg-emerald-500 text-white shadow-sm'
                    : 'bg-white/10 text-white/85 ring-1 ring-white/15 backdrop-blur hover:bg-white/25'
                  }`}
                >
                  {FX_CATEGORY_CN[c]} {n}
                </button>
              )
            })}
          </div>
        </div>
      </div>

      {randomId && (
        <div className="flex items-center gap-2 text-xs text-emerald-700">
          <Sparkles className="h-3.5 w-3.5" />
          正在展示随机特效
          <button className="underline underline-offset-2" onClick={clearRandom}>
            查看全部
          </button>
        </div>
      )}

      {/* 特效网格 */}
      {filtered.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-10 text-center">
            <Sparkles className="h-8 w-8 text-emerald-300" />
            <p className="text-sm text-muted-foreground">没有匹配的特效，换个关键词试试</p>
          </CardContent>
        </Card>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filtered.slice(0, visible).map((e) => (
              <FxCard key={e.id} fx={e} copied={copied === e.id} onCopy={() => copyCss(e)} />
            ))}
          </div>

          {visible < filtered.length && (
            <div className="flex justify-center">
              <Button variant="outline" onClick={() => setVisible((v) => v + PAGE_SIZE)}>
                加载更多（{filtered.length - visible} 个）
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

/** 单个特效卡片：实时演示 + 元信息 + 复制（视口内才播放动画） */
function FxCard({ fx, copied, onCopy }: { fx: FxEffect; copied: boolean; onCopy: () => void }) {
  const sizeCls =
    fx.category === 'Text' ? 'fx-demo-text' : fx.category === 'Backgrounds' ? 'fx-demo-bg' : ''
  const demoRef = useRef<HTMLDivElement>(null)

  // 视口外暂停动画：省 CPU / GPU，避免几十个特效同时运行卡顿
  useEffect(() => {
    const el = demoRef.current
    if (!el) return
    if (typeof IntersectionObserver === 'undefined') return
    const io = new IntersectionObserver(
      (entries) => {
        for (const en of entries) {
          en.target.classList.toggle('fx-paused', !en.isIntersecting)
        }
      },
      { rootMargin: '120px 0px' }
    )
    io.observe(el)
    return () => io.disconnect()
  }, [])

  return (
    <Card className="group overflow-hidden transition-shadow hover:shadow-lg">
      {/* 演示区（深浅双底，保证深色/浅色效果都可见） */}
      <div ref={demoRef} className="grid grid-cols-2 divide-x border-b">
        <div className="flex h-36 items-center justify-center overflow-hidden bg-white p-3">
          <div
            dangerouslySetInnerHTML={{ __html: fx.html }}
            className={`fx-demo-host ${sizeCls} pointer-events-none`}
          />
        </div>
        <div className="flex h-36 items-center justify-center overflow-hidden bg-slate-900 p-3">
          <div
            dangerouslySetInnerHTML={{ __html: fx.html }}
            className={`fx-demo-host ${sizeCls} pointer-events-none`}
          />
        </div>
      </div>
      {/* hover 时叠加说明（不影响演示） */}
      <CardContent className="space-y-2 p-3">
        <div className="flex items-center justify-between gap-2">
          <p className="truncate text-sm font-semibold text-foreground/90">{fx.title}</p>
          <span className="shrink-0 rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-semibold text-emerald-700">
            {FX_CATEGORY_CN[fx.category]}
          </span>
        </div>
        <p className="line-clamp-2 min-h-[2.2em] text-xs text-muted-foreground">{fx.description}</p>
        <div className="flex items-center justify-between gap-2">
          <div className="flex flex-wrap gap-1 overflow-hidden">
            {fx.tags.slice(0, 3).map((t) => (
              <Badge key={t} variant="secondary" className="px-1.5 text-[10px] font-normal">
                {t}
              </Badge>
            ))}
          </div>
          <button
            onClick={onCopy}
            className="inline-flex shrink-0 items-center gap-1 rounded-md border px-2 py-1 text-[11px] font-semibold text-emerald-700 transition hover:bg-emerald-50"
            aria-label={`复制 ${fx.title} 的 CSS`}
          >
            {copied ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
            {copied ? '已复制' : '复制 CSS'}
          </button>
        </div>
      </CardContent>
    </Card>
  )
}

'use client'

/**
 * 判分矢量图标（√ / × / 半对）：
 * 三端（复核界面、叠加层、PDF、Brother JPEG）统一画法 ——
 *   correct: 绿色完整勾
 *   wrong:   红色叉
 *   partial: 勾 + 贯穿斜杠（v2.0.3 重制：旧版勾上部小叉不像半对半错；
 *            新版勾被一斜杠划过，与全对/全错一眼区分，老师批改习惯符号）
 */

export const JUDGE_COLOR: Record<string, string> = {
  correct: '#16a34a',
  wrong: '#dc2626',
  partial: '#d97706',
}

export function JudgeMarkIcon({
  type,
  size = 22,
  color,
  className,
  strokeWidth,
}: {
  type: string
  size?: number
  color?: string
  className?: string
  strokeWidth?: number
}) {
  const c = color || JUDGE_COLOR[type] || JUDGE_COLOR.wrong
  const sw = strokeWidth ?? 3

  if (type === 'correct') {
    return (
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        className={className}
        aria-hidden
      >
        <path
          d="M4 12.5 L9.5 18 L20 6"
          stroke={c}
          strokeWidth={sw}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    )
  }

  if (type === 'wrong') {
    return (
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        className={className}
        aria-hidden
      >
        <path
          d="M6 6 L18 18 M18 6 L6 18"
          stroke={c}
          strokeWidth={sw}
          strokeLinecap="round"
        />
      </svg>
    )
  }

  // 半对：完整勾 + 贯穿斜杠（左上→右下，穿过勾的两臂）
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      className={className}
      aria-hidden
    >
      <path
        d="M4 12.5 L9.5 18 L20 6"
        stroke={c}
        strokeWidth={sw}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M6 3 L18 21"
        stroke={c}
        strokeWidth={sw * 0.85}
        strokeLinecap="round"
      />
    </svg>
  )
}

/** 半勾 ⍻ 单独导出（列表徽标等场景使用，颜色跟随后续文字） */
export function HalfCheckIcon({ className }: { className?: string }) {
  return <JudgeMarkIcon type="partial" className={className} />
}

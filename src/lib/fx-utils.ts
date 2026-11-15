'use client'

/**
 * 站内通用动效工具：
 *  - fxSpotlightProps：给任意元素加 onMouseMove，更新 CSS 变量 --mx/--my，
 *    配合 .fx-spot 类实现「鼠标聚光灯」跟随光标的柔光特效（纯 CSS 渲染）。
 */
export function fxSpotlightProps() {
  return {
    onMouseMove: (e: React.MouseEvent<HTMLElement>) => {
      const el = e.currentTarget
      const r = el.getBoundingClientRect()
      el.style.setProperty('--mx', `${e.clientX - r.left}px`)
      el.style.setProperty('--my', `${e.clientY - r.top}px`)
    },
  }
}

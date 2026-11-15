/**
 * CSSFX 特效库数据（100 效果）
 * 来源：u1q2h5ky5jp1-deploy.space-z.ai（CSSFX — A CSS Effect Library）
 * CSS 样式见 src/app/cssfx.css（已换肤为 PaperMarker 翠绿/青绿主题）
 */
export interface FxEffect {
  id: string
  title: string
  category: string
  description: string
  tags: string[]
  html: string
}

export const FX_CATEGORIES = ['Buttons', 'Loaders', 'Cards', 'Text', 'Backgrounds', 'Hover', 'Borders', 'Forms', 'Misc'] as const

export const FX_CATEGORY_CN: Record<string, string> = {
  Buttons: '按钮',
  Loaders: '加载动画',
  Cards: '卡片',
  Text: '文字',
  Backgrounds: '背景',
  Hover: '悬停',
  Borders: '边框',
  Forms: '表单',
  Misc: '杂项',
}

export const FX_EFFECTS: FxEffect[] = [
  { id: 'btn-gradient-slide', title: 'Gradient Slide', category: 'Buttons', description: 'A gradient that slides from left to right on hover, with a soft lift.', tags: ['hover', 'gradient', 'cta'], html: `<button class="fx-btn-gradient-slide">Explore →</button>` },
  { id: 'btn-neon-glow', title: 'Neon Glow Pulse', category: 'Buttons', description: 'A neon outlined button with a continuously pulsing aura.', tags: ['neon', 'pulse', 'outline'], html: `<button class="fx-btn-neon-glow">Launch</button>` },
  { id: 'btn-3d-press', title: '3D Press', category: 'Buttons', description: 'Chunky 3D button that physically depresses on click.', tags: ['3d', 'press', 'skeuomorphic'], html: `<button class="fx-btn-3d-press">Save</button>` },
  { id: 'btn-liquid-fill', title: 'Liquid Fill', category: 'Buttons', description: 'Color floods up from the bottom on hover like rising liquid.', tags: ['hover', 'fill', 'wave'], html: `<button class="fx-btn-liquid-fill"><span>Subscribe</span></button>` },
  { id: 'btn-border-trace', title: 'Border Trace', category: 'Buttons', description: 'A glowing line traces the perimeter on hover.', tags: ['hover', 'outline', 'animated'], html: `<button class="fx-btn-border-trace"><span>Continue</span></button>` },
  { id: 'btn-shine-sweep', title: 'Shine Sweep', category: 'Buttons', description: 'A diagonal light beam sweeps across on hover.', tags: ['hover', 'shine', 'glossy'], html: `<button class="fx-btn-shine-sweep">Get Started</button>` },
  { id: 'btn-skew-slide', title: 'Skew Slide', category: 'Buttons', description: 'Bold skew with a translation that nudges on hover.', tags: ['hover', 'skew'], html: `<button class="fx-btn-skew-slide">Read more</button>` },
  { id: 'loader-spinner', title: 'Dual Ring Spinner', category: 'Loaders', description: 'Two counter-rotating rings for a refined loading cue.', tags: ['spinner', 'rotate'], html: `<div class="fx-loader-spinner"><span></span></div>` },
  { id: 'loader-dots', title: 'Bouncing Dots', category: 'Loaders', description: 'Three dots bob in sequence — a classic chat indicator.', tags: ['dots', 'bounce'], html: `<div class="fx-loader-dots"><span></span><span></span><span></span></div>` },
  { id: 'loader-pulse-ring', title: 'Pulse Ring', category: 'Loaders', description: 'Expanding concentric rings, perfect for status indicators.', tags: ['pulse', 'ring'], html: `<div class="fx-loader-pulse-ring"></div>` },
  { id: 'loader-bars', title: 'Equalizer Bars', category: 'Loaders', description: 'Audio-equalizer style bars for media contexts.', tags: ['bars', 'audio'], html: `<div class="fx-loader-bars"><span></span><span></span><span></span><span></span><span></span></div>` },
  { id: 'loader-orbit', title: 'Orbit', category: 'Loaders', description: 'Satellites orbit a central star — great for syncing states.', tags: ['orbit', 'space'], html: `<div class="fx-loader-orbit"><span></span><span></span><span></span></div>` },
  { id: 'loader-cube', title: 'Folding Cube', category: 'Loaders', description: 'A cube that folds in on itself in 3D.', tags: ['3d', 'cube', 'fold'], html: `<div class="fx-loader-cube"><span></span><span></span><span></span><span></span></div>` },
  { id: 'card-glass', title: 'Glassmorphism', category: 'Cards', description: 'Frosted glass over a colorful backdrop with subtle border highlight.', tags: ['glass', 'blur', 'frosted'], html: `<div class="fx-card-glass">
      <div class="fx-card-glass-badge">PRO</div>
      <h3>Aurora Plan</h3>
      <p>Frosted panel floating above an animated mesh gradient.</p>
      <button class="fx-card-glass-btn">Upgrade</button>
    </div>` },
  { id: 'card-spotlight', title: 'Spotlight Cursor', category: 'Cards', description: 'A radial spotlight follows the cursor across the card surface.', tags: ['hover', 'cursor', 'spotlight'], html: `<div class="fx-card-spotlight" onmousemove="this.style.setProperty('--mx', event.offsetX+'px'); this.style.setProperty('--my', event.offsetY+'px')">
      <h3>Hover me</h3>
      <p>Move your cursor — the glow follows.</p>
      <span class="fx-card-spotlight-cta">→</span>
    </div>` },
  { id: 'card-flip', title: 'Flip Card', category: 'Cards', description: '3D flip revealing the back face on hover.', tags: ['3d', 'flip', 'hover'], html: `<div class="fx-card-flip">
      <div class="fx-card-flip-face fx-card-flip-front">
        <span class="fx-card-flip-icon">★</span>
        <p>Hover to flip</p>
      </div>
      <div class="fx-card-flip-face fx-card-flip-back">
        <p>Surprise! 🎉</p>
      </div>
    </div>` },
  { id: 'card-neon-border', title: 'Conic Neon Border', category: 'Cards', description: 'A gradient conic border that rotates around the card.', tags: ['border', 'neon', 'animated'], html: `<div class="fx-card-neon-border">
      <div class="fx-card-neon-inner">
        <h3>Live</h3>
        <p>Conic gradient halo rotating 360°.</p>
      </div>
    </div>` },
  { id: 'text-gradient', title: 'Animated Gradient', category: 'Text', description: 'Text filled with a flowing multi-color gradient.', tags: ['gradient', 'color'], html: `<h2 class="fx-text-gradient">Make it vivid</h2>` },
  { id: 'text-glitch', title: 'Glitch', category: 'Text', description: 'Cyberpunk RGB-split glitch with continuous jitter.', tags: ['glitch', 'rgb', 'cyberpunk'], html: `<h2 class="fx-text-glitch" data-text="SYSTEM">SYSTEM</h2>` },
  { id: 'text-neon', title: 'Neon Flicker', category: 'Text', description: 'Glowing neon sign with an organic flicker.', tags: ['neon', 'glow', 'flicker'], html: `<h2 class="fx-text-neon">OPEN</h2>` },
  { id: 'text-shimmer', title: 'Shimmer', category: 'Text', description: 'A traveling highlight sweeps across the text — great for loading.', tags: ['shimmer', 'loading'], html: `<h2 class="fx-text-shimmer">Loading experience</h2>` },
  { id: 'text-3d', title: 'Layered 3D', category: 'Text', description: 'Stacked shadow layers create depth without transforms.', tags: ['3d', 'depth'], html: `<h2 class="fx-text-3d">DEPTH</h2>` },
  { id: 'bg-mesh', title: 'Animated Mesh', category: 'Backgrounds', description: 'Soft drifting color blobs forming a mesh gradient.', tags: ['gradient', 'mesh'], html: `<div class="fx-bg-mesh"></div>` },
  { id: 'bg-grid', title: 'Pulse Grid', category: 'Backgrounds', description: 'A perspective grid that pans forever.', tags: ['grid', 'retro'], html: `<div class="fx-bg-grid"></div>` },
  { id: 'bg-aurora', title: 'Aurora Waves', category: 'Backgrounds', description: 'Smoothly morphing aurora ribbon across the sky.', tags: ['aurora', 'ribbon'], html: `<div class="fx-bg-aurora"></div>` },
  { id: 'bg-orbs', title: 'Floating Orbs', category: 'Backgrounds', description: 'Soft blurred orbs floating upward — great for hero sections.', tags: ['orbs', 'floating'], html: `<div class="fx-bg-orbs">
      <span></span><span></span><span></span><span></span>
    </div>` },
  { id: 'hover-underline', title: 'Sliding Underline', category: 'Hover', description: 'Underline that slides in from the left on hover.', tags: ['link', 'underline'], html: `<a class="fx-hover-underline">Documentation</a>` },
  { id: 'hover-zoom-reveal', title: 'Zoom Reveal', category: 'Hover', description: 'Image zooms while an overlay caption slides up.', tags: ['image', 'zoom', 'caption'], html: `<figure class="fx-hover-zoom">
      <img src="https://images.unsplash.com/photo-1518770660439-4636190af475?w=400&q=60" alt="Chip" />
      <figcaption>
        <strong>Silicon</strong>
        <span>The future, miniaturized</span>
      </figcaption>
    </figure>` },
  { id: 'hover-icon-bounce', title: 'Icon Bounce', category: 'Hover', description: 'Icon springs up with a satisfying overshoot.', tags: ['icon', 'bounce', 'spring'], html: `<button class="fx-hover-icon-bounce">
      <span class="fx-hover-icon-bounce-icon">🔔</span>
      <span>Notifications</span>
    </button>` },
  { id: 'border-gradient', title: 'Gradient Border', category: 'Borders', description: 'Animated gradient border using border-image.', tags: ['gradient', 'border-image'], html: `<div class="fx-border-gradient">Hover-tint frame</div>` },
  { id: 'border-corners', title: 'Corner Brackets', category: 'Borders', description: 'Sci-fi style corner brackets that close in on hover.', tags: ['corners', 'sci-fi'], html: `<div class="fx-border-corners"><span>Scan target</span></div>` },
  { id: 'form-toggle', title: 'Glow Toggle', category: 'Forms', description: 'iOS-style switch with a smooth glow on activate.', tags: ['toggle', 'switch', 'ios'], html: `<label class="fx-form-toggle">
      <input type="checkbox" checked>
      <span class="fx-form-toggle-slider"></span>
    </label>` },
  { id: 'form-heart', title: 'Heart Like', category: 'Forms', description: 'Checkbox styled as a beating heart on like.', tags: ['heart', 'like', 'checkbox'], html: `<label class="fx-form-heart">
      <input type="checkbox">
      <svg viewBox="0 0 24 24"><path d="M12 21s-7-4.5-9.5-9C.8 8.6 2.5 5 6 5c2 0 3.4 1.2 4 2.3C10.6 6.2 12 5 14 5c3.5 0 5.2 3.6 3.5 7-2.5 4.5-9.5 9-9.5 9z"/></svg>
    </label>` },
  { id: 'form-input-glow', title: 'Floating Label Input', category: 'Forms', description: 'Underline input with floating label and glowing focus line.', tags: ['input', 'focus', 'floating-label'], html: `<div class="fx-form-input">
      <input type="text" placeholder=" " id="fx-name">
      <label for="fx-name">Your name</label>
      <span></span>
    </div>` },
  { id: 'misc-progress', title: 'Striped Progress', category: 'Misc', description: 'Animated candy-striped progress bar.', tags: ['progress', 'striped'], html: `<div class="fx-misc-progress"><span style="width:68%"></span></div>` },
  { id: 'misc-skeleton', title: 'Skeleton Shimmer', category: 'Misc', description: 'Loading placeholder with shimmer sweep.', tags: ['skeleton', 'loading'], html: `<div class="fx-misc-skeleton">
      <div class="fx-misc-skeleton-avatar"></div>
      <div class="fx-misc-skeleton-lines">
        <span></span><span></span><span></span>
      </div>
    </div>` },
  { id: 'misc-fab', title: 'Pulse FAB', category: 'Misc', description: 'Floating action button with an attention-grabbing pulse ring.', tags: ['fab', 'pulse', 'cta'], html: `<button class="fx-misc-fab">+</button>` },
  { id: 'misc-tooltip', title: 'Pure CSS Tooltip', category: 'Misc', description: 'Tooltip that fades & slides in without any JavaScript.', tags: ['tooltip', 'css-only'], html: `<div class="fx-misc-tooltip-wrap">
      <button class="fx-misc-tooltip-btn">Hover</button>
      <span class="fx-misc-tooltip">I'm a pure CSS tooltip 👋</span>
    </div>` },
  { id: 'btn-ripple', title: 'Ripple Click', category: 'Buttons', description: 'Material-style ripple emanating from the click point.', tags: ['ripple', 'material', 'click'], html: `<button class="fx-btn-ripple" onclick="this.classList.remove('fx-rippling'); void this.offsetWidth; this.classList.add('fx-rippling')">Click me</button>` },
  { id: 'btn-icon-slide', title: 'Icon Slide-In', category: 'Buttons', description: 'Arrow icon slides in from the right on hover.', tags: ['hover', 'icon', 'arrow'], html: `<button class="fx-btn-icon-slide">
      <span>Learn more</span>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M13 5l7 7-7 7"/></svg>
    </button>` },
  { id: 'btn-morph', title: 'Morph Pill', category: 'Buttons', description: 'Pill that morphs from outline to solid on hover.', tags: ['hover', 'morph', 'pill'], html: `<button class="fx-btn-morph">Get the app</button>` },
  { id: 'loader-clock', title: 'Clock Wiper', category: 'Loaders', description: 'A circular wiper sweeping around like a clock hand.', tags: ['clock', 'wiper', 'circle'], html: `<div class="fx-loader-clock"></div>` },
  { id: 'loader-typewriter', title: 'Typing Dots', category: 'Loaders', description: 'Word that types itself out and erases — perfect for AI UIs.', tags: ['typing', 'text', 'ai'], html: `<div class="fx-loader-typewriter">Thinking</div>` },
  { id: 'text-typewriter', title: 'Typewriter', category: 'Text', description: 'Classic typewriter reveal with a blinking caret.', tags: ['typewriter', 'reveal'], html: `<h2 class="fx-text-typewriter">hello, world.</h2>` },
  { id: 'text-wave', title: 'Wave Letters', category: 'Text', description: 'Each letter bobs in a wave — great for hero copy.', tags: ['wave', 'letters', 'bounce'], html: `<h2 class="fx-text-wave" aria-label="WONDERFUL">
      <span style="--i:0">W</span><span style="--i:1">O</span><span style="--i:2">N</span><span style="--i:3">D</span><span style="--i:4">E</span><span style="--i:5">R</span><span style="--i:6">F</span><span style="--i:7">U</span><span style="--i:8">L</span>
    </h2>` },
  { id: 'text-rainbow', title: 'Rainbow Cycle', category: 'Text', description: 'Each letter cycles through the rainbow continuously.', tags: ['rainbow', 'color', 'cycle'], html: `<h2 class="fx-text-rainbow" aria-label="DISCO">
      <span style="--i:0">D</span><span style="--i:1">I</span><span style="--i:2">S</span><span style="--i:3">C</span><span style="--i:4">O</span>
    </h2>` },
  { id: 'hover-card-tilt', title: '3D Tilt Card', category: 'Hover', description: 'Card tilts in 3D space tracking the cursor.', tags: ['3d', 'tilt', 'cursor'], html: `<div class="fx-hover-card-tilt">
      <div class="fx-hover-card-tild-inner">
        <h4>3D Tilt</h4>
        <p>Move your cursor over me</p>
      </div>
    </div>` },
  { id: 'hover-text-scramble', title: 'Slide & Swap', category: 'Hover', description: 'Text slides up and is replaced with new copy on hover.', tags: ['text', 'swap', 'hover'], html: `<button class="fx-hover-text-swap">
      <span class="fx-hover-text-swap-default">Read</span>
      <span class="fx-hover-text-swap-hover">→ Enjoy</span>
    </button>` },
  { id: 'misc-badge-pulse', title: 'Notification Badge', category: 'Misc', description: 'A notification dot that pulses to draw the eye.', tags: ['badge', 'notification', 'pulse'], html: `<button class="fx-misc-badge">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
      <span class="fx-misc-badge-dot">3</span>
    </button>` },
  { id: 'misc-accordion', title: 'Pure CSS Accordion', category: 'Misc', description: 'Expand/collapse using only <details> — no JavaScript.', tags: ['accordion', 'details', 'css-only'], html: `<details class="fx-misc-accordion">
      <summary>What is pure CSS?</summary>
      <div class="fx-misc-accordion-body">Effects that need zero JavaScript — just CSS rules.</div>
    </details>` },
  { id: 'misc-breadcrumb', title: 'Breadcrumb Arrows', category: 'Misc', description: 'Separators that animate between breadcrumb items.', tags: ['breadcrumb', 'navigation'], html: `<nav class="fx-misc-breadcrumb">
      <a>Home</a>
      <a>Library</a>
      <a>Buttons</a>
      <span>Gradient</span>
    </nav>` },
  { id: 'loader-infinity', title: 'Infinity Loop', category: 'Loaders', description: 'A dot traces a figure-eight infinity symbol.', tags: ['infinity', 'loop', 'elegant'], html: `<div class="fx-loader-infinity"><span></span></div>` },
  { id: 'loader-circle-fade', title: 'Circle Fade', category: 'Loaders', description: 'Eight dots fade in sequence around a circle.', tags: ['circle', 'fade', 'spinner'], html: `<div class="fx-loader-circle-fade">
      <span></span><span></span><span></span><span></span>
      <span></span><span></span><span></span><span></span>
    </div>` },
  { id: 'text-fire', title: 'Fire Glow', category: 'Text', description: 'Hot text with a flickering fire-tinted glow.', tags: ['fire', 'glow', 'warm'], html: `<h2 class="fx-text-fire">HOT</h2>` },
  { id: 'text-stroke', title: 'Outline Reveal', category: 'Text', description: 'Letter outlines draw themselves in like a signature.', tags: ['outline', 'stroke', 'draw'], html: `<h2 class="fx-text-stroke">DESIGN</h2>` },
  { id: 'bg-confetti', title: 'Confetti Rain', category: 'Backgrounds', description: 'Continuous confetti rain — perfect for celebrations.', tags: ['confetti', 'celebration', 'rain'], html: `<div class="fx-bg-confetti">
      <span></span><span></span><span></span><span></span>
      <span></span><span></span><span></span><span></span>
    </div>` },
  { id: 'bg-stars', title: 'Twinkling Stars', category: 'Backgrounds', description: 'A starry night with subtle twinkle animation.', tags: ['stars', 'night', 'twinkle'], html: `<div class="fx-bg-stars">
      <span></span><span></span><span></span><span></span>
      <span></span><span></span><span></span><span></span>
      <span></span><span></span>
    </div>` },
  { id: 'hover-follow-cursor', title: 'Cursor Magnet', category: 'Hover', description: 'Element gently follows the cursor like a magnetic pull.', tags: ['magnet', 'cursor', 'interactive'], html: `<div class="fx-hover-magnet" onmousemove="this.style.setProperty('--x', (event.offsetX-40)+'px'); this.style.setProperty('--y', (event.offsetY-40)+'px')"></div>` },
  { id: 'misc-switch', title: 'Day / Night Switch', category: 'Forms', description: 'Playful toggle that morphs between sun and moon.', tags: ['switch', 'day', 'night'], html: `<label class="fx-misc-switch">
      <input type="checkbox">
      <span class="fx-misc-switch-track">
        <span class="fx-misc-switch-thumb"></span>
      </span>
    </label>` },
  { id: 'misc-counter', title: 'Animated Counter', category: 'Misc', description: 'Number that rolls up using CSS @property (no JS).', tags: ['counter', 'number', 'rollup'], html: `<div class="fx-misc-counter">1,234</div>` },
  { id: 'btn-gooey', title: 'Gooey Merge', category: 'Buttons', description: 'Two dots merge into one with an SVG gooey filter on hover.', tags: ['gooey', 'svg', 'filter', 'merge'], html: `<button class="fx-btn-gooey">
      <span class="fx-btn-gooey-dot"></span>
      <span class="fx-btn-gooey-text">Submit</span>
      <span class="fx-btn-gooey-dot"></span>
    </button>` },
  { id: 'btn-pull', title: 'Pull String', category: 'Buttons', description: 'Animated dashed underline that pulls tight on hover.', tags: ['dashed', 'underline', 'minimal'], html: `<button class="fx-btn-pull">View profile</button>` },
  { id: 'loader-radar', title: 'Radar Sweep', category: 'Loaders', description: 'A radar screen with a sweeping beam and blinking blips.', tags: ['radar', 'sweep', 'scan'], html: `<div class="fx-loader-radar">
      <span class="fx-loader-radar-blip" style="--bx:30%;--by:25%;--bd:0.4s"></span>
      <span class="fx-loader-radar-blip" style="--bx:65%;--by:55%;--bd:1.2s"></span>
      <span class="fx-loader-radar-blip" style="--bx:45%;--by:75%;--bd:2s"></span>
    </div>` },
  { id: 'text-split', title: 'Split Reveal', category: 'Text', description: 'Letters split apart and reform continuously.', tags: ['split', 'letters', 'kinetic'], html: `<h2 class="fx-text-split" aria-label="KINETIC">
      <span style="--i:0">K</span><span style="--i:1">I</span><span style="--i:2">N</span><span style="--i:3">E</span><span style="--i:4">T</span><span style="--i:5">I</span><span style="--i:6">C</span>
    </h2>` },
  { id: 'hover-shine-card', title: 'Shine Sweep Card', category: 'Hover', description: 'A gloss highlight sweeps diagonally across the card on hover.', tags: ['shine', 'gloss', 'card'], html: `<div class="fx-hover-shine-card">
      <div class="fx-hover-shine-card-inner">
        <span>Glossy</span>
      </div>
    </div>` },
  { id: 'misc-tag', title: 'Removable Tag', category: 'Misc', description: 'Pill tag with an X button that animates away on click.', tags: ['tag', 'chip', 'removable'], html: `<div class="fx-misc-tag">
      <span>react</span>
      <button type="button" aria-label="Remove tag">×</button>
    </div>` },
  { id: 'card-holographic', title: 'Holographic Foil', category: 'Cards', description: 'Iridescent foil that shimmers as you move the cursor.', tags: ['holo', 'iridescent', 'shimmer'], html: `<div class="fx-card-holo" onmousemove="this.style.setProperty('--mx', event.offsetX+'px'); this.style.setProperty('--my', event.offsetY+'px')">
      <span class="fx-card-holo-label">PREMIUM</span>
      <strong>Holographic</strong>
    </div>` },
  { id: 'loader-progress-circular', title: 'Circular Progress', category: 'Loaders', description: 'Indeterminate circular progress with conic gradient.', tags: ['progress', 'circular', 'conic'], html: `<div class="fx-loader-progress-circular"><span>75%</span></div>` },
  { id: 'border-glow', title: 'Glow Border', category: 'Borders', description: 'Animated gradient border with soft outer glow.', tags: ['glow', 'gradient', 'animated'], html: `<div class="fx-border-glow"><span>Hover the glow</span></div>` },
  { id: 'border-dashed-flow', title: 'Marching Ants', category: 'Borders', description: 'Dashed border that flows continuously — used for selections.', tags: ['dashed', 'selection', 'animated'], html: `<div class="fx-border-dashed-flow">Selected area</div>` },
  { id: 'misc-keyboard-key', title: 'Keyboard Key', category: 'Misc', description: 'Realistic 3D keyboard key that depresses on click.', tags: ['key', '3d', 'keyboard'], html: `<button class="fx-misc-key"><span>⌘</span></button>` },
  { id: 'misc-divider', title: 'Animated Divider', category: 'Misc', description: 'Ornate divider with traveling shimmer.', tags: ['divider', 'separator', 'shimmer'], html: `<div class="fx-misc-divider"><span>✦</span></div>` },
  { id: 'btn-perspective', title: 'Perspective Tilt', category: 'Buttons', description: 'Button that tilts in 3D toward the cursor with depth shadow.', tags: ['3d', 'perspective', 'cursor'], html: `<button class="fx-btn-perspective" onmousemove="this.style.setProperty('--rx', (40 - event.offsetY)*-0.3+'deg'); this.style.setProperty('--ry', (event.offsetX - 60)*0.3+'deg')" onmouseleave="this.style.setProperty('--rx','0deg'); this.style.setProperty('--ry','0deg')">Tilt</button>` },
  { id: 'loader-bouncing', title: 'Bouncing Ball', category: 'Loaders', description: 'A ball bounces with a shadow that scales with the height.', tags: ['bounce', 'ball', 'shadow'], html: `<div class="fx-loader-bouncing">
      <div class="fx-loader-bouncing-ball"></div>
      <div class="fx-loader-bouncing-shadow"></div>
    </div>` },
  { id: 'text-reveal-mask', title: 'Mask Reveal', category: 'Text', description: 'Letters reveal through a clip-path that sweeps across.', tags: ['reveal', 'mask', 'clip'], html: `<h2 class="fx-text-reveal-mask" data-text="REVEAL">REVEAL</h2>` },
  { id: 'hover-stuck', title: 'Stuck Note', category: 'Hover', description: 'Card lifts off and tilts like a sticky note on hover.', tags: ['paper', 'tilt', 'skeuomorphic'], html: `<div class="fx-hover-stuck">
      <span class="fx-hover-stuck-pin"></span>
      <p>Stuck!</p>
    </div>` },
  { id: 'card-stack-3d', title: '3D Card Stack', category: 'Cards', description: 'Stacked cards that fan out on hover with depth.', tags: ['3d', 'stack', 'depth'], html: `<div class="fx-card-stack">
      <div class="fx-card-stack-card fx-card-stack-back"></div>
      <div class="fx-card-stack-card fx-card-stack-mid"></div>
      <div class="fx-card-stack-card fx-card-stack-front">
        <strong>Hover</strong>
        <span>to fan out</span>
      </div>
    </div>` },
  { id: 'loader-hexagon', title: 'Hexagon Pulse', category: 'Loaders', description: 'Six hexagonal dots pulse in sequence using clip-path.', tags: ['hexagon', 'pulse', 'geometric'], html: `<div class="fx-loader-hexagon">
      <span></span><span></span><span></span>
      <span></span><span></span><span></span>
    </div>` },
  { id: 'form-range-glow', title: 'Glow Range Slider', category: 'Forms', description: 'Range slider with a glowing thumb and gradient track.', tags: ['slider', 'range', 'input'], html: `<input type="range" value="60" min="0" max="100" class="fx-form-range" />` },
  { id: 'misc-qr-scanner', title: 'QR Scanner Line', category: 'Misc', description: 'A scanning line that sweeps across like a QR reader.', tags: ['scanner', 'qr', 'scan'], html: `<div class="fx-misc-qr-scanner">
      <div class="fx-misc-qr-scanner-line"></div>
      <div class="fx-misc-qr-scanner-corners">
        <span></span><span></span><span></span><span></span>
      </div>
    </div>` },
  { id: 'btn-gradient-border-hover', title: 'Gradient Border Hover', category: 'Buttons', description: 'Border that morphs from solid to gradient on hover.', tags: ['border', 'gradient', 'hover'], html: `<button class="fx-btn-gradient-border-hover">Continue</button>` },
  { id: 'loader-battery', title: 'Battery Charge', category: 'Loaders', description: 'A battery icon that fills up like it\'s charging.', tags: ['battery', 'fill', 'icon'], html: `<div class="fx-loader-battery">
      <div class="fx-loader-battery-body">
        <div class="fx-loader-battery-fill"></div>
      </div>
      <div class="fx-loader-battery-tip"></div>
    </div>` },
  { id: 'text-fade-cycle', title: 'Fade Word Cycle', category: 'Text', description: 'Word that cycles through a list with smooth fade.', tags: ['fade', 'cycle', 'rotating'], html: `<h2 class="fx-text-fade-cycle">
      <span>I build</span>
      <span class="fx-text-fade-cycle-words">
        <span style="--i:0">interfaces</span>
        <span style="--i:1">experiences</span>
        <span style="--i:2">animations</span>
      </span>
    </h2>` },
  { id: 'hover-water', title: 'Water Ripple', category: 'Hover', description: 'Ripple emanates from the click point on hover.', tags: ['water', 'ripple', 'click'], html: `<button class="fx-hover-water">Click me</button>` },
  { id: 'btn-jelly', title: 'Jelly Bounce', category: 'Buttons', description: 'Button wobbles like jelly when clicked.', tags: ['jelly', 'wobble', 'spring'], html: `<button class="fx-btn-jelly">Wobble</button>` },
  { id: 'loader-progress-bar', title: 'Progress Bar', category: 'Loaders', description: 'Indeterminate progress bar with shimmer overlay.', tags: ['progress', 'bar', 'shimmer'], html: `<div class="fx-loader-progress-bar"><span></span></div>` },
  { id: 'text-marker', title: 'Marker Highlight', category: 'Text', description: 'Highlight that draws across text like a marker pen.', tags: ['highlight', 'marker', 'draw'], html: `<h2 class="fx-text-marker"><span>important</span> idea</h2>` },
  { id: 'misc-toggle-switch', title: 'Minimal Toggle', category: 'Forms', description: 'Clean minimal toggle switch with smooth thumb slide.', tags: ['toggle', 'switch', 'minimal'], html: `<label class="fx-misc-toggle-switch">
      <input type="checkbox">
      <span class="fx-misc-toggle-track">
        <span class="fx-misc-toggle-thumb"></span>
      </span>
    </label>` },
  { id: 'card-pricing', title: 'Pricing Card Hover', category: 'Cards', description: 'Pricing tier that lifts and glows on hover with CTA emphasis.', tags: ['pricing', 'card', 'lift'], html: `<div class="fx-card-pricing">
      <span class="fx-card-pricing-tag">PRO</span>
      <div class="fx-card-pricing-price">$9<span>/mo</span></div>
      <ul>
        <li>Unlimited projects</li>
        <li>Priority support</li>
        <li>Custom branding</li>
      </ul>
      <button>Choose Pro</button>
    </div>` },
  { id: 'text-typing-rotator', title: 'Typed Rotator', category: 'Text', description: 'Multiple words typed out and erased in sequence.', tags: ['typing', 'rotator', 'animated'], html: `<div class="fx-text-typing-rotator">
      <span class="fx-text-typing-rotator-text"></span><span class="fx-text-typing-rotator-cursor">|</span>
    </div>` },
  { id: 'hover-button-stack', title: 'Stack Reveal', category: 'Hover', description: 'Hover reveals stacked layers underneath like a paper stack.', tags: ['stack', 'layers', 'reveal'], html: `<div class="fx-hover-stack">
      <div class="fx-hover-stack-layer fx-hover-stack-back"></div>
      <div class="fx-hover-stack-layer fx-hover-stack-mid"></div>
      <div class="fx-hover-stack-front">Hover</div>
    </div>` },
  { id: 'misc-avatar-stack', title: 'Avatar Stack', category: 'Misc', description: 'Stacked avatars that fan out on hover with +N overflow.', tags: ['avatar', 'stack', 'users'], html: `<div class="fx-misc-avatar-stack">
      <span style="--i:0">A</span>
      <span style="--i:1">B</span>
      <span style="--i:2">C</span>
      <span style="--i:3">D</span>
      <span class="fx-misc-avatar-stack-more">+5</span>
    </div>` },
  { id: 'loader-book', title: 'Book Flip', category: 'Loaders', description: 'A book whose pages flip continuously in 3D.', tags: ['book', '3d', 'flip'], html: `<div class="fx-loader-book">
      <div class="fx-loader-book-page"></div>
      <div class="fx-loader-book-page"></div>
      <div class="fx-loader-book-page"></div>
      <div class="fx-loader-book-page"></div>
    </div>` },
  { id: 'text-3d-cube', title: '3D Rotating Cube', category: 'Text', description: 'Letters orbit as if on the faces of a 3D cube.', tags: ['3d', 'cube', 'letters'], html: `<div class="fx-text-3d-cube-scene">
      <div class="fx-text-3d-cube">
        <span class="fx-text-3d-cube-face fx-text-3d-cube-front">CSS</span>
        <span class="fx-text-3d-cube-face fx-text-3d-cube-back">FX</span>
        <span class="fx-text-3d-cube-face fx-text-3d-cube-right">VIBES</span>
        <span class="fx-text-3d-cube-face fx-text-3d-cube-left">DESIGN</span>
      </div>
    </div>` },
  { id: 'card-magnetic', title: 'Magnetic Pull', category: 'Cards', description: 'Card gently follows the cursor like a magnet.', tags: ['magnetic', 'cursor', 'interactive'], html: `<div class="fx-card-magnetic" onmousemove="this.style.setProperty('--rx', (50 - event.offsetY)*0.4+'deg'); this.style.setProperty('--ry', (event.offsetX - 100)*0.4+'deg')" onmouseleave="this.style.setProperty('--rx','0deg'); this.style.setProperty('--ry','0deg')">
      <span class="fx-card-magnetic-label">HOVER</span>
      <strong>Magnetic</strong>
    </div>` },
  { id: 'misc-toggle-group', title: 'Segmented Control', category: 'Forms', description: 'iOS-style segmented toggle with sliding indicator.', tags: ['segmented', 'toggle', 'ios'], html: `<div class="fx-misc-segmented">
      <input type="radio" name="seg" id="seg1" checked>
      <label for="seg1">Day</label>
      <input type="radio" name="seg" id="seg2">
      <label for="seg2">Week</label>
      <input type="radio" name="seg" id="seg3">
      <label for="seg3">Month</label>
      <span class="fx-misc-segmented-slider"></span>
    </div>` },
  { id: 'btn-arrow', title: 'Arrow Slide', category: 'Buttons', description: 'Text slides left as an arrow slides in from the right.', tags: ['arrow', 'slide', 'hover'], html: `<button class="fx-btn-arrow">
      <span class="fx-btn-arrow-text">Get started</span>
      <span class="fx-btn-arrow-icon">→</span>
    </button>` },
  { id: 'loader-circle-spin', title: 'Multi-Ring Spin', category: 'Loaders', description: 'Three concentric rings spinning at different speeds.', tags: ['ring', 'spin', 'multi'], html: `<div class="fx-loader-circle-spin">
      <span class="fx-loader-circle-spin-ring"></span>
      <span class="fx-loader-circle-spin-ring"></span>
      <span class="fx-loader-circle-spin-ring"></span>
    </div>` },
  { id: 'hover-tile-3d', title: '3D Tile Flip', category: 'Hover', description: 'Tile flips in 3D revealing a different back face.', tags: ['3d', 'flip', 'tile'], html: `<div class="fx-hover-tile-3d">
      <div class="fx-hover-tile-3d-inner">
        <div class="fx-hover-tile-3d-front">Hover</div>
        <div class="fx-hover-tile-3d-back">Hi! 👋</div>
      </div>
    </div>` },
  { id: 'text-glow-pulse', title: 'Glow Pulse', category: 'Text', description: 'Soft pulsing glow around a heading.', tags: ['glow', 'pulse', 'ambient'], html: `<h2 class="fx-text-glow-pulse">glow</h2>` },
]
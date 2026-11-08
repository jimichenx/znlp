# znlp 文档站

znlp 官方文档站，基于 [Astro](https://astro.build) + [Starlight](https://starlight.astro.build) 构建，静态输出，发布到阿里云 OSS（自定义域名 https://znlp.dev）。

## 本地开发

```bash
cd website
npm install
npm run dev        # http://localhost:5321
```

端口是 `astro.config.mjs` 里 `server.port` 指定的（`dev` 和 `preview` 共用）。临时换端口可以用 `npm run dev -- --port 4321`；要不要带路径前缀由 `base` 决定，当前 `base: '/'`，直接访问根路径即可。

## 构建与预览

```bash
npm run build      # 产物在 dist/
npm run preview    # 用静态服务器跑 dist/
```

## 目录结构

```
website/
├── astro.config.mjs        # Starlight 配置：site / base / 侧边栏 / 中文 locale
├── src/
│   ├── content.config.ts   # Astro 7 内容集合声明（docsLoader / i18nLoader）
│   ├── content/docs/       # 全部文档 Markdown
│   │   ├── index.md                  # 首页（splash）
│   │   ├── getting-started/          # 安装、快速开始
│   │   ├── guides/                   # 七项核心能力 + 三项扩展能力
│   │   ├── deployment/               # 工程落地：配置、资源、Spring Boot、基准
│   │   └── reference/                # API、配置项、架构、示例、FAQ
│   ├── styles/custom.css   # 中文字体栈、品牌色、排版覆盖
│   └── assets/logo.svg     # 站点 logo（纯图形，标题由 Starlight 渲染）
└── public/favicon.svg
```

## 发布

站点发布在自定义域名 **https://znlp.dev**（阿里云 OSS 静态托管），服务在根路径，因此 `astro.config.mjs` 里：

```js
const SITE = 'https://znlp.dev';
const BASE = '/';
```

因为 `BASE` 是 `'/'`，站点挂在域名根路径，没有子路径前缀。

### 构建与上传

```bash
cd website
npm ci
npm run build

# 增量上传，只传变化的文件
ossutil cp -r dist/ oss://<bucket>/ --update
```

**缓存策略**（OSS 静态托管的常见坑）：

- `dist/_astro/**` 文件名带内容 hash，可设长缓存（如 `Cache-Control: max-age=31536000, immutable`）
- `**/*.html` 必须设短缓存或不缓存，否则文档更新后用户会一直看到旧版本
- `dist/pagefind/**` 是搜索索引，建议短缓存

### 域名与备案

`znlp.dev` 为独立域名，直接绑定使用。OSS 绑定自定义域名后，Bucket 位于中国内地即可正常渲染网页——注意 OSS 的默认 Bucket 域名（`*.aliyuncs.com`）访问 HTML 会强制下载而非预览，所以绑域名是必需步骤，不是可选优化。

### 与仓库相关的链接

页眉右上角的 GitHub 入口来自 `astro.config.mjs` 的 `social`，由 `src/components/SocialIcons.astro` 渲染成图标加仓库地址 `jimichenx/znlp`，链到 `https://github.com/jimichenx/znlp`。还没有配置 `editLink`（页脚「编辑此页」）。如果要补：

```js
editLink: { baseUrl: 'https://github.com/jimichenx/znlp/edit/main/website/' },
```

## 内容约定

- 文档内容以**源码为准**，不以 `README.md` / `CONFIG_README.md` 为准。两者已有多处与代码不符（拼音不带声调、NER 仅 ns/nt、Duckling 无 Email/Url、未配置 Maven 发布、`ZnlpSystem.builder()` 包私有等），完整清单在 `reference/faq.md`。
- 新增页面后要在 `astro.config.mjs` 的 `sidebar` 里登记 slug，否则该页不会出现在导航中（但构建不会报错，容易漏）。
- 侧边栏里登记了但文件不存在的 slug 会让 `npm run build` 直接失败，这是有用的护栏。
- **正文内链统一用相对链接**（如 `../deployment/configuration/`、同目录用 `pos-tagging/`），不要写 `/guides/xxx/` 这种绝对路径。原因是 Astro 不会给 Markdown 里的绝对 href 加 `base` 前缀。当前 `BASE` 是 `'/'`，绝对路径暂时能打开；一旦 `base` 带前缀，这些内链会全部 404。之前就踩过这个坑。侧边栏和 hero 按钮由 Starlight 组件处理，不受影响。
- **首页是 `index.mdx`**（必须 `.mdx`，因为要用组件）。结构自上而下：自定义 `Hero`（`src/components/Hero.astro`，在 `astro.config.mjs` 的 `components.Hero` 覆盖 Starlight 默认英雄区；没有 `hero.image` 时右侧放 `SegmentPreview.astro`，词和词性必须与 `SegmentSnapshotTest` 的 `standard/typical` 一致）→ `CardGrid` + `Card` 特性卡片 → 三个 `SplitSection`（`src/components/SplitSection.astro`，图文左右排，`reverse` 控制图在哪边，图放 `slot="visual"`，`framed` 给示意图加底）→ `LinkCard` 落地入口 → CTA 按钮。frontmatter 里有 `prev: false` / `next: false`（Starlight 的翻页配置，见 `schema.js` 的 PrevNextLinkConfigSchema），关掉页面底部自动生成的翻页导航——首页作为落地页不需要它。英雄区按钮的 `variant` 必须显式写成 `primary` / `secondary` / `minimal`，省略时 schema 默认全是 `primary`。两张示意图是内联 SVG 组件（`src/components/diagrams/`），颜色走 Starlight 的 CSS 变量（`--sl-color-accent`、`--sl-color-gray-*`），所以深浅色自动适配，改品牌色无需重画。
- **这个版本的 Starlight 没有 splash 首页的 `features` frontmatter**：`schema` 里只有 `template` 和 `hero`，写进 frontmatter 的 `features` 会被静默丢弃、构建不报错，卡片也不会出现（在 0.42.6 上实测）。特性卡片一律用 `CardGrid` + `Card` 写在正文里。
- 首页的宽版内容区靠 `custom.css` 里的 `@media (min-width: 72rem) { body:has(.landing) { --sl-content-width: min(86rem, 100% - 4rem) } }` 实现，`.landing` 是 `index.mdx` 里包住所有区块的 div；其他页面不受影响。内容区随视口放大、只留 64px 左右的左右页边距，超宽屏（>1488px）由 86rem 封顶。必须限定在 ≥72rem：Starlight 从这个断点起才给 `.sl-container` 加 `margin-inline: auto`，更窄断点套用会左对齐、右侧留一道空白；移动端与平板保持 Starlight 默认的满宽布局。
- 首页两组卡片栅格（`.grid-features` / `.grid-deploy` 包住 `CardGrid`）在 `custom.css` 里按 ≥72rem 覆写列数（3 列 / 4 列）。Starlight 的 `CardGrid` 在 ≥50rem 是固定两列 `1fr 1fr`，不覆写的话内容区变宽后卡片会被拉到近 700px 宽。custom.css 未包 `@layer`，天然压过 Starlight 的 `@layer starlight.components`。

// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

const SITE = 'https://znlp.dev';
const BASE = '/';

export default defineConfig({
  site: SITE,
  base: BASE,
  trailingSlash: 'ignore',
  server: { port: 5321 },
  integrations: [
    starlight({
      title: 'znlp',
      description: '模块化的 Java 中文 NLP 工具包：分词、词性标注、命名实体、拼音、繁简转换、新词发现与智能高亮。',
      logo: { src: './src/assets/logo.svg' },
      defaultLocale: 'root',
      locales: {
        root: { label: '简体中文', lang: 'zh-CN' },
      },
      // 暂不配置 social / editLink：代码库在 codeup.aliyun.com（匿名访问会跳登录页），
      // 没有可公开浏览的仓库页面。等有了公开镜像再补，避免站上出现 404 链接。
      sidebar: [
        {
          label: '开始使用',
          items: [
            { label: '简介', slug: 'index' },
            { label: '安装', slug: 'getting-started/installation' },
            { label: '快速开始', slug: 'getting-started/quick-start' },
          ],
        },
        {
          label: '核心能力',
          items: [
            { label: '中文分词', slug: 'guides/segmentation' },
            { label: '词性标注', slug: 'guides/pos-tagging' },
            { label: '命名实体识别', slug: 'guides/ner' },
            { label: '拼音', slug: 'guides/pinyin' },
            { label: '繁简转换', slug: 'guides/chinese-conversion' },
            { label: '新词发现', slug: 'guides/new-word-discovery' },
            { label: '智能高亮', slug: 'guides/highlight' },
          ],
        },
        {
          label: '扩展能力',
          items: [
            { label: 'Duckling 结构化解析', slug: 'guides/duckling' },
            { label: 'FastText 文本分类', slug: 'guides/fasttext' },
            { label: '感知机框架', slug: 'guides/perceptron' },
          ],
        },
        {
          label: '工程落地',
          items: [
            { label: '配置与数据目录', slug: 'deployment/configuration' },
            { label: '资源下载与离线部署', slug: 'deployment/resources' },
            { label: 'Spring Boot 集成', slug: 'deployment/spring-boot' },
            { label: '性能与基准测试', slug: 'deployment/benchmarks' },
          ],
        },
        {
          label: '参考',
          items: [
            { label: 'API 参考', slug: 'reference/api' },
            { label: '配置项参考', slug: 'reference/configuration' },
            { label: '架构设计', slug: 'reference/architecture' },
            { label: '示例程序', slug: 'reference/examples' },
            { label: '常见问题', slug: 'reference/faq' },
          ],
        },
      ],
      customCss: ['./src/styles/custom.css'],
      components: {
        Hero: './src/components/Hero.astro',
      },
    }),
  ],
});

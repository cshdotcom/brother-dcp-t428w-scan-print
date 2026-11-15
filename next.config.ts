import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  /* config options here */
  typescript: {
    ignoreBuildErrors: true,
  },
  reactStrictMode: false,
  // 关闭开发模式左下角悬浮"N"角标（会遮挡移动端底部导航/内容）
  devIndicators: false,
  // Z.ai 预览面板经网关以 *.zhipuaidemo.cn 域名跨域加载 /_next/* 资源，
  // 显式放行避免 dev 模式跨域拦截（Next 16 起强制要求配置）
  allowedDevOrigins: ["*.zhipuaidemo.cn"],
};

export default nextConfig;

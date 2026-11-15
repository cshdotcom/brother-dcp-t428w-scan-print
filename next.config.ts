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
};

export default nextConfig;

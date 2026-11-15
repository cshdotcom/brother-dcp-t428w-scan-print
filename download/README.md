# PaperMarker 生成产物说明

以下构建产物已全部就绪（public/ 目录，站点可直接下载）：

| 文件 | 说明 |
| --- | --- |
| `public/papermarker.apk` | Android 主客户端 APP（v1.1，相机连拍 / 透视矫正 / API Key 登录） |
| `public/PaperMarker-Bridge.apk` | Android 桥接服务 APP（v2.0.1，HTTP:3790 封装 Brother 54921/9100 裸 TCP） |
| `public/papermarker-bridge.exe` | Windows 桥接服务（x64，双击运行） |
| `public/papermarker-bridge-linux` | Linux 桥接服务（x64，`chmod +x` 后运行） |

全部使用同一签名证书（`android-app/papermarker.keystore`，口令 `papermarker2024`）。

## 重建 Android 桥接 APK

```bash
# 依赖 /home/z/android-tools/{jdk-21.0.13+9, android-14, android-34}
cd android-bridge && bash build.sh
cp PaperMarker-Bridge.apk ../public/
```

## 桥接下载源（v2.1 新增）

站点内「设置 → 网络打印机配置 → 桥接程序下载源」支持把桥接程序的下载基址
改为任意 http(s) 目录（如 GitHub Release / 局域网文件服务器），保存后所有
下载按钮自动指向 `${源}/同名文件`；留空恢复本站默认下载。

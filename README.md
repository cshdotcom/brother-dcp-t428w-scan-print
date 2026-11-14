# Brother DCP-T428W 私有协议逆向 · 扫描 + 打印

面向 **Brother DCP-T428W**（国行家用喷墨一体机）的协议逆向与可运行实现。
目标：**不依赖原厂 APP、不依赖通用协议**，用纯 Socket 自己控制这台机器的扫描与打印。

> 2026-11-14 真机实测结论：**扫描已打通**（54921 / `ESC X` / 单页 A4 300dpi 161162 字节 JPEG）；
> 打印提供 9100 直连三档实验，用于判定能否绕开系统打印框架。

## 一句话结论

| 能力 | 状态 | 依据 |
|---|---|---|
| 扫描 | ✅ **真机已打通** | 54921/TCP，`ESC X` 后直接回 10 字节头 + JPEG 块流 |
| 打印 | ⚠️ 系统框架 + 兄弟插件最稳；9100 直连待判定 | 设备端吃「兄弟自有 PJL 头 + JPEG」，光栅化在闭源插件里 |
| 复印 | ❌ 不做（需求已收窄为扫描 + 打印） | — |
| 通用协议 AirPrint / Mopria / eSCL / WSD | ❌ 本机型不支持 | 官方规格只写 USB 2.0；Mac 必须装 iPrint&Scan |

## 扫描：已实测可用的最小流程

```
TCP <打印机IP>:54921
← "+OK 200"                     （被占用时 "-NG 401"，见下文）
→ ESC X 0A
   R=300,300\n
   M=C256|CGRAY|TEXT\n
   C=JPEG\n J=MID\n B=50\n N=50\n
   A=0,0,<宽>,<高>\n
   D=SIN\n G=1\n L=128\n
   80
← 64 07 00 01 00 84 <prog> 00 00  <len_lo> <len_hi>  <JPEG…>   …重复…
← 82                            本页结束
```

- 头 10 字节：`id=0x64`(JPEG)、`magic=0x0007`、`pageId`、`0x84`、`progress`、保留 0
- 之后 2 字节小端块长 + 数据；JPEG 以 `FFD9` 收尾；单字节 `0x82` 结束本页
- **`ESC D` / `ESC E` / `ESC I` 都不是必需**：实测先发 `ESC I` 反而会因应答少读一字节导致整条流错位

踩过的坑（完整记录见 `02_扫描协议_54921.md`）：

1. `ESC K`（`1B 4B 0A 80`）是 DCP-J105 一代的开场，T428W 上**一个字节都不回**
2. `ESC I` 在代次 ≥ 2009 时**回包没有状态字节**，直接是 `长度 + csv`，按老逻辑解析必然错位
3. `-NG 401` 是会话被占用 → 发 `ESC R`（`1B 52`，唯一 2 字节命令）复位
4. `ECONNREFUSED` 多为打印机休眠，按面板键唤醒即可

## 打印：三条路线

1. **系统打印框架 → 兄弟插件**（最稳）：PDF → `PrintManager` → `com.brother.mfc.brprint.plugin` → 设备
2. **9100 直连 · 纯 JPEG**：不拼 PJL，直接发 JPEG
3. **9100 直连 · PJL + 兄弟参数族**：
   `@PJL SET BRMonoColor / BRInputSlot / BRResolution / BRColorMediaType / BRBiDir / BRReverse / BRMirror / Jpeg Printing`
   + `@PJL ENTER LANGUAGE=JPEG` + JPEG

若 2/3 不出纸但端口收了数据，说明设备拒绝渲染，下一步方向是改 JPEG 子采样（4:4:4）或先下发 `BRResolution`。

## 目录

```
01_总览与结论.md            结论与链路总览
02_扫描协议_54921.md       字节级协议 + 伪代码 + 真机实测记录
03_打印链路与实测方法.md    PDL / PJL 参数族 / 三条打印路线取舍
04_真机测试手册.md          v0.6 按步骤验证 + 会话状态处理 + 直连打印判读
05_参考资料.md              公开资料 + 原厂 SDK 字节码出处（复现方法）
BrotherTestLab-v0.6.apk     可安装测试台（minSdk 21，25~29 KB，debug 签名）
测试APK源码/                纯 Java，无 Gradle：aapt2 → javac → d8 → zipalign → apksigner
源码_BrotherMFC_Kotlin/      可嵌入自己 APP 的 Kotlin 模块（scan + print）
```

## 构建测试 APK（不需要 Gradle）

需要 JDK 17 + Android SDK（`platforms;android-34`、`build-tools;34.0.0`）：

```bash
cd 测试APK源码 && bash build.sh     # 产物 BrotherTestLab-vX.Y.apk
```

## 核心源码

- `测试APK源码/src/com/doubao/printlab/BrotherScanSdk.java` — 扫描（54921），含 `probe / scan / listenOnly / queryQ / xOnly / resetSession`
- `测试APK源码/src/com/doubao/printlab/PrintRaw.java` — 9100 直连打印三档实验
- `测试APK源码/src/com/doubao/printlab/PrintHelper.java` — PDF 生成 + `PrintManager` 调用
- `源码_BrotherMFC_Kotlin/app/src/main/java/com/example/brothermfc/scan/BrotherScan.kt` — 可直接嵌入的扫描实现

## 免责声明

仅用于学习与自用设备的互操作性研究；未上架、未分发。所有协议细节来源于对自有设备与自己持有 APK 的分析，以及公开开源实现（MIT）与公开抓包文档。

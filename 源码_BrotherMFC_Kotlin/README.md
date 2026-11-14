# Brother DCP-T428W 扫描 / 打印自研模块（自用版）

只做两件事：**扫描、打印**。复印已移除，无第三方 SDK 依赖，无成品 UI。

- 扫描：**54921/TCP 私有协议，已按真实帧格式实现，可直接跑**（不再需要抓包补全）。
- 打印：走 **Android 打印框架 + 兄弟官方打印服务插件**（PDL 由插件负责），另留一条 9100 实验通道。
- 复印：不做（远程复印虽在原厂 SDK 里存在，但按需求已移除）。

---

## 一、传输矩阵（修正后）

| 方向 | 传输 | 端口 | 状态 |
|---|---|---|---|
| 扫描 | ScanCommand（私有，ESC 命令 + 二进制块） | **54921/TCP** | ✅ 已实现 |
| 扫描 | eSCL / WSScan | — | ❌ 本机型不支持 |
| 打印 | Android 打印框架 → 兄弟打印服务插件 | 由插件决定 | ✅ 推荐 |
| 打印 | 9100 RAW（裸 JPEG / PJL 包裹） | 9100 | ⚠️ 实验，待真机验证 |
| 打印 | IPP Print-Job | 631 `/ipp/print` | ⚠️ 本机型无标注，保留代码备用 |
| 发现 | mDNS / SNMP | 5353 / 161 | ✅ |
| 按钮触发（可选） | SNMP set 注册 + UDP 通知 | 161 / 54925(udp) | 文档齐全，未实现 |

> 注：先前版本把扫描写成 9102，那是老款商用机端口，**本机型为 54921**，已修正。

## 二、扫描协议（54921）——完整帧格式（v0.2，按原厂 SDK 字节码）

所有命令以 `0x1B`(ESC) 开头、以 `0x80` 结尾；命令字后跟 `0x0A` 再跟 `键=值\n` 正文。

```
连接 → 设备先发 "+OK 200\r\n"（忙时 "-NG 401\r\n"）
→ 1B 44 0A "AUTO\n" 80                          纸源（ESC 'D'）         ← 1 字节，0x80 = OK
→ 1B 45 0A "R=200,200\nM=CGRAY\n" 80           初始探测（ESC 'E'）      ← 00 + len(u16 LE) + 键值文本
→ 1B 49 0A "R=..\nM=..\nD=SIN\n" 80           下发参数（ESC 'I'）      ← 00 + len(u16 LE) + 7 个整数
→ 1B 58 0A R/M/C/J/B/N/A/D/G/L 80                执行扫描（ESC 'X'）
← 图像流：1 字节位域头 + 2 字节块长 + 负载 …… 循环
```

**不要发 ESC 'K'**：`1B 4B 0A 80` 只适用于 DCP-J105 一代；在 DCP-T428W 上实测无任何回包。

图像流头字节：
- `h & 0x80 == 0` → 数据块；压缩 = `h & 3`（0 NONE / 1 RLENGTH / 2 MH / 3 JPEG），色彩 = `(h>>2)&7`，bit5 置位表示 JPEG；随后 2 字节小端块长。
- `h & 0x80 != 0` → 控制头（0x80..0x87：EdgeInformation / 本页结束 / 全部结束），随后 2 字节小端长度 + 数据。

`ESC I` 回包的 7 个整数：0 = xdpi、1 = ydpi、**3 = 宽、4 = 高**（与老开源的 4/6 不同）。

参数键：`R=分辨率x,y`、`M=TEXT|CGRAY|GRAY256|C256|ERRDIF`、`C=NONE|RLENGTH|MH|JPEG`、`J=MID`、`B=亮度50`、`N=对比度50`、`A=x,y,w,h`、`D=SIN|DUP`、`G=1`、`L=128`。


## 三、扫描参数（APK 内 `ScanCommandParameters` 枚举，供扩展用）

| 枚举 | 取值 |
|---|---|
| ScanMode | TEXT / CGRAY / GRAY256 / C256 / ERRDIF |
| DataCompression | NONE / RLENGTH / MH / JPEG |
| ScanImageDataFormat | Bitmap_BlackAndWhite / Gray8 / RGB8 / RGB24 / BGR24 / R_G_B_24 / Jpeg |
| ColorType | Monochrome / TrueGray8bits / Color24bits / ColorIndices16 / ColorIndices256 / R / G / B / RGB / BGR |
| ProtocolGeneration | Pre1999 … 2014_2 |
| CarrierSheet / ErrorDetect | NoSupport/ADS；NoDetect/MultiPaperFeedDetect/LessThanB4InADF |

`ScanState` 48 项、`PrintState` 14 项状态码同样来自 APK 实测。

## 四、打印 PDL 事实

`PrinterPDL` 枚举：`PWGRaster`、`BrJpc_PWGRaster`、`Jpeg_BH9/BH11/BHS13/BHmini11`、`PCL_BrotherMonochrome`、`PostScript`。

兄弟 CUPS 驱动参数族（官方 FAQ）：`PageSize`/`media`、`BRInputSlot=AutoSelect`、`Resolution`、`BRMonoColor=Color`、`BRSlowDrying`、`BRColorMediaType=Plain Paper`、`BRColorPaperThick`、`BRBiDir`、`BRReverse`、`BRMirror`、`Jpeg Printing`
→ 喷墨机走 **JPEG + 兄弟自有 PJL 参数头**，光栅化/封装在兄弟自己的转换器里完成。

因此打印侧最稳的做法：**PDF 交给系统打印框架，由兄弟打印服务插件生成 PDL**。

## 五、代码结构

```
app/src/main/java/com/example/brothermfc/
├── MainActivity.kt              调试台：发现 / SNMP / 打印（系统）/ 9100 实验 / 扫描
├── net/MdnsDiscovery.kt         mDNS 发现
├── net/SnmpClient.kt            SNMP GET（完整 BER）
├── print/AndroidPrint.kt        系统打印框架：文本/图片 → PDF → PrintManager
├── print/PrintClient.kt         9100 RAW / IPP Print-Job / PrintState 映射
└── scan/BrotherScan.kt          54921 完整实现：握手 + 参数交换 + 帧解析 + 落盘
```

每个文件只依赖 JDK/Android 标准库，可单独拷走。

## 六、真机验证

1. 同网段，SNMP 能读到状态。
2. 扫描：平板放 A4 → 点「扫描」→ 日志给出 `DPI`、`像素`，图片存到 `Android/data/<包名>/files/scans/scan_0.jpg`。
   - 无数据：先关掉原厂 iPrint&Scan（会占 54921）；再降分辨率重试。
3. 打印：装「Brother Print Service Plugin」→ 系统打印面板选 DCP-T428W。
4. 可选：`9100` 实验通道验证设备是否接受裸 JPEG。

## 七、编译

```
cd BrotherMFC
./gradlew :app:assembleDebug   # JDK 17 + SDK 34
```

包名 `com.example.brothermfc`，按需替换。

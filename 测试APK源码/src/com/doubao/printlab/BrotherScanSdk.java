package com.doubao.printlab;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 54921 扫描协议 v0.3 —— 按 iPrint&Scan 6.14.2 内 SDK 字节码实现。
 *
 * 命令序列：ESC D（纸源）→ ESC E（初始探测）→ ESC I（参数）→ ESC X（执行）→ 图像流
 *
 * 应答格式按协议代次不同（关键修正）：
 *   ProtocolGeneration >= 2009（T428W 属于这一档）：
 *     ESC I 的回包 **没有状态字节**，直接是 [长度 u16 小端][逗号分隔 7 个整数]
 *   代次 < 2009 才是 [状态 0x00][长度][数据]
 *   ESC E 始终是 [状态][长度][键值文本]；ESC D 只回 1 字节（0x80 = OK）。
 *
 * 图像流头字节 h：
 *   h & 0x80 == 0 → 数据块，压缩 = h&3（0 NONE / 1 RLENGTH / 2 MH / 3 JPEG），色彩 = (h>>2)&7
 *   h & 0x80 != 0 → 控制头（0x82 一族的页结束 / 全部结束）
 */
public class BrotherScanSdk {

    public static final int PORT = 54921;
    private static final int ESC = 0x1B;
    private static final int END = 0x80;

    public static class Result {
        public boolean ok;
        public String message;
        public int xdpi = 300, ydpi = 300;
        public int width, height;
        public int pages;
        public byte[] jpeg;
        public String path;
        public StringBuilder trace = new StringBuilder();
    }

    private Socket sock;
    private InputStream in;
    private OutputStream out;
    private final StringBuilder log = new StringBuilder();
    private int pages = 0;

    public Result probe(String host, int dpi, String mode, int timeoutMs) {
        Result r = new Result();
        r.trace = log;
        try {
            open(host, timeoutMs);

            byte st = sendStatus("D", "AUTO\n");
            log.append("ESC D -> 0x").append(u8(st)).append('\n');

            st = sendStatus("E", "R=" + dpi + "," + dpi + "\nM=" + mode + "\n");
            log.append("ESC E -> 0x").append(u8(st)).append('\n');
            if (st == 0) {
                byte[] payload = readPayload();
                log.append("ESC E payload: ").append(new String(payload)).append('\n');
            }
            r.ok = true;
            r.message = "探测完成";
            return r;
        } catch (Exception e) {
            r.message = "异常：" + e.getClass().getSimpleName() + " " + e.getMessage();
            return r;
        } finally {
            close();
        }
    }

    /**
     * 真机验证过的扫描流程（2026-11-14 实测：单页 161162 字节 JPEG）：
     *   welcome "+OK 200" → 直接发 ESC X → 图像流
     * 实测：ESC D / E / I 都不是必需；ESC I 解析错反而会把会话搞坏。
     * 图流：10 字节头（id=0x64 JPEG, magic=0x0007, pageId, 0x84, progress, 0,0）
     *       + 2 字节小端长度 + 数据；0xFFD9 结束 JPEG；0x82 本页结束。
     */
    public Result scan(String host, int dpi, String mode, int brightness, int contrast,
                       int timeoutMs, java.io.File outDir) {
        Result r = new Result();
        r.trace = log;
        try {
            open(host, timeoutMs);
            r.xdpi = dpi;
            r.ydpi = dpi;
            int w = mm2px(210, dpi);
            int h = mm2px(297, dpi);

            StringBuilder x = new StringBuilder();
            x.append("R=").append(dpi).append(',').append(dpi).append('\n');
            x.append("M=").append(mode).append('\n');
            x.append("C=JPEG\n");
            x.append("J=MID\n");
            x.append("B=").append(brightness).append('\n');
            x.append("N=").append(contrast).append('\n');
            x.append("A=0,0,").append(w).append(',').append(h).append('\n');
            x.append("D=SIN\n");
            x.append("G=1\n");
            x.append("L=128\n");
            sendFrame("X", x.toString());
            log.append("ESC X sent: R=").append(dpi).append(" A=0,0,").append(w).append(',').append(h).append('\n');

            ByteArrayOutputStream cur = new ByteArrayOutputStream();
            int pageIdx = 0;
            int guard = 0;
            while (guard++ < 100000) {
                byte[] hdr = readN(10);
                if (hdr == null || hdr.length == 0) { log.append("流结束(EOF)\n"); break; }
                if (hdr.length < 10) {
                    log.append("尾部 ").append(hdr.length).append(" 字节：").append(hex(hdr, 10)).append('\n');
                    break;
                }
                int id = hdr[0] & 0xFF;
                if (id == 0x80) { log.append("收到结束标记 0x80\n"); break; }
                int magic = (hdr[1] & 0xFF) | ((hdr[2] & 0xFF) << 8);
                int pageId = (hdr[3] & 0xFF) | ((hdr[4] & 0xFF) << 8);
                int progress = (hdr[6] & 0xFF) | ((hdr[7] & 0xFF) << 8);
                if (magic != 0x0007) {
                    log.append("包 magic 异常 ").append(magic).append("，头：").append(hex(hdr, 10)).append('\n');
                    break;
                }
                if (id == 0x82) {
                    log.append("本页结束 pageId=").append(pageId).append('\n');
                    if (cur.size() > 0) {
                        pages++;
                        byte[] data = cur.toByteArray();
                        if (r.jpeg == null) r.jpeg = data;
                        savePage(outDir, pageIdx, data, r);
                        log.append("  page ").append(pageIdx).append(" ").append(data.length).append(" 字节\n");
                        pageIdx++;
                        cur = new ByteArrayOutputStream();
                    }
                    continue;
                }
                byte[] lenB = readN(2);
                if (lenB == null || lenB.length < 2) break;
                int len = (lenB[0] & 0xFF) | ((lenB[1] & 0xFF) << 8);
                log.append("块 id=0x").append(Integer.toHexString(id))
                   .append(" page=").append(pageId)
                   .append(" 进度=").append(progress)
                   .append(" len=").append(len).append('\n');
                int left = len;
                while (left > 0) {
                    byte[] chunk = readN(Math.min(left, 4096));
                    if (chunk == null || chunk.length == 0) break;
                    cur.write(chunk); left -= chunk.length;
                }
                r.width = w;
                r.height = h;
            }

            if (cur.size() > 0) {
                pages++;
                byte[] data = cur.toByteArray();
                if (r.jpeg == null) r.jpeg = data;
                savePage(outDir, pageIdx, data, r);
                log.append("  page ").append(pageIdx).append(" ").append(data.length).append(" 字节(尾)\n");
            }

            r.pages = pages;
            r.ok = pages > 0;
            r.message = r.ok ? ("成功 " + pages + " 页") : "未收到图像数据";
            return r;
        } catch (Exception e) {
            r.message = "异常：" + e.getClass().getSimpleName() + " " + e.getMessage();
            return r;
        } finally {
            close();
        }
    }

    private void savePage(java.io.File outDir, int idx, byte[] data, Result r) {
        if (outDir == null) return;
        try {
            outDir.mkdirs();
            java.io.File f = new java.io.File(outDir, "scan_" + idx + ".jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            fos.write(data);
            fos.close();
            r.path = f.getAbsolutePath();
        } catch (Exception ignored) { }
    }

    /**
     * ESC I 回包：代次 >= 2009 是 [长度 u16 小端][csv]，无状态字节；老代次是 [0x00][长度][csv]。
     * 兼容判读：首字节 0x00 当状态字节；否则当长度低字节。（当前 scan() 已不使用，保留供诊断）
     */
    private byte[] readParamReply() throws Exception {
        int b1 = in.read();
        if (b1 < 0) return new byte[0];
        if ((b1 & 0xFF) == 0x00) {
            log.append("ESC I 走状态字节分支（老代次格式）\n");
            byte[] lb = readN(2);
            if (lb == null) return new byte[0];
            int len = (lb[0] & 0xFF) | ((lb[1] & 0xFF) << 8);
            byte[] d = readN(len);
            return d == null ? new byte[0] : d;
        }
        int b2 = in.read();
        if (b2 < 0) return new byte[0];
        int len = (b1 & 0xFF) | ((b2 & 0xFF) << 8);
        log.append("ESC I 走长度优先分支 len=").append(len).append('\n');
        if (len < 3 || len > 4096) {
            log.append("长度不合理，按状态字节处理\n");
            byte[] lb = readN(2);
            if (lb == null) return new byte[0];
            int l2 = (lb[0] & 0xFF) | ((lb[1] & 0xFF) << 8);
            byte[] d = readN(l2);
            return d == null ? new byte[0] : d;
        }
        byte[] d = readN(len);
        return d == null ? new byte[0] : d;
    }

    private int[] parseInts(String s) {
        String[] arr = s.replace("\r", "").replace("\n", "").trim().split(",");
        int[] v = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            try { v[i] = Integer.parseInt(arr[i].trim()); } catch (Exception e) { v[i] = 0; }
        }
        return v;
    }

    private int mm2px(double mm, int dpi) {
        return (int) Math.round(mm / 25.4 * dpi);
    }

    private String u8(byte b) {
        return Integer.toHexString(b & 0xFF);
    }

    /** 只连接、不发任何命令，连续监听 seconds 秒，把所有收到的字节按原始 hex 打出来 */
    public Result listenOnly(String host, int seconds, int timeoutMs) {
        Result r = new Result();
        r.trace = log;
        try {
            open(host, timeoutMs);
            log.append("已连接，开始裸监听 ").append(seconds).append(" 秒（不发任何命令）\n");
            dumpFor(seconds);
            r.ok = true; r.message = "监听结束";
            return r;
        } catch (Exception e) {
            r.message = "异常：" + e.getClass().getSimpleName() + " " + e.getMessage();
            return r;
        } finally { close(); }
    }

    /** 发 ESC Q（查询设备能力大结构），只打原始字节，不做解析 */
    public Result queryQ(String host, int timeoutMs) {
        Result r = new Result();
        r.trace = log;
        try {
            open(host, timeoutMs);
            sendFrame("Q", "");
            log.append("已发 ESC Q，等待回包\n");
            dumpFor(5);
            r.ok = true; r.message = "ESC Q 查询结束";
            return r;
        } catch (Exception e) {
            r.message = "异常：" + e.getClass().getSimpleName() + " " + e.getMessage();
            return r;
        } finally { close(); }
    }

    /** 只发 ESC X（不先发 D/E/I），然后裸监听：用来判断 X 单独是否被设备接受 */
    public Result xOnly(String host, int dpi, int width, int height, int seconds, int timeoutMs) {
        Result r = new Result();
        r.trace = log;
        try {
            open(host, timeoutMs);
            StringBuilder x = new StringBuilder();
            x.append("R=").append(dpi).append(',').append(dpi).append('\n');
            x.append("M=CGRAY\n");
            x.append("C=JPEG\n");
            x.append("J=MID\n");
            x.append("B=50\n");
            x.append("N=50\n");
            x.append("A=0,0,").append(width).append(',').append(height).append('\n');
            x.append("D=SIN\n");
            x.append("G=1\n");
            x.append("L=128\n");
            sendFrame("X", x.toString());
            log.append("只发 ESC X：A=0,0,").append(width).append(',').append(height).append('\n');
            dumpFor(seconds);
            r.ok = true; r.message = "ESC X 裸发结束";
            return r;
        } catch (Exception e) {
            r.message = "异常：" + e.getClass().getSimpleName() + " " + e.getMessage();
            return r;
        } finally { close(); }
    }

    /** 在 seconds 秒内不停收字节，按"每收到一段就逐字节 hex"输出；超时不算错误 */
    private void dumpFor(int seconds) throws Exception {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        while (System.currentTimeMillis() < deadline) {
            try {
                int b = in.read();
                if (b < 0) { log.append("对端关闭连接\n"); break; }
                all.write(b);
                if (all.size() % 64 == 0) {
                    log.append("  [").append(all.size()).append("B] ").append(hexTail(all.toByteArray(), 64)).append('\n');
                }
            } catch (java.net.SocketTimeoutException ste) {
                continue;
            }
        }
        byte[] data = all.toByteArray();
        if (data.length == 0) {
            log.append("  这 ").append(seconds).append(" 秒内设备一个字节都没发\n");
        } else {
            log.append("共收到 ").append(data.length).append(" 字节，完整 hex:\n");
            log.append(hex(data, Math.min(data.length, 400)).replace(" ", "")).append('\n');
        }
    }

    private String hexTail(byte[] b, int n) {
        int from = Math.max(0, b.length - n);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < b.length; i++) sb.append(String.format("%02x ", b[i] & 0xFF));
        return sb.toString();
    }

    /** 只做一件事：连上去发 ESC R 释放被占用的会话（用于 -NG 401） */
    public Result resetSession(String host, int timeoutMs) {
        Result r = new Result();
        r.trace = log;
        try {
            sock = new Socket();
            sock.connect(new InetSocketAddress(host, PORT), 6000);
            sock.setSoTimeout(timeoutMs);
            in = sock.getInputStream();
            out = sock.getOutputStream();
            String w = readLine();
            log.append("welcome: ").append(w).append('\n');
            out.write(new byte[]{(byte) ESC, 0x52});
            out.flush();
            log.append("已发 ESC R（1B 52）\n");
            Thread.sleep(500);
            r.ok = true;
            r.message = "已发送会话复位";
            return r;
        } catch (Exception e) {
            r.message = "复位异常：" + e.getClass().getSimpleName() + " " + e.getMessage();
            return r;
        } finally {
            try { if (sock != null) sock.close(); } catch (Exception ignored) { }
        }
    }

    // ---------------- 底层 ----------------

    /**
     * 建会话：连上后设备先发欢迎语。
     *  - "+OK 200"  → 可用
     *  - "-NG 401"  → 会话被占用：先发 ESC R（1B 52）复位会话，断开，等 2.5s 再试（最多 3 次）
     *  - 连接被拒    → 多为打印机休眠 / 54921 未监听：等 2s 重试
     */
    private void open(String host, int timeoutMs) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                sock = new Socket();
                sock.connect(new InetSocketAddress(host, PORT), 6000);
                sock.setSoTimeout(timeoutMs);
                in = sock.getInputStream();
                out = sock.getOutputStream();
                String welcome = readLine();
                log.append("welcome(第").append(attempt).append("次): ").append(welcome).append('\n');
                if (welcome.startsWith("+")) return;
                if (welcome.contains("401")) {
                    log.append("会话被占用，发 ESC R 复位后重试\n");
                    try { out.write(new byte[]{(byte) ESC, 0x52}); out.flush(); } catch (Exception ignored) { }
                    try { if (sock != null) sock.close(); } catch (Exception ignored) { }
                    Thread.sleep(2500);
                    continue;
                }
                throw new Exception("设备未就绪：" + welcome);
            } catch (java.net.ConnectException ce) {
                last = ce;
                log.append("连接被拒（打印机休眠 / 端口未监听），").append(attempt).append("/3，等 2s\n");
                try { if (sock != null) sock.close(); } catch (Exception ignored) { }
                Thread.sleep(2000);
            }
        }
        throw last != null ? last : new java.io.IOException("无法建立扫描会话（多次被 401 占用）");
    }

    /** 关闭前发 ESC R（只有 2 字节，无 0x0A / 0x80），让设备释放会话，避免后续 401 */
    private void close() {
        try {
            if (sock != null && out != null) {
                out.write(new byte[]{(byte) ESC, 0x52});
                out.flush();
            }
        } catch (Exception ignored) { }
        try { if (sock != null) sock.close(); } catch (Exception ignored) { }
    }

    private void sendFrame(String cmd, String text) throws Exception {
        out.write(new byte[]{(byte) ESC, (byte) cmd.charAt(0), 0x0A});
        out.write(text.getBytes("UTF-8"));
        out.write((byte) END);
        out.flush();
    }

    private byte sendStatus(String cmd, String text) throws Exception {
        sendFrame(cmd, text);
        int b = in.read();
        return b < 0 ? (byte) -1 : (byte) b;
    }

    private byte[] readPayload() throws Exception {
        byte[] lb = readN(2);
        if (lb == null) return new byte[0];
        int len = (lb[0] & 0xFF) | ((lb[1] & 0xFF) << 8);
        byte[] data = readN(len);
        return data == null ? new byte[0] : data;
    }

    private String readLine() throws Exception {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int b = in.read();
            if (b < 0 || b == 0x0A) break;
            if (b != 0x0D) sb.append((char) b);
        }
        return sb.toString();
    }

    private byte[] readN(int n) throws Exception {
        if (n <= 0) return new byte[0];
        byte[] buf = new byte[n];
        int read = 0;
        while (read < n) {
            int rr = in.read(buf, read, n - read);
            if (rr < 0) return read == 0 ? null : java.util.Arrays.copyOf(buf, read);
            read += rr;
        }
        return buf;
    }

    public static String hex(byte[] b, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(b.length, max); i++) {
            sb.append(String.format("%02x ", b[i] & 0xFF));
        }
        if (b.length > max) sb.append("...(").append(b.length).append("B)");
        return sb.toString();
    }
}

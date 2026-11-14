package com.doubao.printlab;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Brother 网络扫描（TCP 54921）纯 Java 实现。
 *
 * 协议依据：
 *   brother-mfc.sourceforge.net/network.html（原始抓包与字段注释）
 *   github.com/rumpeltux/brother-scand 的 data_channel.c（MIT）
 *
 * 帧语法：所有命令 0x1B 开头、0x80 结尾，字段以 0x0A 分隔，长度小端。
 */
public class BrotherScan {

    public static final int PORT = 54921;
    private static final int ESC = 0x1B;
    private static final int END = 0x80;

    public static class Param {
        public int dpi = 200;
        public String mode = "CGRAY";     // TEXT / CGRAY / GRAY256 / C256 / ERRDIF
        public int brightness = 50;
        public int contrast = 50;
        public String compression = "CJPEG";
        public String quality = "MID";
        public String area = null;        // "x,y,w,h"，null 用设备回报尺寸
        public String paperSource = "AUTO";
    }

    public static class Page {
        public int index;
        public byte[] data;
        public String format;
        public String path;
    }

    public static class Result {
        public boolean ok;
        public String message;
        public int xdpi, ydpi, width, height;
        public List<Page> pages = new ArrayList<>();
        public StringBuilder trace = new StringBuilder();
    }

    public static Result scan(String host, Param p, int timeoutMs, File outDir) {
        Result r = new Result();
        Socket sock = null;
        try {
            sock = new Socket();
            sock.connect(new InetSocketAddress(host, PORT), 6000);
            sock.setSoTimeout(timeoutMs);
            InputStream in = sock.getInputStream();
            OutputStream out = sock.getOutputStream();

            String welcome = readLine(in);
            r.trace.append("welcome: ").append(welcome).append('\n');
            if (!welcome.startsWith("+")) {
                r.message = "设备未就绪：" + welcome;
                return r;
            }

            send(out, ESC, 'K', 0x0A, END);
            byte[] first = readPacket(in);
            r.trace.append("pkt1 len=").append(first.length).append('\n');
            Map<String, String> dev = new HashMap<>();
            if (!(first.length == 1 && first[0] == (byte) 0xD0)) {
                if (first.length == 0 || first[0] != 0x30) {
                    r.message = "初始参数帧首字节非 0x30";
                    return r;
                }
                parseParams(first, 3, first.length - 2, dev);
            }
            r.trace.append("devParams=").append(dev).append('\n');

            String res = dev.containsKey("R") ? dev.get("R") : String.valueOf(p.dpi);
            if (!res.contains(",")) res = res + "," + res;

            StringBuilder sb = new StringBuilder();
            sb.append("R=").append(res).append('\n');
            sb.append("M=").append(p.mode).append('\n');
            sb.append("D=").append(p.paperSource).append('\n');
            sendFrame(out, 'I', sb.toString());

            byte[] ex = readPacket(in);
            r.trace.append("pkt2 len=").append(ex.length).append('\n');
            if (ex.length < 5 || ex[0] != 0x00 || ex[2] != 0x00) {
                r.message = "交换参数帧格式异常：" + hex(ex, 24);
                return r;
            }
            String nums = new String(ex, 3, ex.length - 5, java.nio.charset.StandardCharsets.US_ASCII)
                    .replace("\u0000", "").replace("\n", "").replace("\r", "");
            r.trace.append("nums=").append(nums).append('\n');
            String[] arr = nums.split(",");
            int[] v = new int[8];
            for (int i = 0; i < arr.length && i < 8; i++) {
                try { v[i] = Integer.parseInt(arr[i].trim()); } catch (Exception ignored) { }
            }
            r.xdpi = v[0] > 0 ? v[0] : p.dpi;
            r.ydpi = v[1] > 0 ? v[1] : p.dpi;
            r.width = v[4];
            r.height = v[6];

            String area = p.area != null ? p.area : ("0,0," + r.width + "," + r.height);
            StringBuilder x = new StringBuilder();
            x.append("R=").append(r.xdpi).append(',').append(r.ydpi).append('\n');
            x.append("M=").append(p.mode).append('\n');
            x.append("C=").append(p.compression).append('\n');
            x.append("J=").append(p.quality).append('\n');
            x.append("B=").append(p.brightness).append('\n');
            x.append("N=").append(p.contrast).append('\n');
            x.append("A=").append(area).append('\n');
            sendFrame(out, 'X', x.toString());
            r.trace.append("issued X, area=").append(area).append('\n');

            ByteArrayOutputStream cur = new ByteArrayOutputStream();
            String curFormat = "raw";
            int pageIndex = 0;

            while (true) {
                byte[] hdr = readN(in, 10);
                if (hdr == null || hdr.length < 10) break;
                int magic = (hdr[1] & 0xFF) | ((hdr[2] & 0xFF) << 8);
                if (magic != 0x07) {
                    r.message = "包头 magic 非 0x07（" + magic + "）";
                    break;
                }
                int id = hdr[0] & 0xFF;
                int pageId = (hdr[3] & 0xFF) | ((hdr[4] & 0xFF) << 8);
                int progress = (hdr[6] & 0xFF) | ((hdr[7] & 0xFF) << 8);
                r.trace.append("hdr id=0x").append(Integer.toHexString(id))
                        .append(" page=").append(pageId).append(" prog=").append(progress).append('\n');

                if (id == 0x40 || id == 0x42 || id == 0x64) {
                    curFormat = id == 0x40 ? "raw" : (id == 0x42 ? "rle" : "jpg");
                    byte[] lenB = readN(in, 2);
                    if (lenB == null || lenB.length < 2) break;
                    int len = (lenB[0] & 0xFF) | ((lenB[1] & 0xFF) << 8);
                    int left = len;
                    while (left > 0) {
                        byte[] chunk = readN(in, Math.min(left, 4096));
                        if (chunk == null || chunk.length == 0) break;
                        cur.write(chunk, 0, chunk.length);
                        left -= chunk.length;
                    }
                    r.trace.append("  chunk ").append(len).append(" bytes, total ")
                            .append(cur.size()).append('\n');
                } else if (id == 0x82) {
                    Page pg = new Page();
                    pg.index = pageIndex;
                    pg.data = cur.toByteArray();
                    pg.format = curFormat;
                    if (outDir != null) {
                        outDir.mkdirs();
                        File f = new File(outDir, "scan_" + pageIndex + "." + curFormat);
                        writeFile(f, pg.data);
                        pg.path = f.getAbsolutePath();
                    }
                    r.pages.add(pg);
                    r.trace.append("  page ").append(pageIndex).append(" done, ")
                            .append(pg.data.length).append(" bytes\n");
                    pageIndex++;
                    cur = new ByteArrayOutputStream();
                } else {
                    byte[] one = readN(in, 1);
                    if (one != null && one.length == 1 && one[0] == (byte) END) break;
                }
            }

            r.ok = !r.pages.isEmpty();
            r.message = r.ok ? ("成功 " + r.pages.size() + " 页") : "未收到图像数据";
            return r;
        } catch (Exception e) {
            r.message = "异常：" + e.getClass().getSimpleName() + " " + e.getMessage();
            return r;
        } finally {
            if (sock != null) try { sock.close(); } catch (Exception ignored) { }
        }
    }

    // ---------- 底层 ----------

    private static void send(OutputStream out, int... bytes) throws Exception {
        byte[] b = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) b[i] = (byte) bytes[i];
        out.write(b);
        out.flush();
    }

    private static void sendFrame(OutputStream out, int cmd, String text) throws Exception {
        byte[] body = text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        out.write(new byte[]{(byte) ESC, (byte) cmd, 0x0A});
        out.write(body);
        out.write((byte) END);
        out.flush();
    }

    private static byte[] readPacket(InputStream in) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        while (true) {
            int b = in.read();
            if (b < 0) break;
            buf.write(b);
            if (b == END) break;
            if (buf.size() > 8192) break;
        }
        return buf.toByteArray();
    }

    private static byte[] readN(InputStream in, int n) throws Exception {
        byte[] out = new byte[n];
        int read = 0;
        while (read < n) {
            int r = in.read(out, read, n - read);
            if (r < 0) return read == 0 ? null : java.util.Arrays.copyOf(out, read);
            read += r;
        }
        return out;
    }

    private static String readLine(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int b = in.read();
            if (b < 0 || b == 0x0A) break;
            if (b != 0x0D) sb.append((char) b);
        }
        return sb.toString();
    }

    private static void parseParams(byte[] buf, int from, int to, Map<String, String> out) {
        int i = from;
        while (i < to) {
            char id = (char) (buf[i] & 0xFF);
            if (i + 1 >= to || buf[i + 1] != '=') { i++; continue; }
            i += 2;
            StringBuilder sb = new StringBuilder();
            while (i < to && buf[i] != 0x0A) { sb.append((char) (buf[i] & 0xFF)); i++; }
            out.put(String.valueOf(id), sb.toString());
            i++;
        }
    }

    public static String hex(byte[] b, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(b.length, max); i++) {
            sb.append(String.format("%02x ", b[i] & 0xFF));
        }
        return sb.toString();
    }

    private static void writeFile(File f, byte[] data) throws Exception {
        java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
        try { fos.write(data); } finally { fos.close(); }
    }
}

package com.doubao.printlab;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 9100 RAW 直连打印实验（不经过 Android 系统打印框架 / 兄弟插件）。
 *
 * 背景：DCP-T428W 是喷墨一体机，原厂链路是  PDF/图片 → 兄弟打印服务插件 → 光栅化成
 * "兄弟自有 PJL 头 + JPEG" → 9100 发给设备。也就是说设备端吃的是 JPEG + PJL，
 * 而 PJL 字段名来自兄弟 CUPS 驱动（BRMonoColor / BRInputSlot / BRResolution ...）。
 *
 * 这里提供三种发法，用来实测"到底能不能绕开插件直接打"：
 *   variant 0：纯 JPEG，连 PJL 都不带
 *   variant 1：PJL JOB + ENTER LANGUAGE=JPEG 的最简包裹
 *   variant 2：PJL JOB + 兄弟参数族（BRMonoColor/BRInputSlot/BRMediaType 等）+ JPEG
 *
 * 打印类端口通常是 9100/TCP（RAW），部分机型 515(LPR) / 631(IPP)。
 */
public class PrintRaw {

    public static final int PORT_RAW = 9100;

    public static class Result {
        public boolean ok;
        public String message;
        public StringBuilder trace = new StringBuilder();
    }

    private static final byte[] UEL = {0x1B, 0x25, 0x2D, 0x31, 0x32, 0x33, 0x34, 0x35, 0x58}; // ESC %-12345X

    public Result send(String host, byte[] jpeg, int variant, int timeoutMs) {
        Result r = new Result();
        Socket sock = null;
        try {
            sock = new Socket();
            sock.connect(new InetSocketAddress(host, PORT_RAW), 6000);
            sock.setSoTimeout(timeoutMs);
            OutputStream out = sock.getOutputStream();
            InputStream in = sock.getInputStream();

            byte[] body = wrap(jpeg, variant, r.trace);
            r.trace.append("variant=").append(variant)
                    .append(" 发送 ").append(body.length).append(" 字节到 ").append(host)
                    .append(':').append(PORT_RAW).append('\n');

            out.write(body);
            out.flush();

            // 打印类端口通常不回数据；有回就说明被拒/需要交互
            StringBuilder back = new StringBuilder();
            try {
                sock.setSoTimeout(3000);
                int b;
                int n = 0;
                while (n < 512 && (b = in.read()) >= 0) { back.append((char) b); n++; }
            } catch (java.net.SocketTimeoutException ignored) { }
            if (back.length() > 0) r.trace.append("设备回：").append(back).append('\n');
            else r.trace.append("设备无回包（正常，RAW 端口一般静默）\n");

            r.ok = true;
            r.message = "已发送，去看打印机是否出纸";
            return r;
        } catch (Exception e) {
            r.message = "异常：" + e.getClass().getSimpleName() + " " + e.getMessage();
            return r;
        } finally {
            try { if (sock != null) sock.close(); } catch (Exception ignored) { }
        }
    }

    private byte[] wrap(byte[] jpeg, int variant, StringBuilder trace) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        if (variant == 0) {
            bos.write(jpeg);
            trace.append("纯 JPEG，无 PJL\n");
            return bos.toByteArray();
        }
        if (variant == 1) {
            bos.write(UEL);
            bos.write("@PJL JOB NAME=\"Doubao Test\"\r\n".getBytes("US-ASCII"));
            bos.write("@PJL ENTER LANGUAGE=JPEG\r\n".getBytes("US-ASCII"));
            bos.write(jpeg);
            bos.write("\r\n".getBytes("US-ASCII"));
            bos.write(UEL);
            bos.write("@PJL EOJ\r\n".getBytes("US-ASCII"));
            trace.append("PJL 最简包裹（ENTER LANGUAGE=JPEG）\n");
            return bos.toByteArray();
        }
        // variant 2：兄弟参数族
        bos.write(UEL);
        bos.write("@PJL JOB NAME=\"Doubao Test\"\r\n".getBytes("US-ASCII"));
        bos.write("@PJL SET BRMonoColor=FullColor\r\n".getBytes("US-ASCII"));
        bos.write("@PJL SET BRInputSlot=AutoSelect\r\n".getBytes("US-ASCII"));
        bos.write("@PJL SET BRResolution=Normal\r\n".getBytes("US-ASCII"));
        bos.write("@PJL SET BRColorMediaType=Plain Paper\r\n".getBytes("US-ASCII"));
        bos.write("@PJL SET BRColorPaperThick=0\r\n".getBytes("US-ASCII"));
        bos.write("@PJL SET BRBiDir=ON\r\n".getBytes("US-ASCII"));
        bos.write("@PJL SET BRReverse=OFF\r\n".getBytes("US-ASCII"));
        bos.write("@PJL SET BRMirror=OFF\r\n".getBytes("US-ASCII"));
        bos.write("@PJL SET Jpeg Printing=ON\r\n".getBytes("US-ASCII"));
        bos.write("@PJL ENTER LANGUAGE=JPEG\r\n".getBytes("US-ASCII"));
        bos.write(jpeg);
        bos.write("\r\n".getBytes("US-ASCII"));
        bos.write(UEL);
        bos.write("@PJL EOJ\r\n".getBytes("US-ASCII"));
        trace.append("PJL + 兄弟参数族（BRMonoColor 等）\n");
        return bos.toByteArray();
    }
}

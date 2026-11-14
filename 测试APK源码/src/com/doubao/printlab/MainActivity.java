package com.doubao.printlab;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * v0.2 测试台：扫描按 iPrint&Scan 6.14.2 内 SDK 的真实命令序列（ESC D / E / I / X），不再用 ESC K。
 */
public class MainActivity extends Activity {

    private EditText etIp, etDpi;
    private CheckBox cbColor;
    private TextView tvLog;
    private ImageView ivPreview;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(12);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Brother DCP-T428W 扫描 / 打印 测试台 v0.6");
        title.setTextSize(16);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("v0.6：扫描改成实测可用的 ESC X 直连流程；新增 9100 直连打印（纯 JPEG / PJL / 兄弟参数族）");
        sub.setTextSize(11);
        sub.setTextColor(Color.GRAY);
        root.addView(sub);

        etIp = new EditText(this);
        etIp.setHint("打印机 IP，例如 192.168.1.20");
        etIp.setInputType(InputType.TYPE_CLASS_TEXT);
        etIp.setSingleLine(true);
        root.addView(etIp);

        LinearLayout rowParam = new LinearLayout(this);
        rowParam.setOrientation(LinearLayout.HORIZONTAL);
        etDpi = new EditText(this);
        etDpi.setHint("DPI");
        etDpi.setText("300");
        etDpi.setInputType(InputType.TYPE_CLASS_NUMBER);
        etDpi.setEms(4);
        rowParam.addView(etDpi);
        cbColor = new CheckBox(this);
        cbColor.setText("彩色");
        rowParam.addView(cbColor);
        root.addView(rowParam);

        root.addView(btn("1. 探测（握手 + ESC D + ESC E）", new Runnable() {
            public void run() { probe(); }
        }));
        root.addView(btn("2. 扫描（ESC D → E → I → X → 图流）", new Runnable() {
            public void run() { scan(); }
        }));
        root.addView(btn("3. 打印测试页（系统打印框架）", new Runnable() {
            public void run() { printTest(); }
        }));
        root.addView(btn("4. 释放会话（发 ESC R，解 -NG 401）", new Runnable() {
            public void run() { resetSession(); }
        }));
        root.addView(btn("5. 连上后裸监听 15 秒（不发命令）", new Runnable() {
            public void run() { listenOnly(); }
        }));
        root.addView(btn("6. ESC Q 查询（打原始字节）", new Runnable() {
            public void run() { queryQ(); }
        }));
        root.addView(btn("7. 只发 ESC X 后裸监听 15 秒", new Runnable() {
            public void run() { xOnly(); }
        }));
        root.addView(btn("9. 直连打印 9100：纯 JPEG（无 PJL）", new Runnable() {
            public void run() { printRaw(0); }
        }));
        root.addView(btn("10. 直连打印 9100：PJL 最简包裹", new Runnable() {
            public void run() { printRaw(1); }
        }));
        root.addView(btn("11. 直连打印 9100：PJL + 兄弟参数族", new Runnable() {
            public void run() { printRaw(2); }
        }));
        root.addView(btn("12. 清空日志", new Runnable() {
            public void run() { tvLog.setText(""); }
        }));

        ScrollView scroll = new ScrollView(this);
        tvLog = new TextView(this);
        tvLog.setTextSize(10);
        tvLog.setTextColor(Color.DKGRAY);
        tvLog.setTextIsSelectable(true);
        scroll.addView(tvLog);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(lp);
        root.addView(scroll);

        ivPreview = new ImageView(this);
        ivPreview.setAdjustViewBounds(true);
        ivPreview.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(150)));
        root.addView(ivPreview);

        setContentView(root);
        log("就绪。填 IP → 先点 1（应回 ESC D=0x80 / ESC E=0x0），再点 2 扫描。");
    }

    private Button btn(String text, final Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { action.run(); }
        });
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private String ip() {
        String s = etIp.getText().toString().trim();
        if (s.isEmpty()) {
            log("错误：请先填写打印机 IP");
            return null;
        }
        return s;
    }

    private int dpiValue() {
        try {
            return Integer.parseInt(etDpi.getText().toString().trim());
        } catch (Exception e) {
            return 200;
        }
    }

    private void log(final String msg) {
        final String t = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        ui.post(new Runnable() {
            public void run() { tvLog.append("[" + t + "] " + msg + "\n"); }
        });
    }

    private void probe() {
        final String host = ip();
        if (host == null) return;
        final int dpi = dpiValue();
        final String mode = cbColor.isChecked() ? "C256" : "CGRAY";
        bg(new Runnable() {
            public void run() {
                log(">>> 探测开始，超时 6s/步");
                BrotherScanSdk.Result r = new BrotherScanSdk().probe(host, dpi, mode, 6000);
                log(String.valueOf(r.trace));
                log("结果：" + (r.ok ? r.message : "失败 — " + r.message));
            }
        });
    }

    private void scan() {
        final String host = ip();
        if (host == null) return;
        final int dpi = dpiValue();
        final String mode = cbColor.isChecked() ? "C256" : "CGRAY";
        bg(new Runnable() {
            public void run() {
                File dir = new File(getExternalFilesDir(null), "scans");
                log(">>> 扫描开始 dpi=" + dpi + " mode=" + mode);
                final BrotherScanSdk.Result r =
                        new BrotherScanSdk().scan(host, dpi, mode, 50, 50, 30000, dir);
                log(String.valueOf(r.trace));
                log("结果：" + r.message + "，设备回报 " + r.xdpi + "x" + r.ydpi
                        + " dpi，像素 " + r.width + "x" + r.height + "，页数 " + r.pages);
                if (r.path != null) log("首图：" + r.path);
                final byte[] jpg = r.jpeg;
                if (jpg != null) {
                    final android.graphics.Bitmap bmp = BitmapFactory.decodeByteArray(jpg, 0, jpg.length);
                    ui.post(new Runnable() {
                        public void run() {
                            if (bmp != null) {
                                ivPreview.setImageBitmap(bmp);
                                log("预览已加载");
                            } else {
                                log("JPEG 解码失败");
                            }
                        }
                    });
                }
            }
        });
    }

    private void resetSession() {
        final String host = ip();
        if (host == null) return;
        bg(new Runnable() {
            public void run() {
                BrotherScanSdk.Result r = new BrotherScanSdk().resetSession(host, 6000);
                log(String.valueOf(r.trace));
                log("结果：" + r.message);
            }
        });
    }

    private void listenOnly() {
        final String host = ip();
        if (host == null) return;
        bg(new Runnable() {
            public void run() {
                BrotherScanSdk.Result r = new BrotherScanSdk().listenOnly(host, 15, 6000);
                log(String.valueOf(r.trace));
                log("结果：" + r.message);
            }
        });
    }

    private void queryQ() {
        final String host = ip();
        if (host == null) return;
        bg(new Runnable() {
            public void run() {
                BrotherScanSdk.Result r = new BrotherScanSdk().queryQ(host, 6000);
                log(String.valueOf(r.trace));
                log("结果：" + r.message);
            }
        });
    }

    private void xOnly() {
        final String host = ip();
        if (host == null) return;
        final int dpi = dpiValue();
        bg(new Runnable() {
            public void run() {
                BrotherScanSdk.Result r = new BrotherScanSdk()
                        .xOnly(host, dpi, 2440, 3450, 15, 6000);
                log(String.valueOf(r.trace));
                log("结果：" + r.message);
            }
        });
    }

    /** 生成一张 300dpi A4 的测试页 JPEG（黑字白底） */
    private byte[] testJpeg() {
        int w = 2480, h = 3508;
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                w, h, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(bmp);
        c.drawColor(Color.WHITE);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setTextSize(72);
        String[] lines = {
                "Brother DCP-T428W 直连打印测试",
                "",
                "来源：Android Socket → " + "9100/TCP（绕过系统打印框架）",
                "时间：" + new Date(),
                "",
                "如果这张纸打出来了，说明不装兄弟插件",
                "也能直接让打印机出纸。",
                "",
                "扫描链路已实测可用（54921 / ESC X）"
        };
        int y = 320;
        for (String line : lines) {
            c.drawText(line, 180, y, paint);
            y += 120;
        }
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, bos);
        bmp.recycle();
        return bos.toByteArray();
    }

    private void printRaw(final int variant) {
        final String host = ip();
        if (host == null) return;
        bg(new Runnable() {
            public void run() {
                byte[] jpg = testJpeg();
                log("测试页 JPEG " + jpg.length + " 字节，variant=" + variant);
                PrintRaw.Result r = new PrintRaw().send(host, jpg, variant, 8000);
                log(String.valueOf(r.trace));
                log("结果：" + r.message);
            }
        });
    }

    private void printTest() {
        log("插件状态：" + (PrintHelper.isPluginInstalled(this)
                ? "已装 com.brother.mfc.brprint.plugin"
                : "未装 com.brother.mfc.brprint.plugin（面板里可能看不到打印机）"));
        byte[] pdf = PrintHelper.textPdf(
                "Brother DCP-T428W 测试页\n"
                        + "来源：Android 打印框架\n"
                        + "时间：" + new Date() + "\n\n"
                        + "若这张纸能打出来，说明打印链路（PDF → 兄弟插件 → 设备）通了。");
        PrintHelper.printOnMain(this, pdf, "BrotherTestPage");
        log("已提交系统打印任务，请在打印面板里选择 DCP-T428W");
    }

    private void bg(final Runnable r) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    r.run();
                } catch (Throwable t) {
                    log("后台异常：" + t);
                }
            }
        }).start();
    }
}

package com.papermarker.bridge;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.net.*;

public class MainActivity extends Activity {
    private static final int REQ_NOTIF = 1001;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        // v2.0.2 修复「界面被遮/显示不全」：旧版 LinearLayout 无 ScrollView，
        // 长说明文本把启动/停止按钮挤到屏幕外且无法滚动。
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        // px → dp 换算，适配不同密度屏幕（旧版硬编码 px 在高密度屏上过小/错位）
        float d = getResources().getDisplayMetrics().density;
        root.setPadding((int) (16 * d), (int) (20 * d), (int) (16 * d), (int) (20 * d));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("PaperMarker Android 桥接");
        title.setTextSize(22);
        title.setPadding(0, 0, 0, (int) (12 * d));
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("本应用在手机上运行 HTTP 桥接服务（localhost:3790），"
            + "将浏览器的打印/扫描请求转为 Brother 裸 TCP 协议，直连局域网打印机。\n\n"
            + "使用方法：\n"
            + "1. 手机与 Brother 打印机连同一 WiFi\n"
            + "2. 点击下方「启动桥接服务」\n"
            + "3. 在同手机浏览器打开 PaperMarker 网页\n"
            + "4. 网页自动探测 localhost:3790，经桥接连打印机\n\n"
            + "v2.0.2 更新：修复 Android 14/15 上启动桥接服务即闪退的问题"
            + "（补齐 FOREGROUND_SERVICE_DATA_SYNC 权限）；"
            + "支持 HTTPS 网页调用（PNA 预检）。\n\n"
            + "全程本地通信，不经云端。关闭本应用即停止桥接。");
        desc.setTextSize(13);
        desc.setLineSpacing(4, 1);
        root.addView(desc);

        Button startBtn = new Button(this);
        startBtn.setText("启动桥接服务");
        startBtn.setOnClickListener(v -> {
            // Android 13+ 请求通知权限（拒绝也不影响桥接功能，仅通知不可见）
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF);
            }
            try {
                Intent svc = new Intent(this, BridgeService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
                else startService(svc);
                Toast.makeText(this, "桥接服务已启动（localhost:3790）", Toast.LENGTH_LONG).show();
            } catch (Throwable e) {
                // Android 12+ 后台启动限制等极端场景：提示而不是闪退
                Toast.makeText(this, "启动失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        startParams.topMargin = (int) (16 * d);
        startBtn.setLayoutParams(startParams);
        root.addView(startBtn);

        Button stopBtn = new Button(this);
        stopBtn.setText("停止桥接服务");
        stopBtn.setOnClickListener(v -> {
            stopService(new Intent(this, BridgeService.class));
            Toast.makeText(this, "桥接服务已停止", Toast.LENGTH_SHORT).show();
        });
        root.addView(stopBtn);

        // 状态显示
        TextView status = new TextView(this);
        status.setTextSize(12);
        root.addView(status);

        Button checkBtn = new Button(this);
        checkBtn.setText("检查桥接状态");
        checkBtn.setOnClickListener(v -> {
            new Thread(() -> {
                boolean ok = checkPing();
                runOnUiThread(() -> {
                    status.setText(ok ? "✓ 桥接运行中（localhost:3790）" : "✗ 桥接未运行");
                });
            }).start();
        });
        root.addView(checkBtn);
    }

    boolean checkPing() {
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress("127.0.0.1", 3790), 2000);
            s.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

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
    private static final String PREFS = "pm_prefs";
    private static final String KEY_URL = "server_url";
    private TextView status;

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
        title.setText("PaperMarker Android");
        title.setTextSize(24);
        title.setPadding(0, 0, 0, (int) (4 * d));
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("桥接 + 全功能主应用（v2.0.3）");
        sub.setTextSize(13);
        sub.setTextColor(0xff047857);
        sub.setPadding(0, 0, 0, (int) (12 * d));
        root.addView(sub);

        // ---------- v2.0.3：全功能主界面（WebView 内嵌完整 Web 应用） ----------
        TextView webDesc = new TextView(this);
        webDesc.setText("「打开主应用」= 完整 PaperMarker 界面：扫描采集、拍照批改、"
            + "AI 批改复核、打印下发、打印机配置与能力查询、设置等全部功能，"
            + "与电脑浏览器完全一致。打开时自动启动桥接服务。\n\n"
            + "首次使用请先「设置服务器地址」填入你的 PaperMarker 网页地址"
            + "（教师账号登录的那个站点，含 https:// 前缀）。");
        webDesc.setTextSize(13);
        webDesc.setLineSpacing(4, 1);
        root.addView(webDesc);

        Button openWebBtn = new Button(this);
        openWebBtn.setText("打开主应用（全部功能）");
        openWebBtn.setTextSize(16);
        openWebBtn.setOnClickListener(v -> {
            String url = getUrl();
            if (url == null) {
                Toast.makeText(this, "请先「设置服务器地址」填入 PaperMarker 网页地址", Toast.LENGTH_LONG).show();
                promptUrl(true);
                return;
            }
            // 打开前顺带申请通知权限（前台服务通知）
            askNotifPermission();
            Intent it = new Intent(this, WebActivity.class);
            it.putExtra("url", url);
            startActivity(it);
        });
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        openParams.topMargin = (int) (14 * d);
        openWebBtn.setLayoutParams(openParams);
        root.addView(openWebBtn);

        Button urlBtn = new Button(this);
        urlBtn.setText("设置服务器地址");
        urlBtn.setOnClickListener(v -> promptUrl(false));
        root.addView(urlBtn);

        TextView urlNow = new TextView(this);
        urlNow.setTextSize(12);
        urlNow.setPadding(0, (int) (2 * d), 0, (int) (10 * d));
        root.addView(urlNow);
        status = urlNow;

        // ---------- 桥接服务控制 ----------
        TextView secTitle = new TextView(this);
        secTitle.setText("本地桥接服务（打印机协议）");
        secTitle.setTextSize(15);
        secTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        secTitle.setPadding(0, (int) (14 * d), 0, (int) (4 * d));
        root.addView(secTitle);

        TextView desc = new TextView(this);
        desc.setText("桥接 = 手机上运行 HTTP 服务（localhost:3790），把网页的打印/扫描请求"
            + "转为 Brother 裸 TCP 协议（打印 9100 / 扫描 54921）直连局域网打印机。\n"
            + "「打开主应用」时会自动启动；也可手动启停。\n"
            + "v2.0.3：修复高 DPI/彩色扫描中断（Read timed out）；"
            + "支持自动双面（探测打印机能力）与 PJL INFO CONFIG 协议查询。");
        desc.setTextSize(13);
        desc.setLineSpacing(4, 1);
        root.addView(desc);

        Button startBtn = new Button(this);
        startBtn.setText("启动桥接服务");
        startBtn.setOnClickListener(v -> {
            askNotifPermission();
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
        startParams.topMargin = (int) (12 * d);
        startBtn.setLayoutParams(startParams);
        root.addView(startBtn);

        Button stopBtn = new Button(this);
        stopBtn.setText("停止桥接服务");
        stopBtn.setOnClickListener(v -> {
            stopService(new Intent(this, BridgeService.class));
            Toast.makeText(this, "桥接服务已停止", Toast.LENGTH_SHORT).show();
        });
        root.addView(stopBtn);

        Button checkBtn = new Button(this);
        checkBtn.setText("检查桥接状态");
        checkBtn.setOnClickListener(v -> {
            new Thread(() -> {
                boolean ok = checkPing();
                runOnUiThread(() -> {
                    status.setText(ok
                        ? "✓ 桥接运行中（localhost:3790）"
                        : "✗ 桥接未运行");
                });
            }).start();
        });
        root.addView(checkBtn);
    }

    private String getUrl() {
        String u = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_URL, "");
        if (u == null || u.trim().isEmpty()) return null;
        return u.trim();
    }

    /** 弹窗输入服务器地址（保存后可选直接打开主应用） */
    private void promptUrl(final boolean openAfter) {
        final EditText input = new EditText(this);
        input.setHint("https://你的-PaperMarker-站点地址");
        String cur = getUrl();
        if (cur != null) input.setText(cur);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(this)
            .setTitle("服务器地址")
            .setMessage("PaperMarker 网页地址（教师登录的那个站点）。\n"
                + "手机需能访问该地址（同一局域网或公网部署均可）。")
            .setView(input)
            .setPositiveButton("保存", (dlg, w) -> {
                String v = input.getText().toString().trim();
                if (v.isEmpty()) {
                    Toast.makeText(this, "地址为空，未保存", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!v.startsWith("http://") && !v.startsWith("https://")) v = "https://" + v;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_URL, v).apply();
                Toast.makeText(this, "已保存：" + v, Toast.LENGTH_SHORT).show();
                if (status != null) status.setText("服务器：" + v);
                if (openAfter) {
                    Intent it = new Intent(this, WebActivity.class);
                    it.putExtra("url", v);
                    startActivity(it);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /** Android 13+ 通知权限（拒绝也不影响桥接运行） */
    private void askNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String url = getUrl();
        if (status != null && url != null) status.setText("服务器：" + url);
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

package com.papermarker.bridge;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * PaperMarker 主界面（v2.0.3）：全功能 WebView 壳。
 *
 * 用户反馈「安卓端功能太少，把 Web 全部功能复刻过来」——
 * 本 Activity 内嵌完整 PaperMarker Web 应用（登录/扫描/拍照批改/
 * 复核/打印/设置等全部功能），同时确保本地桥接服务在线，
 * 网页内的 localhost:3790 桥接调用直接可达（同进程设备）：
 *
 *   扫描采集 → 托盘 → AI 批改 → 复核调整 → 打印下发 → 设置
 *   全部可用，与浏览器端 100% 同功能。
 *
 * 关键适配：
 *  1. 混合内容放行：HTTPS 站点页面内 fetch http://localhost:3790
 *     （桥接）不被拦截（MIXED_CONTENT_ALWAYS_ALLOW）。
 *  2. 文件上传（拍照/相册）：WebChromeClient.onShowFileChooser →
 *     系统选择器/相机 Intent， onActivityResult 回传。
 *  3. getUserMedia 相机流（拍照矫正页）：onPermissionRequest →
 *     校验 CAMERA 运行时权限后 grant。
 *  4. PDF 下载（答案/解析等）：DownloadListener → 系统下载管理器。
 *  5. 返回键 = 网页后退，无历史才退出。
 *
 * 构建链约束（d8 8.2.2-dev + javac21-target8）：
 *  - 匿名内部类会触发 d8 NPE → 全部用命名类；
 *  - 内部类里的 lambda 也会触发 → 内部类内用命名 Runnable；
 *  - 顶层方法里的 lambda 无碍（v2.0.2 已验证）。
 */
public class WebActivity extends Activity {
    private static final int REQ_FILE = 2001;
    private static final int REQ_CAM = 2002;

    private WebView web;
    private ProgressBar bar;
    private ValueCallback<Uri[]> fileCb;

    /** Chrome 回调（命名静态内部类：进度/文件选择/相机权限） */
    private static class PmChrome extends WebChromeClient {
        private final WebActivity a;

        PmChrome(WebActivity a) {
            this.a = a;
        }

        @Override
        public void onProgressChanged(WebView v, int p) {
            a.bar.setProgress(p);
            a.bar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
        }

        /** 文件上传（拍照/选图上传批改）：系统选择器/相机 */
        @Override
        public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
            if (a.fileCb != null) a.fileCb.onReceiveValue(null);
            a.fileCb = cb;
            try {
                Intent it = params.createIntent();
                it.addCategory(Intent.CATEGORY_OPENABLE);
                a.startActivityForResult(Intent.createChooser(it, "选择图片或拍照"), REQ_FILE);
            } catch (Exception e) {
                a.fileCb = null;
                Toast.makeText(a, "无法打开文件选择器：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }

        /** getUserMedia（相机实时取景）：权限校验后放行 */
        @Override
        public void onPermissionRequest(final PermissionRequest req) {
            String[] res = req.getResources();
            boolean cam = false;
            for (String r : res) {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)
                        || PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) {
                    cam = true;
                }
            }
            a.runOnUiThread(new PermRun(a, req, res, cam));
        }
    }

    /** 相机权限校验 Runnable（命名类：内部类里不能用 lambda） */
    private static class PermRun implements Runnable {
        private final WebActivity a;
        private final PermissionRequest req;
        private final String[] res;
        private final boolean needCam;

        PermRun(WebActivity a, PermissionRequest req, String[] res, boolean needCam) {
            this.a = a;
            this.req = req;
            this.res = res;
            this.needCam = needCam;
        }

        @Override
        public void run() {
            if (needCam && Build.VERSION.SDK_INT >= 23
                    && a.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                req.deny();
                Toast.makeText(a, "相机权限未授予：请回主界面重新进入并允许，或改用「上传照片」",
                    Toast.LENGTH_LONG).show();
                return;
            }
            req.grant(res);
        }
    }

    /** PDF/文件下载（答案汇总/解析汇总等）：系统下载管理器 */
    private static class PmDownloader implements android.webkit.DownloadListener {
        private final WebActivity a;

        PmDownloader(WebActivity a) {
            this.a = a;
        }

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                    String mimeType, long size) {
            try {
                DownloadManager.Request rq = new DownloadManager.Request(Uri.parse(url));
                String name = a.extractName(url, contentDisposition);
                rq.setMimeType(mimeType == null ? "application/octet-stream" : mimeType);
                rq.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                rq.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                DownloadManager dm = (DownloadManager) a.getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) dm.enqueue(rq);
                Toast.makeText(a, "已开始下载：" + name, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(a, "下载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 恢复 WebView 状态（旋转/进程回收后不丢登录态与页面）
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 8));
        bar.setMax(100);
        bar.setProgress(0);
        root.addView(bar);

        web = new WebView(this);
        web.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(web);
        setContentView(root);

        setupWebView();

        // 相机运行时权限（getUserMedia 拍照矫正需要；拒绝时文件上传路径仍可用）
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM);
        }

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            String url = getUrl();
            if (url == null) {
                Toast.makeText(this, "未配置服务器地址，请回主界面设置", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            web.loadUrl(url);
        }
    }

    private String getUrl() {
        String u = getIntent().getStringExtra("url");
        if (u == null || u.trim().isEmpty()) {
            u = getSharedPreferences("pm_prefs", Context.MODE_PRIVATE).getString("server_url", "");
        }
        if (u == null || u.trim().isEmpty()) return null;
        u = u.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://" + u;
        return u;
    }

    private void setupWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // localStorage：登录令牌/桥接地址/图层配置
        s.setDatabaseEnabled(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // https 页面 → http://localhost:3790
        s.setTextZoom(100);                    // 不继承系统字体缩放，避免布局错位
        s.setSupportZoom(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new PmChrome(this));
        web.setDownloadListener(new PmDownloader(this));
    }

    /** 从 URL/ContentDisposition 提取下载文件名 */
    String extractName(String url, String disposition) {
        try {
            if (disposition != null && disposition.contains("filename=")) {
                int i = disposition.indexOf("filename=") + 9;
                String n = disposition.substring(i).replace("\"", "").trim();
                if (n.endsWith(";")) n = n.substring(0, n.length() - 1);
                if (!n.isEmpty()) return n;
            }
            String path = Uri.parse(url).getLastPathSegment();
            if (path != null && path.contains(".")) return path;
        } catch (Exception ignore) {}
        return "papermarker-" + System.currentTimeMillis() + ".pdf";
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 确保桥接在线：网页所有打印/扫描都经 localhost:3790
        try {
            Intent svc = new Intent(this, BridgeService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);
        } catch (Throwable ignore) {}
        web.onResume();
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE && fileCb != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            } else if (resultCode == RESULT_OK && data != null && data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                results = new Uri[n];
                for (int i = 0; i < n; i++) results[i] = data.getClipData().getItemAt(i).getUri();
            }
            fileCb.onReceiveValue(results);
            fileCb = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        if (fileCb != null) {
            fileCb.onReceiveValue(null);
            fileCb = null;
        }
        web.destroy();
        super.onDestroy();
    }
}

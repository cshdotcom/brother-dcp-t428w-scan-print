package com.papermarker.app;

import android.app.Application;
import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 全局崩溃记录：任何未捕获异常写入 cache/last_crash.txt（保留最近 1 次），
 * 登录页若检测到该文件会提示用户反馈错误内容，避免「只会闪退不知原因」。
 */
public class PmApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String text = "time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                            .format(new Date())
                            + "\nthread: " + t.getName() + "\n\n" + sw.toString();
                    File dir = getCacheDir();
                    if (dir != null && (dir.exists() || dir.mkdirs())) {
                        FileOutputStream fos = new FileOutputStream(new File(dir, "last_crash.txt"));
                        fos.write(text.getBytes("UTF-8"));
                        fos.close();
                    }
                } catch (Throwable ignore) {
                }
                if (prev != null) prev.uncaughtException(t, e);
            }
        });
    }

    /** 读取最近一次崩溃日志（供登录页展示给用户） */
    public static String lastCrash(Context c) {
        try {
            File f = new File(c.getCacheDir(), "last_crash.txt");
            if (!f.exists()) return "";
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f));
            StringBuilder b = new StringBuilder();
            String line;
            int n = 0;
            while ((line = r.readLine()) != null && n < 40) {
                b.append(line).append('\n');
                n++;
            }
            r.close();
            return b.toString();
        } catch (Throwable e) {
            return "";
        }
    }
}

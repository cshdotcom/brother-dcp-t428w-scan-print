package com.doubao.printlab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;

import java.io.ByteArrayOutputStream;

/** 打印：把 PDF 交给系统打印框架，PDL 由兄弟打印服务插件生成 */
public class PrintHelper {

    public static boolean isPluginInstalled(Context ctx) {
        try {
            ctx.getPackageManager().getPackageInfo("com.brother.mfc.brprint.plugin", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static byte[] textPdf(String text) {
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = doc.startPage(info);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(13f);
        float y = 56f;
        for (String line : text.split("\n")) {
            canvas.drawText(line, 40f, y, paint);
            y += 20f;
            if (y > 790f) break;
        }
        doc.finishPage(page);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try { doc.writeTo(out); } catch (Exception e) { throw new RuntimeException(e); }
        doc.close();
        return out.toByteArray();
    }

    public static byte[] imagePdf(byte[] jpeg) {
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = doc.startPage(info);
        Canvas canvas = page.getCanvas();
        Bitmap bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        if (bmp != null) {
            float scale = Math.min(515f / bmp.getWidth(), 762f / bmp.getHeight());
            float w = bmp.getWidth() * scale;
            float h = bmp.getHeight() * scale;
            canvas.drawBitmap(bmp, null, new android.graphics.RectF(40f, 40f, 40f + w, 40f + h), new Paint());
        }
        doc.finishPage(page);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try { doc.writeTo(out); } catch (Exception e) { throw new RuntimeException(e); }
        doc.close();
        return out.toByteArray();
    }

    public static void print(Context ctx, byte[] pdf, String jobName) {
        PrintManager pm = (PrintManager) ctx.getSystemService(Context.PRINT_SERVICE);
        pm.print(jobName, new Adapter(pdf, jobName), new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build());
    }

    private static class Adapter extends PrintDocumentAdapter {
        private final byte[] pdf;
        private final String name;

        Adapter(byte[] pdf, String name) {
            this.pdf = pdf;
            this.name = name;
        }

        @Override
        public void onLayout(PrintAttributes oldAttrs, PrintAttributes newAttrs,
                             CancellationSignal signal, LayoutResultCallback callback, Bundle extras) {
            if (signal != null && signal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }
            PrintDocumentInfo info = new PrintDocumentInfo.Builder(name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build();
            callback.onLayoutFinished(info, true);
        }

        @Override
        public void onWrite(PageRange[] pages, ParcelFileDescriptor dest,
                            CancellationSignal signal, WriteResultCallback callback) {
            try {
                java.io.OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(dest);
                os.write(pdf);
                os.close();
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            } catch (Exception e) {
                callback.onWriteFailed(e.getMessage());
            }
        }
    }

    /** 主线程工具：包一层 */
    public static void printOnMain(Context ctx, final byte[] pdf, final String name) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                print(ctx, pdf, name);
            }
        });
    }
}

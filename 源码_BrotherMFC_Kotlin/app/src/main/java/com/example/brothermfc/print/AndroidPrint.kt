package com.example.brothermfc.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.ByteArrayOutputStream

/**
 * 打印通道三（推荐、实测可用）：Android 打印框架 + 兄弟官方打印服务插件。
 *
 * 原理：DCP-T428W 的 PDL 由兄弟自己的打印服务插件
 * （com.brother.mfc.brprint.plugin，即 iPrint&Scan 同一套 SDK 的打印插件）负责光栅化与封装，
 * 我方只需交给系统一个“可打印的 PDF”。这样既不用猜 PWG/JPEG 封装，也不依赖 AirPrint。
 *
 * 前置条件（真机一次性）：
 *   1. 手机装「Brother Print Service Plugin」（应用商店搜 Brother 打印服务插件）
 *   2. 手机与打印机同一局域网，打印机已配网
 *   3. 插件内能看到 DCP-T428W
 *
 * 若插件未安装，系统打印面板里不会出现该打印机，此时退回 [PrintClient.printVia9100] 实验通道。
 */
object AndroidPrint {

    /** 是否已安装兄弟打印服务插件 */
    fun isBrotherPluginInstalled(ctx: Context): Boolean =
        try {
            ctx.packageManager.getPackageInfo("com.brother.mfc.brprint.plugin", 0); true
        } catch (_: Exception) {
            false
        }

    /** 用纯文本生成一页 A4 PDF */
    fun textPdf(text: String, title: String = "BrotherMFC"): ByteArray {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()) // A4 @72dpi
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { color = Color.BLACK; textSize = 12f }
        var y = 48f
        for (line in text.split("\n")) {
            canvas.drawText(line, 40f, y, paint); y += 18f
            if (y > 800f) break
        }
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    /** 把一张 Bitmap 缩放铺进 A4 PDF */
    fun imagePdf(bmp: Bitmap): ByteArray {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val scale = minOf(515f / bmp.width, 762f / bmp.height)
        val w = bmp.width * scale
        val h = bmp.height * scale
        canvas.drawBitmap(bmp, null, android.graphics.RectF(40f, 40f, 40f + w, 40f + h), Paint())
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    /** 拉起系统打印面板（用户在里面选 DCP-T428W 即可） */
    fun printPdf(ctx: Context, pdf: ByteArray, jobName: String) {
        val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
        pm.print(jobName, ByteArrayAdapter(pdf, jobName), PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build())
    }

    /** 把内存里的 PDF 交给打印框架 */
    private class ByteArrayAdapter(
        private val pdf: ByteArray,
        private val jobName: String
    ) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?, callback: LayoutResultCallback?, extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) { callback?.onLayoutCancelled(); return }
            val info = PrintDocumentInfo.Builder(jobName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback?.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?, destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?, callback: WriteResultCallback?
        ) {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(destination).use { it.write(pdf) }
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback?.onWriteFailed(e.message)
            }
        }
    }
}

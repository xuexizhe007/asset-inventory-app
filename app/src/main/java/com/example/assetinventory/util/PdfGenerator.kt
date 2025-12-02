package com.example.assetinventory.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.assetinventory.model.Asset
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream

object PdfGenerator {

    /**
     * 生成资产标签 PDF：
     * - 每页对应 1 个资产
     * - 尺寸 7cm x 4cm
     * - 左侧 3/4 为文字，右侧 1/4 为二维码（内容为资产编码）
     */
    fun generateLabelsPdf(context: Context, taskName: String?, assets: List<Asset>) {
        if (assets.isEmpty()) {
            Toast.makeText(context, "请选择需要打印的资产", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = android.graphics.pdf.PdfDocument()

        // 7cm x 4cm -> point（1 inch = 2.54cm, 1 inch = 72pt）
        val widthPoints = (7f / 2.54f * 72f).toInt()
        val heightPoints = (4f / 2.54f * 72f).toInt()

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        val title = taskName ?: "资产标签"

        assets.forEachIndexed { index, asset ->
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                widthPoints,
                heightPoints,
                index + 1
            ).create()

            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val padding = 8f
            val textAreaWidth = widthPoints * 0.75f
            val qrAreaWidth = widthPoints - textAreaWidth

            val qrSize = (qrAreaWidth - padding * 2).toInt()

            var y = padding + 10f

            fun drawTextLine(label: String, value: String) {
                canvas.drawText("$label：$value", padding, y, textPaint)
                y += 14f
            }

            // 第一、二行：资产编码、资产名称
            drawTextLine("资产编码", asset.code)
            drawTextLine("资产名称", asset.name)

            // 第三到第六行：使用人、使用部门、存放地点、投用日期
            drawTextLine("使用人", asset.user.orEmpty())
            drawTextLine("使用部门", asset.department.orEmpty())
            drawTextLine("存放地点", asset.location.orEmpty())
            drawTextLine("投用日期", asset.startDate)

            // 右侧二维码
            try {
                val qrBitmap = createQrBitmap(asset.code, qrSize)
                val left = textAreaWidth + padding
                val top = padding
                canvas.drawBitmap(qrBitmap, null, RectF(left, top, left + qrSize, top + qrSize), null)
            } catch (e: Exception) {
                // ignore QR failure, still output text
            }

            pdfDocument.finishPage(page)
        }

        // 写入到 Downloads
        val fileName = "${title}_${System.currentTimeMillis()}.pdf"
        val resolver = context.contentResolver
        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        val pdfBytes = outputStream.toByteArray()

        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AssetLabels")
                }
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                }
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            }

            if (uri == null) {
                Toast.makeText(context, "保存 PDF 失败", Toast.LENGTH_SHORT).show()
                return
            }

            resolver.openOutputStream(uri)?.use { os ->
                os.write(pdfBytes)
                os.flush()
            }

            Toast.makeText(context, "PDF 已保存到下载目录，可复制到电脑使用斑马打印机打印。", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "保存 PDF 时出错：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createQrBitmap(content: String, size: Int): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565)
    }
}

package com.example.assetinventory.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    /**
     * 生成资产标签 PDF 并通过分享功能发给其他 APP：
     * - 每个资产 1 页
     * - 尺寸：7cm x 4cm
     * - 左侧 3/4 为文字区域，右侧 1/4 为二维码
     * - 二维码顶部与“使用人”文字顶部对齐
     * - 资产名称、使用部门、存放地点支持换行（在左侧 3/4 区域内）
     */
    fun generateLabelsPdf(context: Context, taskName: String?, assets: List<Asset>) {
        if (assets.isEmpty()) {
            Toast.makeText(context, "请选择需要打印的资产", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()

        // 7cm x 4cm -> PDF point 单位
        val widthPoints = (7f / 2.54f * 72f).toInt()
        val heightPoints = (4f / 2.54f * 72f).toInt()

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        val title = taskName ?: "资产标签"

        assets.forEachIndexed { index, asset ->
            val pageInfo = PdfDocument.PageInfo.Builder(
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
            var userBaseline: Float? = null

            val maxTextWidth = textAreaWidth - padding * 2

            fun drawSingleLine(label: String, value: String) {
                val text = "$label：$value"
                canvas.drawText(text, padding, y, textPaint)
                y += 14f
            }

            fun drawWrapped(label: String, value: String) {
                // 首行包含 label
                val labelPrefix = "$label："
                val full = labelPrefix + value
                val chars = full.toCharArray()
                var start = 0
                while (start < chars.size) {
                    var i = start
                    var line = ""
                    while (i < chars.size) {
                        val candidate = (line + chars[i])
                        val w = textPaint.measureText(candidate)
                        if (w > maxTextWidth && line.isNotEmpty()) {
                            break
                        }
                        line = candidate
                        i++
                    }
                    canvas.drawText(line, padding, y, textPaint)
                    y += 14f
                    start += line.length
                }
            }

            // 第一行：资产编码（单行）
            drawSingleLine("资产编码", asset.code)

            // 第二行：资产名称（允许换行）
            drawWrapped("资产名称", asset.name)

            // 使用人：一般较短，用单行即可
            val labelUser = "使用人"
            val userText = asset.user.orEmpty()
            val textUser = "$labelUser：$userText"
            canvas.drawText(textUser, padding, y, textPaint)
            userBaseline = y
            y += 14f

            // 使用部门：允许换行
            drawWrapped("使用部门", asset.department.orEmpty())

            // 存放地点：允许换行
            drawWrapped("存放地点", asset.location.orEmpty())

            // 投用日期：单行
            drawSingleLine("投用日期", asset.startDate)

            // 右侧二维码区域，顶部与“使用人”对齐
            try {
                val qrBitmap = createQrBitmap(asset.code, qrSize)
                val left = textAreaWidth + padding
                val top = (userBaseline ?: (padding + 10f)) - textPaint.textSize
                val rect = RectF(left, top, left + qrSize, top + qrSize)
                canvas.drawBitmap(qrBitmap, null, rect, null)
            } catch (e: Exception) {
                // ignore QR failure
            }

            pdfDocument.finishPage(page)
        }

        // 写入临时文件并分享
        try {
            val fileName = "${title}_${System.currentTimeMillis()}.pdf"
            val cacheDir = File(context.cacheDir, "share_pdf")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, fileName)

            FileOutputStream(file).use { os ->
                pdfDocument.writeTo(os)
            }
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享资产标签 PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "生成或分享 PDF 时出错：${e.message}", Toast.LENGTH_LONG).show()
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

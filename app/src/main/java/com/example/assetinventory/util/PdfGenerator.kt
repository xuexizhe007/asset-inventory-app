package com.example.assetinventory.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.assetinventory.model.Asset
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object PdfGenerator {

    /**
     * 生成标签 PDF（每页一张标签）
     *
     * 需求：
     * 1) 标签尺寸：7cm x 4cm
     * 2) 左侧文本（资产名称、编码、使用人、部门、存放地点、状态）
     * 3) 右侧二维码
     * 4) 字体自动缩放，避免溢出
     * 5) 二维码：白边更小 + 码体更大（真正增大二维码绘制尺寸）
     * 6) 若资产类别是低值易耗品，则资产名称后追加“(低值易耗品)”
     */
    fun generateLabelsPdf(context: Context, taskName: String?, assets: List<Asset>) {
        if (assets.isEmpty()) {
            Toast.makeText(context, "请选择需要打印的资产", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()

        // 7cm x 4cm -> PDF point
        val widthPoints = (7f / 2.54f * 72f).toInt()
        val heightPoints = (4f / 2.54f * 72f).toInt()

        val fileName = buildString {
            append("labels")
            if (!taskName.isNullOrBlank()) append("_").append(taskName)
            append(".pdf")
        }
        val file = File(context.cacheDir, fileName)

        assets.forEachIndexed { index, asset ->
            val pageInfo = PdfDocument.PageInfo.Builder(widthPoints, heightPoints, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val padding = 8f
            val lineHeight = 14f

            // 保持原布局：左 0.75 文本区，右 0.25 二维码区
            val textAreaWidth = widthPoints * 0.75f
            val qrAreaWidth = widthPoints - textAreaWidth

            val maxTextWidth = textAreaWidth - padding * 2
            val maxFullWidth = widthPoints - padding * 2

            var y = padding + 10f
            var userBaseline: Float? = null

            // 背景白
            canvas.drawColor(Color.WHITE)

            // 标题（资产名称）
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }

            // 标签与值
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.DEFAULT
            }

            // ========== 标题自动缩放 ==========
            val title = buildString {
                append(asset.name ?: "")
                if (asset.category?.contains("低值易耗", ignoreCase = true) == true) {
                    append("(低值易耗品)")
                }
            }.ifBlank { "未命名资产" }

            val titleMaxSize = 14f
            val titleMinSize = 8f

            var tSize = titleMaxSize
            titlePaint.textSize = tSize
            while (titlePaint.measureText(title) > maxTextWidth && tSize > titleMinSize) {
                tSize -= 0.5f
                titlePaint.textSize = tSize
            }
            canvas.drawText(title, padding, y, titlePaint)
            y += lineHeight + 2f

            // 画分隔线
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }
            canvas.drawLine(padding, y, textAreaWidth - padding, y, linePaint)
            y += lineHeight - 4f

            // 通用函数：绘制 label + value，并自动缩放 value 字体避免溢出
            fun drawLabelValue(labelText: String, valueRaw: String?, recordUserBaseline: Boolean = false) {
                val value = valueRaw?.ifBlank { "-" } ?: "-"

                val labelW = labelPaint.measureText(labelText)
                val originalSize = valuePaint.textSize
                val minValueSize = 7f

                var size = originalSize
                valuePaint.textSize = size

                fun totalWidth(): Float {
                    return labelW + valuePaint.measureText(value)
                }

                while (totalWidth() > maxTextWidth && size > minValueSize) {
                    size -= 0.5f
                    valuePaint.textSize = size
                }

                canvas.drawText(labelText, padding, y, labelPaint)
                canvas.drawText(value, padding + labelW, y, valuePaint)

                if (recordUserBaseline) userBaseline = y

                y += lineHeight
                valuePaint.textSize = originalSize
            }

            // ========= 资产编码（单行） =========
            // 编码用稍大一点字体，自动缩放到不超过整行
            run {
                val codeLabel = "编码："
                val code = asset.code ?: "-"
                val codeLabelW = labelPaint.measureText(codeLabel)

                val codeValuePaint = Paint(valuePaint).apply {
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                }

                val minSize = 8f
                var size = codeValuePaint.textSize
                while ((codeLabelW + codeValuePaint.measureText(code)) > maxTextWidth && size > minSize) {
                    size -= 0.5f
                    codeValuePaint.textSize = size
                }

                canvas.drawText(codeLabel, padding, y, labelPaint)
                canvas.drawText(code, padding + codeLabelW, y, codeValuePaint)
                y += lineHeight
            }

            // ========= 使用人 / 部门 / 存放地点 / 状态 =========
            drawLabelValue("使用人：", asset.user, recordUserBaseline = true)
            drawLabelValue("部门：", asset.department)
            drawLabelValue("地点：", asset.location)
            drawLabelValue("状态：", asset.status)

            // ========= 备注（如果有的话，按需缩放） =========
            run {
                val remark = asset.remark?.trim().orEmpty()
                if (remark.isNotBlank()) {
                    val label = "备注："
                    val labelW = labelPaint.measureText(label)
                    val originalSize = valuePaint.textSize
                    val minSize = 7f
                    var size = originalSize

                    valuePaint.textSize = size
                    while ((labelW + valuePaint.measureText(remark)) > maxTextWidth && size > minSize) {
                        size -= 0.5f
                        valuePaint.textSize = size
                    }

                    canvas.drawText(label, padding, y, labelPaint)
                    canvas.drawText(remark, padding + labelW, y, valuePaint)
                    y += lineHeight
                    valuePaint.textSize = originalSize
                }
            }

            // ========= 右侧二维码 =========
            try {
                // 二维码区 padding 更小：让二维码矩形尺寸更大
                val qrPadding = 2f

                // 某些标签打印机右侧有不可打印边距，导致二维码右侧被裁切。
                // 这里将二维码整体向左平移约 3.5mm（10pt），以保证实际打印完整。
                val qrShiftLeft = 10f
                val left = max(padding, textAreaWidth + qrPadding - qrShiftLeft)

                // 顶部与“使用人”对齐
                val top = (userBaseline ?: (padding + 10f)) - labelPaint.textSize

                val maxByWidth = (qrAreaWidth - 2f * qrPadding).toInt()
                val maxByHeight = (heightPoints - top - qrPadding).toInt()
                val qrSize = max(1, min(maxByWidth, maxByHeight))

                val qrBitmap = createQrBitmap(
                    content = asset.code,
                    size = qrSize,
                    marginModules = 0
                )

                val rect = RectF(left, top, left + qrSize, top + qrSize)

                val safeRect = RectF(
                    rect.left,
                    rect.top,
                    min(rect.right, widthPoints.toFloat()),
                    min(rect.bottom, heightPoints.toFloat())
                )

                canvas.drawBitmap(qrBitmap, null, safeRect, null)
            } catch (_: Exception) {
                // ignore
            }

            pdfDocument.finishPage(page)
        }

        try {
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
            context.startActivity(Intent.createChooser(intent, "导出标签PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createQrBitmap(content: String?, size: Int, marginModules: Int = 0): Bitmap {
        val value = content?.ifBlank { " " } ?: " "
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.MARGIN to marginModules
        )
        val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}

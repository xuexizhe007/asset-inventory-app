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
     * 最终要求：
     * 1) 使用人在使用部门上面
     * 2) 资产名称独占一行，允许占据整行(按全页宽度)，不换行，超长缩小 value 字体（label 不缩小）
     * 3) 使用人 value 不换行，超长缩小 value 字体（label 不缩小）
     * 4) 使用部门单行：不换行，超长缩小 value 字体（label 不缩小）
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

        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        val title = taskName ?: "资产标签"

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

            fun drawWrapped(label: String, value: String) {
                val labelPrefix = "$label："
                val full = labelPrefix + value
                val chars = full.toCharArray()
                var start = 0
                while (start < chars.size) {
                    var i = start
                    var line = ""
                    while (i < chars.size) {
                        val candidate = (line + chars[i])
                        val w = labelPaint.measureText(candidate)
                        if (w > maxTextWidth && line.isNotEmpty()) break
                        line = candidate
                        i++
                    }
                    canvas.drawText(line, padding, y, labelPaint)
                    y += lineHeight
                    start += line.length
                }
            }

            /**
             * 单行绘制：label 固定字号，value 可缩小字号，不允许换行
             * maxWidth 指整行允许占用的最大宽度（从 x=padding 开始）
             */
            fun drawSingleLineShrinkValue(label: String, value: String, maxWidth: Float, minValueSize: Float = 7.5f) {
                val labelText = "$label："
                val labelW = labelPaint.measureText(labelText)

                val originalSize = valuePaint.textSize
                var size = originalSize

                fun totalWidth(): Float {
                    valuePaint.textSize = size
                    return labelW + valuePaint.measureText(value)
                }

                while (totalWidth() > maxWidth && size > minValueSize) {
                    size -= 0.5f
                }
                valuePaint.textSize = size

                canvas.drawText(labelText, padding, y, labelPaint)
                canvas.drawText(value, padding + labelW, y, valuePaint)

                y += lineHeight
                valuePaint.textSize = originalSize
            }

            // ========= 资产编码（单行） =========
            drawSingleLineShrinkValue("资产编码", asset.code, maxFullWidth)

            // ========= 资产名称（独占一行，占全页宽度，不换行，超长缩小 value） =========
            val isLowValue = asset.category?.trim() == "低值易耗品"
            val nameToPrint = asset.name + if (isLowValue) "(低值易耗品)" else ""
            drawSingleLineShrinkValue("资产名称", nameToPrint, maxFullWidth)

            // ========= 使用人（在使用部门上面，单行，不换行，超长缩小 value） =========
            val beforeUserY = y
            drawSingleLineShrinkValue("使用人", asset.user.orEmpty(), maxTextWidth)
            userBaseline = beforeUserY

            // ========= 使用部门（单行，不换行，超长缩小 value） =========
            drawSingleLineShrinkValue("使用部门", asset.department.orEmpty(), maxTextWidth)

            // ========= 存放地点（允许换行） =========
            drawWrapped("存放地点", asset.location.orEmpty())

            // ========= 投用日期（单行） =========
            drawSingleLineShrinkValue("投用日期", asset.startDate, maxTextWidth)

            // ========= 二维码：白边更小 + 码体更大 =========
            try {
                val qrPadding = 2f // 比 padding 更小：让二维码矩形尺寸更大

                // --- 关键修改：二维码整体向左平移，避免右侧不可打印边距导致裁切 ---
                val qrShiftLeft = 10f // 10pt ≈ 3.5mm，可按打印机情况调大/调小
                val left = max(padding, textAreaWidth + qrPadding - qrShiftLeft)
                // -----------------------------------------------------------

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

    /**
     * marginModules：二维码白边模块数（quiet zone）。
     */
    private fun createQrBitmap(content: String, size: Int, marginModules: Int): Bitmap {
        val writer = QRCodeWriter()
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.MARGIN, marginModules)
        }
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

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

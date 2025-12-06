package com.example.assetinventory.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.assetinventory.model.Asset
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object PdfGenerator {

    /**
     * 生成资产标签 PDF 并通过分享功能发给其他 APP：
     * - 每个资产 1 页
     * - 尺寸：7cm x 4cm（这里用固定像素模拟打印比例，和你原工程保持一致风格）
     * - 左侧 3/4 为文字区域，右侧 1/4 为二维码
     * - 二维码更大（新需求）
     * - “资产名称 + 使用部门”同一行，放不下则缩小 value 字体（仅缩小值，不缩小 label）
     * - 若资产类别为“低值易耗品”，资产名称后追加“(低值易耗品)”
     */
    fun generateLabelsPdf(context: Context, taskName: String?, assets: List<Asset>) {
        if (assets.isEmpty()) {
            Toast.makeText(context, "请选择需要打印的资产", Toast.LENGTH_SHORT).show()
            return
        }

        // 页面尺寸：约 7cm x 4cm（像素）
        // 这类标签一般由打印机驱动缩放，这里给一个清晰的画布尺寸即可。
        val pageWidth = 700
        val pageHeight = 400

        val padding = 18f
        val qrAreaRatio = 0.26f // 右侧二维码区域占比
        val qrAreaWidth = pageWidth * qrAreaRatio
        val textAreaWidth = pageWidth - qrAreaWidth

        // Paint：label 固定字号；value 可缩放字号
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val thinLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }

        val pdfDocument = PdfDocument()

        // PDF 标题（用于文件名）
        val title = (taskName?.trim().takeUnless { it.isNullOrEmpty() } ?: "资产标签")

        for ((index, asset) in assets.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            // 分割线：文字区域 / 二维码区域
            val splitX = textAreaWidth
            canvas.drawLine(splitX, 0f, splitX, pageHeight.toFloat(), thinLinePaint)

            // ========== 左侧文字 ==========
            var y = padding + 30f
            val maxTextWidth = textAreaWidth - padding * 2
            val gapBetweenBlocks = 22f

            // 需求1：低值易耗品名称追加
            val isLowValue = asset.category?.trim() == "低值易耗品"
            val nameValue = if (isLowValue) "${asset.name}(低值易耗品)" else asset.name

            val deptValue = asset.department ?: ""
            val userValue = asset.user ?: ""
            val locValue = asset.location ?: ""
            val codeValue = asset.code
            val startDateValue = asset.startDate

            // 需求3：“资产名称 + 使用部门”同一行，放不下就缩小 value 字体（label 不缩小）
            y = drawNameAndDeptOneLineAdaptive(
                canvas = canvas,
                x = padding,
                y = y,
                maxWidth = maxTextWidth,
                labelPaint = labelPaint,
                valuePaint = valuePaint,
                nameValue = nameValue,
                deptValue = deptValue
            )

            y += gapBetweenBlocks

            // 其它字段正常一行；值为空时也保持格式
            y = drawLabelValueLine(canvas, padding, y, labelPaint, valuePaint, "资产编码", codeValue)
            y += gapBetweenBlocks
            val userLineTopY = y - labelPaint.textSize // 记录“使用人”行的大致顶部，用于二维码对齐
            y = drawLabelValueLine(canvas, padding, y, labelPaint, valuePaint, "使用人", userValue)
            y += gapBetweenBlocks
            y = drawLabelValueLine(canvas, padding, y, labelPaint, valuePaint, "存放地点", locValue)
            y += gapBetweenBlocks
            y = drawLabelValueLine(canvas, padding, y, labelPaint, valuePaint, "投用日期", startDateValue)

            // ========== 右侧二维码（更大） ==========
            try {
                val qrContent = asset.code.ifBlank { asset.name }
                val qrSize = 220 // 需求2：二维码边长更大（可按领导感觉继续加到 240/260）
                val qrBitmap = createQrBitmap(qrContent, qrSize)

                // 二维码放在右侧区域，尽量大，且顶部与“使用人”行对齐（原需求）
                val qrLeft = splitX + (qrAreaWidth - qrSize) / 2f
                val qrTop = max(padding, userLineTopY) // 与“使用人”行顶部对齐
                val rect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)

                canvas.drawBitmap(qrBitmap, null, rect, null)
            } catch (_: Exception) {
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

            context.startActivity(Intent.createChooser(intent, "分享 PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "生成PDF失败：${e.message}", Toast.LENGTH_LONG).show()
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * 画一行：label（固定） + value（正常）
     */
    private fun drawLabelValueLine(
        canvas: Canvas,
        x: Float,
        y: Float,
        labelPaint: Paint,
        valuePaint: Paint,
        label: String,
        value: String
    ): Float {
        var curY = y
        val labelText = "$label："
        canvas.drawText(labelText, x, curY, labelPaint)
        val labelW = labelPaint.measureText(labelText)
        canvas.drawText(value, x + labelW, curY, valuePaint)
        return curY
    }

    /**
     * 需求3：资产名称 + 使用部门在同一行，如果放不下，则缩小 value 字体（只缩小值，不缩小 label）
     */
    private fun drawNameAndDeptOneLineAdaptive(
        canvas: Canvas,
        x: Float,
        y: Float,
        maxWidth: Float,
        labelPaint: Paint,
        valuePaint: Paint,
        nameValue: String,
        deptValue: String
    ): Float {
        val label1 = "资产名称："
        val label2 = "使用部门："
        val gap = "    " // 两个字段之间的间隔

        val label1W = labelPaint.measureText(label1)
        val label2W = labelPaint.measureText(label2)
        val gapW = valuePaint.measureText(gap)

        val originalValueSize = valuePaint.textSize
        val minValueSize = 18f

        var testSize = originalValueSize

        fun totalWidth(): Float {
            valuePaint.textSize = testSize
            val v1 = valuePaint.measureText(nameValue)
            val v2 = valuePaint.measureText(deptValue)
            return label1W + v1 + gapW + label2W + v2
        }

        while (totalWidth() > maxWidth && testSize > minValueSize) {
            testSize -= 1.5f
        }
        valuePaint.textSize = testSize

        // 开始绘制
        canvas.drawText(label1, x, y, labelPaint)
        var cursorX = x + label1W
        canvas.drawText(nameValue, cursorX, y, valuePaint)

        cursorX += valuePaint.measureText(nameValue)
        canvas.drawText(gap, cursorX, y, valuePaint)
        cursorX += valuePaint.measureText(gap)

        canvas.drawText(label2, cursorX, y, labelPaint)
        cursorX += label2W
        canvas.drawText(deptValue, cursorX, y, valuePaint)

        // 恢复 value 字体（避免影响后续行）
        valuePaint.textSize = originalValueSize

        return y
    }

    private fun createQrBitmap(content: String, size: Int): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (yy in 0 until height) {
            val offset = yy * width
            for (xx in 0 until width) {
                pixels[offset + xx] = if (bitMatrix.get(xx, yy)) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565)
    }
}

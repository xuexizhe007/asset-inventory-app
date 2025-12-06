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
import kotlin.math.min

object PdfGenerator {

    /**
     * 生成资产标签 PDF 并通过分享功能发给其他 APP：
     * - 每个资产 1 页
     * - 尺寸：7cm x 4cm
     * - 左侧 3/4 为文字区域，右侧 1/4 为二维码
     * - 二维码顶部与“使用人”文字顶部对齐
     * - 存放地点支持换行（在左侧 3/4 区域内）
     *
     * 新需求：
     * 1) 若资产类别是低值易耗品，则在资产名称后追加“(低值易耗品)”
     * 2) 二维码更大一些
     * 3) 资产名称 和 使用部门 在同一行；如果放不下，则缩小字体（只缩小实际内容，不缩小“资产名称/使用部门”字样）
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

        // label（“资产名称/使用部门”等）固定字号
        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        // value（具体内容）允许缩小字号
        val valuePaint = Paint().apply {
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

            // 需求2：二维码更大（不改变布局，只减少右侧留白）
            // 原来常见写法：(qrAreaWidth - padding * 2)
            // 这里改成：(qrAreaWidth - padding) -> 更大但仍不越界
            val qrSize = max(1, (qrAreaWidth - padding).toInt())

            var y = padding + 10f
            var userBaseline: Float? = null

            val maxTextWidth = textAreaWidth - padding * 2

            fun drawSingleLine(label: String, value: String) {
                // 保持原来的“整行 drawText”写法不动，确保版式稳定
                val text = "$label：$value"
                canvas.drawText(text, padding, y, labelPaint)
                y += 14f
            }

            fun drawWrapped(label: String, value: String) {
                // 原逻辑保留：首行包含 label，并按 maxTextWidth 自动换行
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
                        if (w > maxTextWidth && line.isNotEmpty()) {
                            break
                        }
                        line = candidate
                        i++
                    }
                    canvas.drawText(line, padding, y, labelPaint)
                    y += 14f
                    start += line.length
                }
            }

            /**
             * 需求3：同一行绘制：资产名称：<value1>    使用部门：<value2>
             * 若超宽：仅缩小 valuePaint 的字号，labelPaint 不变。
             * 注意：只占用一行高度（y += 14f 一次），避免破坏原有布局节奏。
             */
            fun drawNameAndDeptOneLineAutoShrinkValue(nameValue: String, deptValue: String) {
                val label1 = "资产名称："
                val label2 = "使用部门："
                val gap = "    "

                val label1W = labelPaint.measureText(label1)
                val label2W = labelPaint.measureText(label2)

                val originalSize = valuePaint.textSize
                val minSize = 7.5f

                fun totalWidth(): Float {
                    val gapW = valuePaint.measureText(gap)
                    val v1W = valuePaint.measureText(nameValue)
                    val v2W = valuePaint.measureText(deptValue)
                    return label1W + v1W + gapW + label2W + v2W
                }

                // 仅缩小 value 字号
                var size = originalSize
                valuePaint.textSize = size
                while (totalWidth() > maxTextWidth && size > minSize) {
                    size -= 0.5f
                    valuePaint.textSize = size
                }

                // 绘制：label 固定字号，value 可能变小
                canvas.drawText(label1, padding, y, labelPaint)
                var cursorX = padding + label1W
                canvas.drawText(nameValue, cursorX, y, valuePaint)
                cursorX += valuePaint.measureText(nameValue)

                canvas.drawText(gap, cursorX, y, valuePaint)
                cursorX += valuePaint.measureText(gap)

                canvas.drawText(label2, cursorX, y, labelPaint)
                cursorX += label2W
                canvas.drawText(deptValue, cursorX, y, valuePaint)

                // 只占一行高度，保持原版式
                y += 14f

                // 恢复字号，避免影响后续行
                valuePaint.textSize = originalSize
            }

            // 第一行：资产编码（单行）
            drawSingleLine("资产编码", asset.code)

            // 需求1：低值易耗品的资产名称追加后缀（仅影响打印显示）
            val isLowValue = asset.category?.trim() == "低值易耗品"
            val nameToPrint = asset.name + if (isLowValue) "(低值易耗品)" else ""

            // ✅ 修复点：这里必须是 orEmpty()
            val deptToPrint = asset.department.orEmpty()

            // 需求3：第二行改为 “资产名称 + 使用部门”同一行
            drawNameAndDeptOneLineAutoShrinkValue(nameToPrint, deptToPrint)

            // 使用人：保持原逻辑（单行）+ 记录 baseline 用于二维码对齐
            val labelUser = "使用人"
            val userText = asset.user.orEmpty()
            val textUser = "$labelUser：$userText"
            canvas.drawText(textUser, padding, y, labelPaint)
            userBaseline = y
            y += 14f

            // 存放地点：允许换行（保持原逻辑）
            drawWrapped("存放地点", asset.location.orEmpty())

            // 投用日期：单行（保持原逻辑）
            drawSingleLine("投用日期", asset.startDate)

            // 右侧二维码区域，顶部与“使用人”对齐（保持原逻辑：仅二维码变大）
            try {
                val qrBitmap = createQrBitmap(asset.code, qrSize)
                val left = textAreaWidth + padding
                val top = (userBaseline ?: (padding + 10f)) - labelPaint.textSize
                val rect = RectF(left, top, left + qrSize, top + qrSize)

                // 防御：极端情况下做一次裁剪
                val safeRect = RectF(
                    rect.left,
                    rect.top,
                    min(rect.right, widthPoints.toFloat()),
                    min(rect.bottom, heightPoints.toFloat())
                )

                canvas.drawBitmap(qrBitmap, null, safeRect, null)
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

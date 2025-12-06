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
     * 生成资产标签 PDF 并通过分享功能发给其他 APP：
     * - 每个资产 1 页
     * - 尺寸：7cm x 4cm
     * - 左侧 3/4 为文字区域，右侧 1/4 为二维码
     * - 二维码顶部与“使用人”文字顶部对齐
     * - 存放地点支持换行（在左侧 3/4 区域内）
     *
     * 需求变更后的最终要求：
     * 1) 若资产类别是低值易耗品，则在资产名称后追加“(低值易耗品)”
     * 2) 二维码要更大：减少白色边框(quiet zone) + 在不裁剪前提下放大显示尺寸
     * 3) 资产名称与使用部门之间要换行（不在一行）
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

        val labelPaint = Paint().apply {
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
            val lineHeight = 14f

            val textAreaWidth = widthPoints * 0.75f
            val qrAreaWidth = widthPoints - textAreaWidth
            val maxTextWidth = textAreaWidth - padding * 2

            var y = padding + 10f
            var userBaseline: Float? = null

            fun drawSingleLine(label: String, value: String) {
                val text = "$label：$value"
                canvas.drawText(text, padding, y, labelPaint)
                y += lineHeight
            }

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

            // 第一行：资产编码
            drawSingleLine("资产编码", asset.code)

            // 需求1：低值易耗品的资产名称追加后缀（仅打印显示）
            val isLowValue = asset.category?.trim() == "低值易耗品"
            val nameToPrint = asset.name + if (isLowValue) "(低值易耗品)" else ""

            // 需求：资产名称与使用部门之间换行（恢复两行绘制）
            drawWrapped("资产名称", nameToPrint)
            drawWrapped("使用部门", asset.department.orEmpty())

            // 使用人：单行 + baseline 用于二维码顶部对齐
            val textUser = "使用人：" + asset.user.orEmpty()
            canvas.drawText(textUser, padding, y, labelPaint)
            userBaseline = y
            y += lineHeight

            // 存放地点：允许换行
            drawWrapped("存放地点", asset.location.orEmpty())

            // 投用日期：单行
            drawSingleLine("投用日期", asset.startDate)

            // ========== 需求2：二维码更大 ==========
            try {
                val left = textAreaWidth + padding

                // 顶部与“使用人”对齐（保持你原来的逻辑）
                val top = (userBaseline ?: (padding + 10f)) - labelPaint.textSize

                // 让二维码在右侧区域尽可能大，但不能被页面底部裁掉
                val maxByWidth = (qrAreaWidth - padding).toInt()          // 减少留白
                val maxByHeight = (heightPoints - top - padding).toInt()  // 防止底部裁剪
                val qrSize = max(1, min(maxByWidth, maxByHeight))

                // 核心：减少二维码白边(quiet zone)，让码体视觉变大
                val qrBitmap = createQrBitmap(
                    content = asset.code,
                    size = qrSize,
                    marginModules = 0 // 0 或 1：越小白边越少
                )

                val rect = RectF(left, top, left + qrSize, top + qrSize)

                // 防御性裁剪（一般不会触发）
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

    /**
     * 生成二维码 bitmap。
     * marginModules：二维码白边（quiet zone）大小，0/1 会显著增大“实际码体”。
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

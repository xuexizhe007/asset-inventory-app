package com.example.assetinventory.util

import android.content.Context
import com.example.assetinventory.model.Asset
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.ByteArrayOutputStream

object ExcelExporter {

    /**
     * 使用 assets 中的 export_template.xlsx 作为模板：
     * - 第 1、2 行保持不变
     * - 第 3 行为表头
     * - 从第 4 行开始按表头逐行写入资产信息
     */
    fun exportFromTemplate(context: Context, assets: List<Asset>): ByteArray {
        context.assets.open(TEMPLATE_FILE_NAME).use { input ->
            val wb = WorkbookFactory.create(input)
            val sheet = wb.getSheetAt(0)

            // 清理旧数据（如果模板里已经包含了历史数据）
            val startRowIdx = 3 // 第4行（0-based）
            val lastRow = sheet.lastRowNum
            if (lastRow >= startRowIdx) {
                for (i in lastRow downTo startRowIdx) {
                    val row = sheet.getRow(i)
                    if (row != null) sheet.removeRow(row)
                }
            }

            assets.forEachIndexed { index, asset ->
                val rowIndex = startRowIdx + index
                val row = sheet.createRow(rowIndex)

                // 表头(第3行)列顺序：资产编码, 资产名称, 资产类别, 使用部门, 使用人, 存放地点, 投用日期
                row.createCell(0).setCellValue(asset.code ?: "")
                row.createCell(1).setCellValue(asset.name ?: "")
                row.createCell(2).setCellValue(asset.category ?: "")
                row.createCell(3).setCellValue(asset.department ?: "")
                row.createCell(4).setCellValue(asset.user ?: "")
                row.createCell(5).setCellValue(asset.location ?: "")
                row.createCell(6).setCellValue(asset.startDate ?: "")
            }

            val out = ByteArrayOutputStream()
            wb.use { it.write(out) }
            return out.toByteArray()
        }
    }

    private const val TEMPLATE_FILE_NAME = "export_template.xlsx"
}

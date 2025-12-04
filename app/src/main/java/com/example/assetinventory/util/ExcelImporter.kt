package com.example.assetinventory.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelImporter {

    private const val HEADER_CODE = "资产编码"
    private const val HEADER_NAME = "资产名称"
    private const val HEADER_USER = "使用人"
    private const val HEADER_DEPT = "使用部门"
    private const val HEADER_LOCATION = "存放地点"
    
    private const val HEADER_CATEGORY = "资产类别"  // New header for asset category
    private const val HEADER_START_DATE = "投用日期"
    

    data class ImportResult(
        val taskName: String,
        val assets: List<Asset>
    )

    fun import(context: Context, uri: Uri): ImportResult {
        val fileName = queryFileName(context, uri) ?: "资产盘点任务"
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("无法打开文件")

        val workbook: Workbook = if (fileName.lowercase(Locale.getDefault()).endsWith(".xlsx")) {
            XSSFWorkbook(inputStream)
        } else {
            HSSFWorkbook(inputStream)
        }

        val sheet = workbook.getSheetAt(0)
        if (sheet.physicalNumberOfRows <= 1) {
            workbook.close()
            throw IllegalStateException("表格内容为空")
        }

        val headerRow = sheet.getRow(0) ?: throw IllegalStateException("缺少表头")

        val codeIndex = findColumnIndex(headerRow, HEADER_CODE)
        val nameIndex = findColumnIndex(headerRow, HEADER_NAME)
        val userIndex = findColumnIndex(headerRow, HEADER_USER)
        val deptIndex = findColumnIndex(headerRow, HEADER_DEPT)
        val locationIndex = findColumnIndex(headerRow, HEADER_LOCATION)
        val startDateIndex = findColumnIndex(headerRow, HEADER_START_DATE)

        val formatter = DataFormatter()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val list = mutableListOf<Asset>()

        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val code = getCellString(row.getCell(codeIndex), formatter)?.trim().orEmpty()
            val name = getCellString(row.getCell(nameIndex), formatter)?.trim().orEmpty()
            if (code.isEmpty() || name.isEmpty()) continue

            val user = getCellString(row.getCell(userIndex), formatter)?.trim()
            val dept = getCellString(row.getCell(deptIndex), formatter)?.trim()
            val location = getCellString(row.getCell(locationIndex), formatter)?.trim()

            val startDateCell = row.getCell(startDateIndex)
            val startDate = if (startDateCell != null &&
                startDateCell.cellType == CellType.NUMERIC &&
                DateUtil.isCellDateFormatted(startDateCell)
            ) {
                sdf.format(startDateCell.dateCellValue)
            } else {
                getCellString(startDateCell, formatter)?.trim().orEmpty()
            }

            list.add(
                Asset(
                    code = code,
                    name = name,
                    user = user,
                    department = dept,
                    location = location,
                    startDate = startDate,
                    status = AssetStatus.UNCHECKED
                )
            )
        }

        workbook.close()
        return ImportResult(taskName = fileName.substringBeforeLast("."), assets = list)
    }

    private fun findColumnIndex(row: Row, headerName: String): Int {
        for (i in 0 until row.lastCellNum) {
            val cell = row.getCell(i) ?: continue
            val value = cell.stringCellValue?.trim()
            if (value == headerName) return i
        }
        throw IllegalStateException("表头中缺少字段：$headerName")
    }

    private fun getCellString(cell: Cell?, formatter: DataFormatter): String? {
        if (cell == null) return null
        return formatter.formatCellValue(cell)
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }
}

    // Add logic to handle category column
    val category = row.getCell(7)?.stringCellValue?.trim() ?: "" // Assuming it's in the 8th column (index 7)
    val asset = Asset(
        code = row.getCell(0)?.stringCellValue ?: "",
        name = row.getCell(1)?.stringCellValue ?: "",
        user = row.getCell(2)?.stringCellValue,
        department = row.getCell(3)?.stringCellValue,
        location = row.getCell(4)?.stringCellValue,
        startDate = row.getCell(5)?.stringCellValue ?: "",
        status = AssetStatus.valueOf(row.getCell(6)?.stringCellValue ?: "UNCHECKED"),
        category = category // Set the category
    )

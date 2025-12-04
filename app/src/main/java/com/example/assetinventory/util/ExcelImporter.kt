
val code = row.getCell(0)?.stringCellValue?.trim()
val name = row.getCell(1)?.stringCellValue?.trim()
val category = row.getCell(2)?.stringCellValue?.trim()  // 新增
val user = row.getCell(3)?.stringCellValue?.trim()
val dept = row.getCell(4)?.stringCellValue?.trim()
val location = row.getCell(5)?.stringCellValue?.trim()
val startDate = parseDate(row.getCell(6))  // 解析日期

val asset = Asset(
    code = code ?: "",
    name = name ?: "",
    user = user,
    department = dept,
    location = location,
    startDate = startDate ?: "",
    category = category,          // 新增
    status = AssetStatus.UNCHECKED,
    selectedForPrint = false,
    taskId = 0L
)


val values = ContentValues().apply {
    put("code", asset.code)
    put("name", asset.name)
    put("user", asset.user)
    put("department", asset.department)
    put("location", asset.location)
    put("start_date", asset.startDate)
    put("category", asset.category)   // 新增字段
    put("status", asset.status.ordinal)
    put("task_id", taskId)
}
db.insert("assets", null, values)

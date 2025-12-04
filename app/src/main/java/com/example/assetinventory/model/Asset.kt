
data class Asset(
    val code: String,
    val name: String,
    var user: String?,
    var department: String?,
    var location: String?,
    val startDate: String,
    var category: String? = null,  // 新增字段

    var status: AssetStatus = AssetStatus.UNCHECKED,
    var selectedForPrint: Boolean = false,
    var taskId: Long = 0L
)

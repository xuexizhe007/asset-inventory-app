package com.example.assetinventory.model

data class Asset(
    val code: String,
    val name: String,
    var user: String?,
    var department: String?,
    var location: String?,
    val startDate: String,
    var category: String? = null,
    var status: AssetStatus = AssetStatus.UNCHECKED,
    var selectedForPrint: Boolean = false,
    var taskId: Long = 0L
)

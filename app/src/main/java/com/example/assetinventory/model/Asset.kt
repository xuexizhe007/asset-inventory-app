package com.example.assetinventory.model

data class Asset(
    val code: String,
    val name: String,
    var user: String?,
    var department: String?,
    var location: String?,
    val startDate: String,
    
    
    var status: AssetStatus = AssetStatus.UNCHECKED,
    var category: String = ""  // New field for asset category
    var selectedForPrint: Boolean = false,
    var taskId: Long = 0L
)

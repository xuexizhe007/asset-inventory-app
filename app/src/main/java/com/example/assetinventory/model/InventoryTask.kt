package com.example.assetinventory.model

data class InventoryTask(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val assets: MutableList<Asset>
)

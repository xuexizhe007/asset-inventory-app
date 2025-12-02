package com.example.assetinventory.data

import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus

object AssetRepository {
    var taskName: String? = null
    val assets: MutableList<Asset> = mutableListOf()

    fun setTask(name: String, list: List<Asset>) {
        taskName = name
        assets.clear()
        assets.addAll(list)
    }

    fun findByCode(code: String): Asset? =
        assets.firstOrNull { it.code == code }

    fun clear() {
        taskName = null
        assets.clear()
    }

    fun statusSummary(): Map<AssetStatus, Int> =
        assets.groupingBy { it.status }.eachCount()
}

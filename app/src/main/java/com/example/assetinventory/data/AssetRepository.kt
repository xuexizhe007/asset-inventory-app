package com.example.assetinventory.data

import android.content.Context
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus
import com.example.assetinventory.model.InventoryTask
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AssetRepository {

    private const val PREF_NAME = "asset_inventory_pref"
    private const val KEY_DATA = "repository_data"

    private val gson = Gson()

    private var initialized = false

    val tasks: MutableList<InventoryTask> = mutableListOf()
    var currentTaskId: Long? = null
        private set

    private data class RepositoryData(
        val currentTaskId: Long?,
        val tasks: List<InventoryTask>
    )

    fun init(context: Context) {
        if (initialized) return
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sp.getString(KEY_DATA, null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<RepositoryData>() {}.type
                val data: RepositoryData = gson.fromJson(json, type)
                tasks.clear()
                tasks.addAll(data.tasks)
                currentTaskId = data.currentTaskId
            } catch (e: Exception) {
                tasks.clear()
                currentTaskId = null
            }
        }
        initialized = true
    }

    private fun persist(context: Context) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val data = RepositoryData(currentTaskId = currentTaskId, tasks = tasks)
        val json = gson.toJson(data)
        sp.edit().putString(KEY_DATA, json).apply()
    }

    fun createTask(context: Context, name: String, assets: List<Asset>): InventoryTask {
        init(context)
        val id = System.currentTimeMillis()
        val task = InventoryTask(
            id = id,
            name = name,
            createdAt = System.currentTimeMillis(),
            assets = assets.toMutableList()
        )
        tasks.add(0, task)
        currentTaskId = id
        persist(context)
        return task
    }

    fun deleteTask(context: Context, taskId: Long) {
        init(context)
        tasks.removeAll { it.id == taskId }
        if (currentTaskId == taskId) {
            currentTaskId = tasks.firstOrNull()?.id
        }
        persist(context)
    }

    fun setCurrentTask(context: Context, taskId: Long) {
        init(context)
        currentTaskId = taskId
        persist(context)
    }

    fun getCurrentTask(context: Context): InventoryTask? {
        init(context)
        val id = currentTaskId ?: return null
        return tasks.firstOrNull { it.id == id }
    }

    fun getTaskById(context: Context, taskId: Long): InventoryTask? {
        init(context)
        return tasks.firstOrNull { it.id == taskId }
    }

    fun getCurrentAssets(context: Context): MutableList<Asset> {
        return getCurrentTask(context)?.assets ?: mutableListOf()
    }

    fun findAssetInCurrentTask(context: Context, code: String): Asset? {
        return getCurrentAssets(context).firstOrNull { it.code == code }
    }

    fun onTaskChanged(context: Context) {
        persist(context)
    }

    fun statusSummary(context: Context): Map<AssetStatus, Int> =
        getCurrentAssets(context).groupingBy { it.status }.eachCount()
}

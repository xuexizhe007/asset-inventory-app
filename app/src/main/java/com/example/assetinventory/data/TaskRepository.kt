package com.example.assetinventory.data

import com.example.assetinventory.model.InventoryTask
import com.example.assetinventory.model.Asset

object TaskRepository {
    private val tasks = mutableListOf<InventoryTask>()
    private var nextId: Long = 1

    fun addTask(name: String, assets: List<Asset>): InventoryTask {
        val task = InventoryTask(
            id = nextId++,
            name = name,
            createdAt = System.currentTimeMillis(),
            assets = assets.toMutableList()
        )
        tasks.add(0, task) // newest first
        return task
    }

    fun getTasks(): List<InventoryTask> = tasks

    fun getTask(id: Long): InventoryTask? = tasks.firstOrNull { it.id == id }

    fun clear() {
        tasks.clear()
        nextId = 1
    }
}

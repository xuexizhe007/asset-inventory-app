package com.example.assetinventory.data

import android.content.ContentValues
import android.content.Context
import com.example.assetinventory.db.AssetDbHelper
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus

object LocalStore {

    private fun helper(context: Context) = AssetDbHelper(context.applicationContext)

    fun insertTaskWithAssets(context: Context, name: String, assets: List<Asset>): Long {
        val db = helper(context).writableDatabase
        db.beginTransaction()
        try {
            val taskValues = ContentValues().apply {
                put("name", name)
                put("file_name", name)
                put("created_at", System.currentTimeMillis())
            }
            val taskId = db.insertOrThrow("tasks", null, taskValues)

            assets.forEach { asset ->
                val values = ContentValues().apply {
                    put("task_id", taskId)
                    put("code", asset.code)
                    put("name", asset.name)
                    put("user", asset.user)
                    put("department", asset.department)
                    put("location", asset.location)
                    put("start_date", asset.startDate)
                    put("status", asset.status.name)
                }
                db.insert("assets", null, values)
            }

            db.setTransactionSuccessful()
            return taskId
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getTasks(context: Context): List<TaskInfo> {
        val db = helper(context).readableDatabase
        val list = mutableListOf<TaskInfo>()
        val sql = """SELECT t.id, t.name, t.created_at, COUNT(a.id) AS asset_count
                     FROM tasks t
                     LEFT JOIN assets a ON t.id = a.task_id
                     GROUP BY t.id
                     ORDER BY t.created_at DESC"""
        val cursor = db.rawQuery(sql, null)
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val name = it.getString(1) ?: ""
                val createdAt = it.getLong(2)
                val count = it.getInt(3)
                list.add(TaskInfo(id, name, count, createdAt))
            }
        }
        db.close()
        return list
    }

    fun getAssetsForTask(context: Context, taskId: Long): List<Asset> {
        val db = helper(context).readableDatabase
        val list = mutableListOf<Asset>()
        val cursor = db.query(
            "assets",
            arrayOf("code", "name", "user", "department", "location", "start_date", "status"),
            "task_id=?",
            arrayOf(taskId.toString()),
            null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                val code = it.getString(0)
                val name = it.getString(1)
                val user = it.getString(2)
                val dept = it.getString(3)
                val location = it.getString(4)
                val startDate = it.getString(5)
                val statusName = it.getString(6)
                val status = try {
                    AssetStatus.valueOf(statusName)
                } catch (e: Exception) {
                    AssetStatus.UNCHECKED
                }
                list.add(
                    Asset(
                        code = code,
                        name = name,
                        user = user,
                        department = dept,
                        location = location,
                        startDate = startDate,
                        status = status,
                        selectedForPrint = false,
                        taskId = taskId
                    )
                )
            }
        }
        db.close()
        return list
    }

    fun findAssetByCode(context: Context, taskId: Long, code: String): Asset? {
        val db = helper(context).readableDatabase
        val cursor = db.query(
            "assets",
            arrayOf("code", "name", "user", "department", "location", "start_date", "status"),
            "task_id=? AND code=?",
            arrayOf(taskId.toString(), code),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val name = it.getString(1)
                val user = it.getString(2)
                val dept = it.getString(3)
                val location = it.getString(4)
                val startDate = it.getString(5)
                val statusName = it.getString(6)
                val status = try {
                    AssetStatus.valueOf(statusName)
                } catch (e: Exception) {
                    AssetStatus.UNCHECKED
                }
                db.close()
                return Asset(
                    code = code,
                    name = name,
                    user = user,
                    department = dept,
                    location = location,
                    startDate = startDate,
                    status = status,
                    selectedForPrint = false,
                    taskId = taskId
                )
            }
        }
        db.close()
        return null
    }

    fun updateAssetStatus(context: Context, taskId: Long, code: String, status: AssetStatus) {
        val db = helper(context).writableDatabase
        val values = ContentValues().apply {
            put("status", status.name)
        }
        db.update(
            "assets",
            values,
            "task_id=? AND code=?",
            arrayOf(taskId.toString(), code)
        )
        db.close()
    }

    fun updateAssetDetails(
        context: Context,
        taskId: Long,
        code: String,
        user: String?,
        department: String?,
        location: String?,
        status: AssetStatus
    ) {
        val db = helper(context).writableDatabase
        val values = ContentValues().apply {
            put("user", user)
            put("department", department)
            put("location", location)
            put("status", status.name)
        }
        db.update(
            "assets",
            values,
            "task_id=? AND code=?",
            arrayOf(taskId.toString(), code)
        )
        db.close()
    }

    fun clearAll(context: Context) {
        val db = helper(context).writableDatabase
        db.execSQL("DELETE FROM assets")
        db.execSQL("DELETE FROM tasks")
        db.close()
    }
}

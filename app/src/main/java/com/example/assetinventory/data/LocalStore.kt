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
            // insert task
            val taskValues = ContentValues().apply {
                put("name", name)
                put("created_at", System.currentTimeMillis())
            }
            val taskId = db.insert("tasks", null, taskValues)

            // insert assets
            for (asset in assets) {
                val values = ContentValues().apply {
                    put("task_id", taskId)
                    put("code", asset.code)
                    put("name", asset.name)
                    put("category", asset.category)
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
                val name = it.getString(1)
                val createdAt = it.getLong(2)
                val count = it.getInt(3)
                list.add(TaskInfo(id = id, name = name, createdAt = createdAt, assetCount = count))
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
            arrayOf("code", "name", "category", "user", "department", "location", "start_date", "status"),
            "task_id=?",
            arrayOf(taskId.toString()),
            null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                val code = it.getString(0)
                val name = it.getString(1)
                val category = it.getString(2)
                val user = it.getString(3)
                val dept = it.getString(4)
                val location = it.getString(5)
                val startDate = it.getString(6)
                val statusName = it.getString(7)
                val status = try {
                    AssetStatus.valueOf(statusName)
                } catch (e: Exception) {
                    AssetStatus.UNCHECKED
                }

                list.add(
                    Asset(
                        code = code,
                        name = name,
                        category = category,
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

    /**
     * ✅ 兼容原工程：AssetDetailActivity / AssetEditActivity / QrScanActivity 依赖这个方法名
     */
    fun findAssetByCode(context: Context, taskId: Long, code: String): Asset? {
        val db = helper(context).readableDatabase
        val cursor = db.query(
            "assets",
            arrayOf("code", "name", "category", "user", "department", "location", "start_date", "status"),
            "task_id=? AND code=?",
            arrayOf(taskId.toString(), code),
            null, null, null,
            "1"
        )
        cursor.use {
            if (it.moveToFirst()) {
                val c = it.getString(0)
                val name = it.getString(1)
                val category = it.getString(2)
                val user = it.getString(3)
                val dept = it.getString(4)
                val location = it.getString(5)
                val startDate = it.getString(6)
                val statusName = it.getString(7)
                val status = try {
                    AssetStatus.valueOf(statusName)
                } catch (e: Exception) {
                    AssetStatus.UNCHECKED
                }
                db.close()
                return Asset(
                    code = c,
                    name = name,
                    category = category,
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

    /**
     * ✅ 兼容原工程：AssetEditActivity 调用 updateAssetDetails
     */
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

    /**
     * ✅ 新增：导出用（按任务取全量资产）
     * 注：这里直接复用 getAssetsForTask，避免重复逻辑
     */
    fun getAssetsByTask(context: Context, taskId: Long): List<Asset> {
        return getAssetsForTask(context, taskId)
    }

    /**
     * ✅ 新增：删除任务 + 级联删除任务资产（事务保证一致性）
     */
    fun deleteTaskCascade(context: Context, taskId: Long) {
        val db = helper(context).writableDatabase
        db.beginTransaction()
        try {
            db.delete("assets", "task_id=?", arrayOf(taskId.toString()))
            db.delete("tasks", "id=?", arrayOf(taskId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun clearAll(context: Context) {
        val db = helper(context).writableDatabase
        db.execSQL("DELETE FROM assets")
        db.execSQL("DELETE FROM tasks")
        db.close()
    }
}

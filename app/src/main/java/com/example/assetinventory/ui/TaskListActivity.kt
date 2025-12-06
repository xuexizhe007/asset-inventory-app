package com.example.assetinventory.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assetinventory.R
import com.example.assetinventory.data.LocalStore
import com.example.assetinventory.data.TaskInfo
import com.example.assetinventory.util.ExcelExporter
import com.example.assetinventory.util.ExcelImporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskListActivity : AppCompatActivity() {

    private lateinit var btnBackTaskList: Button
    private lateinit var btnBack: Button
    private lateinit var btnImportTask: Button
    private lateinit var rvTasks: RecyclerView
    private lateinit var adapter: TaskAdapter

    private var pendingExportFileName: String? = null
    private var pendingExportBytes: ByteArray? = null

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            importExcel(uri)
        }
    }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult

        val bytes = pendingExportBytes
        if (bytes == null) {
            Toast.makeText(this, "导出失败：没有可写入的数据", Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }

        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
                out.flush()
            }
            Toast.makeText(this, "导出成功", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            AlertDialog.Builder(this)
                .setTitle("导出失败")
                .setMessage(e.message ?: "未知错误")
                .setPositiveButton("确定", null)
                .show()
        } finally {
            pendingExportBytes = null
            pendingExportFileName = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        btnBackTaskList = findViewById(R.id.btnBackTaskList)
        btnBack = findViewById(R.id.btnBack)
        btnImportTask = findViewById(R.id.btnImportTask)
        rvTasks = findViewById(R.id.rvTasks)

        supportActionBar?.title = getString(R.string.task_list_title)

        btnBackTaskList.setOnClickListener { finish() }
        btnBack.setOnClickListener { finish() }
        btnImportTask.setOnClickListener { openExcelPicker() }

        adapter = TaskAdapter(
            items = emptyList(),
            onItemClick = { openTask(it) },
            onDeleteClick = { confirmDelete(it) },
            onExportClick = { exportTask(it) }
        )

        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = adapter

        loadTasks()
    }

    private fun openExcelPicker() {
        importLauncher.launch(
            arrayOf(
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        )
    }

    private fun importExcel(uri: Uri) {
        try {
            val result = ExcelImporter.import(this, uri)
            if (result.assets.isEmpty()) {
                Toast.makeText(this, "表格没有有效资产数据", Toast.LENGTH_LONG).show()
                return
            }
            LocalStore.insertTaskWithAssets(this, result.taskName, result.assets)
            Toast.makeText(this, "导入成功，生成任务：${result.taskName}", Toast.LENGTH_LONG).show()
            loadTasks()
        } catch (e: Exception) {
            e.printStackTrace()
            AlertDialog.Builder(this)
                .setTitle("导入失败")
                .setMessage(e.message ?: "未知错误")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    private fun loadTasks() {
        val tasks = LocalStore.getTasks(this)
        adapter.update(tasks)
    }

    private fun openTask(task: TaskInfo) {
        if (task.assetCount <= 0) {
            Toast.makeText(this, "该任务没有资产记录", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_TASK_ID, task.id)
        intent.putExtra(MainActivity.EXTRA_TASK_NAME, task.name)
        startActivity(intent)
    }

    private fun confirmDelete(task: TaskInfo) {
        AlertDialog.Builder(this)
            .setTitle("删除任务")
            .setMessage("确认删除任务“${task.name}”及其所有资产信息吗？此操作不可恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                try {
                    LocalStore.deleteTaskCascade(this, task.id)
                    Toast.makeText(this, "已删除：${task.name}", Toast.LENGTH_LONG).show()
                    loadTasks()
                } catch (e: Exception) {
                    e.printStackTrace()
                    AlertDialog.Builder(this)
                        .setTitle("删除失败")
                        .setMessage(e.message ?: "未知错误")
                        .setPositiveButton("确定", null)
                        .show()
                }
            }
            .show()
    }

    private fun exportTask(task: TaskInfo) {
        if (task.assetCount <= 0) {
            Toast.makeText(this, "该任务没有资产记录", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val assets = LocalStore.getAssetsByTask(this, task.id)
            if (assets.isEmpty()) {
                Toast.makeText(this, "该任务没有资产记录", Toast.LENGTH_SHORT).show()
                return
            }

            val bytes = ExcelExporter.exportFromTemplate(this, assets)

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val ts = sdf.format(Date())
            val safeName = task.name.replace(Regex("[\\\\/:*?\\\"<>|]"), "_")
            val filename = "${safeName}_${ts}.xlsx"

            pendingExportFileName = filename
            pendingExportBytes = bytes
            exportLauncher.launch(filename)
        } catch (e: Exception) {
            e.printStackTrace()
            AlertDialog.Builder(this)
                .setTitle("导出失败")
                .setMessage(e.message ?: "未知错误")
                .setPositiveButton("确定", null)
                .show()
        }
    }
}

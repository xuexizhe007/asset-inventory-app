package com.example.assetinventory.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskListActivity : AppCompatActivity() {

    private lateinit var btnImportTask: ExtendedFloatingActionButton
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
        val bytes = pendingExportBytes ?: return@registerForActivityResult
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
                out.flush()
            }
            Toast.makeText(this, "导出成功", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pendingExportBytes = null
            pendingExportFileName = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        // 设置 Toolbar 标题
        supportActionBar?.title = "资产盘点任务"

        btnImportTask = findViewById(R.id.btnImportTask)
        rvTasks = findViewById(R.id.rvTasks)

        // 移除旧的 Back 按钮监听，因为 XML 里设为 gone 了，逻辑也不需要了

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
            .setMessage("确认删除任务“${task.name}”？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                LocalStore.deleteTaskCascade(this, task.id)
                loadTasks()
            }
            .show()
    }

    private fun exportTask(task: TaskInfo) {
        // ... (保持原有导出逻辑)
        try {
            val assets = LocalStore.getAssetsByTask(this, task.id)
            if (assets.isEmpty()) return

            val bytes = ExcelExporter.exportFromTemplate(this, assets)
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val filename = "${task.name}_${sdf.format(Date())}.xlsx"

            pendingExportFileName = filename
            pendingExportBytes = bytes
            exportLauncher.launch(filename)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

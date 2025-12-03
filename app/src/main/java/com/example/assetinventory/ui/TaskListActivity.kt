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
import com.example.assetinventory.util.ExcelImporter

class TaskListActivity : AppCompatActivity() {

    private lateinit var btnBackTaskList: Button
    private lateinit var btnBack: Button
    private lateinit var btnImportTask: Button
    private lateinit var rvTasks: RecyclerView
    private lateinit var adapter: TaskAdapter

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        btnBackTaskList = findViewById(R.id.btnBackTaskList)
        btnBack = findViewById(R.id.btnBack)
        btnImportTask = findViewById(R.id.btnImportTask)
        rvTasks = findViewById(R.id.rvTasks)

        supportActionBar?.title = getString(R.string.task_list_title)

        adapter = TaskAdapter(emptyList()) { task ->
            openTask(task)
        }
        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = adapter

        btnBackTaskList.setOnClickListener {
            // 已经在任务列表，可提示一下
            Toast.makeText(this, "已在任务列表页面", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnImportTask.setOnClickListener {
            openExcelPicker()
        }

        loadTasks()
    }

    private fun openExcelPicker() {
        importLauncher.launch(arrayOf(
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ))
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
        val tasks: List<TaskInfo> = LocalStore.getTasks(this)
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
}

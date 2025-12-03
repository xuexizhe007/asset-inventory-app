package com.example.assetinventory.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assetinventory.R
import com.example.assetinventory.data.TaskRepository
import com.example.assetinventory.util.ExcelImporter

class TaskListActivity : AppCompatActivity() {

    private lateinit var btnImportTask: Button
    private lateinit var rvTasks: RecyclerView
    private lateinit var tvNoTask: TextView
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

        btnImportTask = findViewById(R.id.btnImportTask)
        rvTasks = findViewById(R.id.rvTasks)
        tvNoTask = findViewById(R.id.tvNoTask)

        adapter = TaskAdapter(TaskRepository.getTasks()) { task ->
            AssetListActivity.start(this, task.id)
        }
        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = adapter

        btnImportTask.setOnClickListener {
            importLauncher.launch(arrayOf(
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
        }

        refreshList()
    }

    private fun importExcel(uri: Uri) {
        try {
            val result = ExcelImporter.import(this, uri)
            val task = TaskRepository.addTask(result.taskName, result.assets)
            Toast.makeText(this, "导入成功：${task.name}，共 ${task.assets.size} 条资产", Toast.LENGTH_LONG).show()
            refreshList()
            AssetListActivity.start(this, task.id)
        } catch (e: Exception) {
            e.printStackTrace()
            android.app.AlertDialog.Builder(this)
                .setTitle("导入失败")
                .setMessage(e.message ?: "未知错误")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    private fun refreshList() {
        val tasks = TaskRepository.getTasks()
        adapter.update(tasks)
        tvNoTask.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
    }
}

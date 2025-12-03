package com.example.assetinventory.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assetinventory.R
import com.example.assetinventory.data.AssetRepository
import com.example.assetinventory.model.InventoryTask
import com.example.assetinventory.util.ExcelImporter

class TaskListActivity : AppCompatActivity() {

    private lateinit var btnImportTask: Button
    private lateinit var tvEmpty: TextView
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
        AssetRepository.init(this)
        setContentView(R.layout.activity_task_list)

        btnImportTask = findViewById(R.id.btnImportTask)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvTasks = findViewById(R.id.rvTasks)

        adapter = TaskAdapter(emptyList()) { task ->
            openTask(task)
        }
        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = adapter

        btnImportTask.setOnClickListener {
            openExcelPicker()
        }

        refreshTaskList()
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
            val task = AssetRepository.createTask(this, result.taskName, result.assets)
            Toast.makeText(this, "导入成功，共 ${result.assets.size} 条资产", Toast.LENGTH_LONG).show()
            refreshTaskList()
            openTask(task)
        } catch (e: Exception) {
            e.printStackTrace()
            AlertDialog.Builder(this)
                .setTitle("导入失败")
                .setMessage(e.message ?: "未知错误")
                .setPositiveButton(R.string.dialog_ok, null)
                .show()
        }
    }

    private fun openTask(task: InventoryTask) {
        AssetRepository.setCurrentTask(this, task.id)
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_TASK_ID, task.id)
        startActivity(intent)
    }

    private fun refreshTaskList() {
        val list = AssetRepository.tasks
        adapter.update(list)
        tvEmpty.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}

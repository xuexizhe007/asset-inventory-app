package com.example.assetinventory.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assetinventory.R
import com.example.assetinventory.data.TaskRepository
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus
import com.example.assetinventory.util.PdfGenerator

class AssetListActivity : AppCompatActivity() {

    private var taskId: Long = -1
    private lateinit var tvTaskName: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnFilterStatus: Button
    private lateinit var rvAssets: RecyclerView
    private lateinit var btnInventory: Button
    private lateinit var btnPrint: Button
    private lateinit var adapter: AssetAdapter

    private val selectedStatuses: MutableSet<AssetStatus> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_list)

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val task = TaskRepository.getTask(taskId)
        if (task == null) {
            Toast.makeText(this, "未找到任务", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.title = "资产列表"

        tvTaskName = findViewById(R.id.tvTaskName)
        etSearch = findViewById(R.id.etSearch)
        btnFilterStatus = findViewById(R.id.btnFilterStatus)
        rvAssets = findViewById(R.id.rvAssets)
        btnInventory = findViewById(R.id.btnInventory)
        btnPrint = findViewById(R.id.btnPrint)

        tvTaskName.text = "${task.name}（共 ${task.assets.size} 条资产）"

        adapter = AssetAdapter(task.assets) { asset ->
            AssetDetailActivity.start(this, taskId, asset.code)
        }
        rvAssets.layoutManager = LinearLayoutManager(this)
        rvAssets.adapter = adapter

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                applyFilter()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnFilterStatus.setOnClickListener {
            showStatusFilterDialog()
        }

        btnInventory.setOnClickListener {
            checkCameraPermissionAndStartScan()
        }

        btnPrint.setOnClickListener {
            val currentTask = TaskRepository.getTask(taskId)
            if (currentTask == null) {
                Toast.makeText(this, "任务不存在", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selected = currentTask.assets.filter { it.selectedForPrint }
            PdfGenerator.generateLabelsPdf(this, currentTask.name, selected)
        }

        applyFilter()
    }

    private fun applyFilter() {
        val task = TaskRepository.getTask(taskId) ?: return
        val keyword = etSearch.text.toString().trim()
        val hasStatusFilter = selectedStatuses.isNotEmpty()

        val filtered = task.assets.filter { asset ->
            val matchKeyword = if (keyword.isEmpty()) {
                true
            } else {
                asset.code.contains(keyword, true) ||
                        asset.name.contains(keyword, true) ||
                        (asset.location ?: "").contains(keyword, true)
            }
            val matchStatus = if (!hasStatusFilter) {
                true
            } else {
                selectedStatuses.contains(asset.status)
            }
            matchKeyword && matchStatus
        }
        adapter.update(filtered)
    }

    private fun showStatusFilterDialog() {
        val all = AssetStatus.values()
        val labels = all.map { it.displayName }.toTypedArray()
        val checked = all.map { selectedStatuses.contains(it) }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("选择盘点状态（可多选）")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val status = all[which]
                if (isChecked) selectedStatuses.add(status) else selectedStatuses.remove(status)
            }
            .setPositiveButton("确定") { _, _ -> applyFilter() }
            .setNegativeButton("清空") { _, _ ->
                selectedStatuses.clear()
                applyFilter()
            }
            .show()
    }

    private fun checkCameraPermissionAndStartScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startQrScan()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQ_CAMERA
            )
        }
    }

    private fun startQrScan() {
        val intent = Intent(this, QrScanActivity::class.java)
        intent.putExtra(QrScanActivity.EXTRA_TASK_ID, taskId)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQrScan()
            } else {
                Toast.makeText(this, "需要相机权限才能进行扫码盘点", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_asset_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_back_tasks -> {
                val intent = Intent(this, TaskListActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val REQ_CAMERA = 100
        private const val EXTRA_TASK_ID = "extra_task_id"

        fun start(context: android.content.Context, taskId: Long) {
            val intent = Intent(context, AssetListActivity::class.java)
            intent.putExtra(EXTRA_TASK_ID, taskId)
            context.startActivity(intent)
        }
    }
}

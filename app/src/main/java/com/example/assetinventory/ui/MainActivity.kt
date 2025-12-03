package com.example.assetinventory.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.example.assetinventory.data.AssetRepository
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus
import com.example.assetinventory.util.PdfGenerator

class MainActivity : AppCompatActivity() {

    private lateinit var tvTaskName: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnFilterStatus: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnInventory: Button
    private lateinit var btnPrint: Button
    private lateinit var btnBackToTasks: Button

    private lateinit var adapter: AssetAdapter

    private val selectedStatuses: MutableSet<AssetStatus> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AssetRepository.init(this)
        setContentView(R.layout.activity_main)

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) {
            Toast.makeText(this, "未找到任务", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val task = AssetRepository.getTaskById(this, taskId)
        if (task == null) {
            Toast.makeText(this, "未找到任务", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        AssetRepository.setCurrentTask(this, taskId)

        tvTaskName = findViewById(R.id.tvTaskName)
        etSearch = findViewById(R.id.etSearch)
        btnFilterStatus = findViewById(R.id.btnFilterStatus)
        recyclerView = findViewById(R.id.rvAssets)
        btnInventory = findViewById(R.id.btnInventory)
        btnPrint = findViewById(R.id.btnPrint)
        btnBackToTasks = findViewById(R.id.btnBackToTasks)

        adapter = AssetAdapter(emptyList(), { asset ->
            val intentDetail = Intent(this, AssetDetailActivity::class.java)
            intentDetail.putExtra(AssetDetailActivity.EXTRA_ASSET_CODE, asset.code)
            startActivity(intentDetail)
        }, { _ ->
            AssetRepository.onTaskChanged(this)
        })
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnBackToTasks.setOnClickListener {
            finish()
        }

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
            if (AssetRepository.getCurrentAssets(this).isEmpty()) {
                Toast.makeText(this, "当前任务暂无资产", Toast.LENGTH_SHORT).show()
            } else {
                checkCameraPermissionAndStartScan()
            }
        }

        btnPrint.setOnClickListener {
            val taskCurrent = AssetRepository.getCurrentTask(this)
            if (taskCurrent == null) {
                Toast.makeText(this, "当前任务不存在", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selected = taskCurrent.assets.filter { it.selectedForPrint }
            PdfGenerator.generateLabelsPdf(this, taskCurrent.name, selected)
        }

        refreshTaskInfo()
        applyFilter()
    }

    override fun onResume() {
        super.onResume()
        refreshTaskInfo()
        applyFilter() // 确保从详情/编辑返回后状态刷新
    }

    private fun refreshTaskInfo() {
        val task = AssetRepository.getCurrentTask(this)
        tvTaskName.text = if (task == null) {
            getString(R.string.no_current_task)
        } else {
            getString(R.string.current_task, task.name, task.assets.size)
        }
    }

    private fun applyFilter() {
        val keyword = etSearch.text.toString().trim()
        val hasStatusFilter = selectedStatuses.isNotEmpty()
        val assets = AssetRepository.getCurrentAssets(this)

        val filtered = assets.filter { asset ->
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
        val allStatuses = AssetStatus.values()
        val labels = allStatuses.map { it.displayName }.toTypedArray()
        val checked = allStatuses.map { selectedStatuses.contains(it) }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("选择盘点状态（可多选）")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val status = allStatuses[which]
                if (isChecked) {
                    selectedStatuses.add(status)
                } else {
                    selectedStatuses.remove(status)
                }
            }
            .setPositiveButton("确定") { _, _ ->
                applyFilter()
            }
            .setNegativeButton("清空") { _, _ ->
                selectedStatuses.clear()
                applyFilter()
            }
            .show()
    }

    private fun checkCameraPermissionAndStartScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startQrScan()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    private fun startQrScan() {
        val intent = Intent(this, QrScanActivity::class.java)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQrScan()
            } else {
                Toast.makeText(this, "需要相机权限才能进行扫码盘点", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_task -> {
                val task = AssetRepository.getCurrentTask(this)
                if (task == null) {
                    Toast.makeText(this, "当前无任务", Toast.LENGTH_SHORT).show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("删除任务")
                        .setMessage("确定要删除任务「${task.name}」及其全部资产吗？")
                        .setPositiveButton("删除") { _, _ ->
                            AssetRepository.deleteTask(this, task.id)
                            Toast.makeText(this, "已删除任务", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}

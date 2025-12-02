package com.example.assetinventory.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.assetinventory.util.ExcelImporter
import com.example.assetinventory.util.PdfGenerator

class MainActivity : AppCompatActivity() {

    private lateinit var tvTaskName: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnFilterStatus: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnInventory: Button
    private lateinit var btnPrint: Button

    private lateinit var adapter: AssetAdapter

    private val selectedStatuses: MutableSet<AssetStatus> = mutableSetOf()

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
        setContentView(R.layout.activity_main)

        tvTaskName = findViewById(R.id.tvTaskName)
        etSearch = findViewById(R.id.etSearch)
        btnFilterStatus = findViewById(R.id.btnFilterStatus)
        recyclerView = findViewById(R.id.rvAssets)
        btnInventory = findViewById(R.id.btnInventory)
        btnPrint = findViewById(R.id.btnPrint)
        val btnImportTask: Button = findViewById(R.id.btnImportTask)

        adapter = AssetAdapter(emptyList()) { asset ->
            val intent = Intent(this, AssetDetailActivity::class.java)
            intent.putExtra(AssetDetailActivity.EXTRA_ASSET_CODE, asset.code)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnImportTask.setOnClickListener {
            openExcelPicker()
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
            if (AssetRepository.assets.isEmpty()) {
                Toast.makeText(this, "请先导入盘点任务", Toast.LENGTH_SHORT).show()
            } else {
                checkCameraPermissionAndStartScan()
            }
        }

        btnPrint.setOnClickListener {
            if (AssetRepository.assets.isEmpty()) {
                Toast.makeText(this, "请先导入盘点任务", Toast.LENGTH_SHORT).show()
            } else {
                val selected = AssetRepository.assets.filter { it.selectedForPrint }
                PdfGenerator.generateLabelsPdf(this, AssetRepository.taskName, selected)
            }
        }

        refreshTaskInfo()
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
            AssetRepository.setTask(result.taskName, result.assets)
            Toast.makeText(this, "导入成功，共 ${result.assets.size} 条资产", Toast.LENGTH_LONG).show()
            refreshTaskInfo()
            applyFilter()
        } catch (e: Exception) {
            e.printStackTrace()
            AlertDialog.Builder(this)
                .setTitle("导入失败")
                .setMessage(e.message ?: "未知错误")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    private fun refreshTaskInfo() {
        val name = AssetRepository.taskName
        tvTaskName.text = if (name.isNullOrEmpty()) {
            "当前任务：暂无"
        } else {
            "当前任务：$name（共 ${AssetRepository.assets.size} 条资产）"
        }
    }

    private fun applyFilter() {
        val keyword = etSearch.text.toString().trim()
        val hasStatusFilter = selectedStatuses.isNotEmpty()

        val filtered = AssetRepository.assets.filter { asset ->
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_task -> {
                AlertDialog.Builder(this)
                    .setTitle("清空当前任务")
                    .setMessage("确定要清空当前任务及资产列表吗？")
                    .setPositiveButton("确定") { _, _ ->
                        AssetRepository.clear()
                        refreshTaskInfo()
                        applyFilter()
                    }
                    .setNegativeButton("取消", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }
}

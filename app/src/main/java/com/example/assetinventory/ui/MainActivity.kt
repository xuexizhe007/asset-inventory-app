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
import com.example.assetinventory.data.LocalStore
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus
import com.example.assetinventory.util.PdfGenerator

class MainActivity : AppCompatActivity() {

    private lateinit var btnBackTaskList: Button
    private lateinit var btnBack: Button
    private lateinit var tvTaskName: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnFilterStatus: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnInventory: Button
    private lateinit var btnPrint: Button

    private lateinit var tvSummaryTotal: TextView
    private lateinit var tvSummaryMatched: TextView
    private lateinit var tvSummaryReprint: TextView
    private lateinit var tvSummaryMismatch: TextView
    private lateinit var tvSummaryUnchecked: TextView

    private lateinit var adapter: AssetAdapter

    private var taskId: Long = -1L
    private var taskName: String = ""
    private var allAssets: List<Asset> = emptyList()
    private var currentFilterStatus: AssetStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: ""

        btnBackTaskList = findViewById(R.id.btnBackTaskList)
        btnBack = findViewById(R.id.btnBack)
        tvTaskName = findViewById(R.id.tvTaskName)
        etSearch = findViewById(R.id.etSearch)
        btnFilterStatus = findViewById(R.id.btnFilterStatus)
        recyclerView = findViewById(R.id.rvAssets)
        btnInventory = findViewById(R.id.btnInventory)
        btnPrint = findViewById(R.id.btnPrint)

        tvSummaryTotal = findViewById(R.id.tvSummaryTotal)
        tvSummaryMatched = findViewById(R.id.tvSummaryMatched)
        tvSummaryReprint = findViewById(R.id.tvSummaryReprint)
        tvSummaryMismatch = findViewById(R.id.tvSummaryMismatch)
        tvSummaryUnchecked = findViewById(R.id.tvSummaryUnchecked)

        tvTaskName.text = "当前任务：$taskName"

        btnBackTaskList.setOnClickListener { finish() }
        btnBack.setOnClickListener { finish() }

        adapter = AssetAdapter(emptyList()) { asset ->
            val intent = Intent(this, AssetDetailActivity::class.java)
            intent.putExtra(AssetDetailActivity.EXTRA_TASK_ID, taskId)
            intent.putExtra(AssetDetailActivity.EXTRA_ASSET_CODE, asset.code)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                applyFilter()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnFilterStatus.setOnClickListener { showFilterDialog() }

        btnInventory.setOnClickListener {
            checkCameraPermissionAndStartScan()
        }

        btnPrint.setOnClickListener {
            val selected = allAssets.filter { it.selectedForPrint }
            PdfGenerator.generateLabelsPdf(this, taskName, selected)
        }
    }

    override fun onResume() {
        super.onResume()
        loadAssets()
    }

    private fun loadAssets() {
        allAssets = LocalStore.getAssetsForTask(this, taskId)

        // 计算 Summary
        val total = allAssets.size
        val matched = allAssets.count { it.status == AssetStatus.MATCHED }
        val reprint = allAssets.count { it.status == AssetStatus.REPRINT }
        val mismatch = allAssets.count { it.status == AssetStatus.MISMATCHED }
        val unchecked = allAssets.count { it.status == AssetStatus.UNCHECKED }

        // ✅ 改为“只显示数字”，标签由布局固定展示
        tvSummaryTotal.text = total.toString()
        tvSummaryMatched.text = matched.toString()
        tvSummaryReprint.text = reprint.toString()
        tvSummaryMismatch.text = mismatch.toString()
        tvSummaryUnchecked.text = unchecked.toString()

        applyFilter()
    }

    private fun applyFilter() {
        val keyword = etSearch.text?.toString()?.trim().orEmpty()

        val filtered = allAssets.filter { asset ->
            val statusOk = currentFilterStatus?.let { asset.status == it } ?: true
            val keywordOk = keyword.isBlank() ||
                asset.code.contains(keyword, ignoreCase = true) ||
                asset.name.contains(keyword, ignoreCase = true)
            statusOk && keywordOk
        }

        adapter.updateItems(filtered)
    }

    private fun showFilterDialog() {
        val items = arrayOf("全部", "相符", "补打标签", "不相符", "未盘点")
        AlertDialog.Builder(this)
            .setTitle("按状态筛选")
            .setItems(items) { _, which ->
                currentFilterStatus = when (which) {
                    1 -> AssetStatus.MATCHED
                    2 -> AssetStatus.REPRINT
                    3 -> AssetStatus.MISMATCHED
                    4 -> AssetStatus.UNCHECKED
                    else -> null
                }
                applyFilter()
            }
            .setNegativeButton("取消", null)
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
        intent.putExtra(QrScanActivity.EXTRA_TASK_ID, taskId)
        intent.putExtra(QrScanActivity.EXTRA_TASK_NAME, taskName)
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

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
    }
}

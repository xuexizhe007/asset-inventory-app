package com.example.assetinventory.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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
    private lateinit var layoutEmptyState: View
    private lateinit var cbSelectAll: CheckBox
    private lateinit var btnInventory: Button
    private lateinit var btnPrint: Button

    private lateinit var tvSummaryTotal: TextView
    private lateinit var tvSummaryMatched: TextView
    private lateinit var tvSummaryReprint: TextView
    private lateinit var tvSummaryMismatch: TextView
    private lateinit var tvSummaryUnchecked: TextView

    private lateinit var adapter: AssetAdapter
    private val selectedStatuses: MutableSet<AssetStatus> = mutableSetOf()
    private var allAssets: MutableList<Asset> = mutableListOf()

    private var taskId: Long = -1L
    private var taskName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 读取任务信息
        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: ""

        // 如果任务名称未通过 Intent 传入（例如从编辑页返回时），
        // 尝试根据 taskId 从本地任务列表中恢复
        if (taskId > 0L && taskName.isBlank()) {
            val tasks = LocalStore.getTasks(this)
            taskName = tasks.firstOrNull { it.id == taskId }?.name ?: ""
        }

        if (taskId <= 0L) {
            Toast.makeText(this, "未找到任务信息", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.title = taskName

        // 绑定视图
        btnBackTaskList = findViewById(R.id.btnBackTaskList)
        btnBack = findViewById(R.id.btnBack)
        tvTaskName = findViewById(R.id.tvTaskName)
        etSearch = findViewById(R.id.etSearch)
        btnFilterStatus = findViewById(R.id.btnFilterStatus)
        recyclerView = findViewById(R.id.rvAssets)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        cbSelectAll = findViewById(R.id.cbSelectAll)
        btnInventory = findViewById(R.id.btnInventory)
        btnPrint = findViewById(R.id.btnPrint)

        tvSummaryTotal = findViewById(R.id.tvSummaryTotal)
        tvSummaryMatched = findViewById(R.id.tvSummaryMatched)
        tvSummaryReprint = findViewById(R.id.tvSummaryReprint)
        tvSummaryMismatch = findViewById(R.id.tvSummaryMismatch)
        tvSummaryUnchecked = findViewById(R.id.tvSummaryUnchecked)

        tvTaskName.text = if (taskName.isNotBlank()) {
            "当前任务：$taskName"
        } else {
            "当前任务："
        }

        // 顶部按钮
        btnBackTaskList.setOnClickListener {
            val intent = Intent(this, TaskListActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }

        // 列表 & 适配器
        adapter = AssetAdapter(emptyList()) { asset ->
            val intent = Intent(this, AssetDetailActivity::class.java)
            intent.putExtra(AssetDetailActivity.EXTRA_TASK_ID, taskId)
            intent.putExtra(AssetDetailActivity.EXTRA_ASSET_CODE, asset.code)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)

        // 全选当前列表
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            val current = adapter.currentItems
            current.forEach { it.selectedForPrint = isChecked }
            adapter.update(current)
        }

        // 搜索
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                applyFilter()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 状态筛选
        btnFilterStatus.setOnClickListener {
            showStatusFilterDialog()
        }

        // 开始盘点（扫码）
        btnInventory.setOnClickListener {
            if (allAssets.isEmpty()) {
                Toast.makeText(this, "该任务没有资产，请先导入任务", Toast.LENGTH_SHORT).show()
            } else {
                checkCameraPermissionAndStartScan()
            }
        }

        // 打印标签（仅当前列表选中项）
        btnPrint.setOnClickListener {
            if (allAssets.isEmpty()) {
                Toast.makeText(this, "该任务没有资产，请先导入任务", Toast.LENGTH_SHORT).show()
            } else {
                val selected = adapter.currentItems.filter { it.selectedForPrint }
                if (selected.isEmpty()) {
                    Toast.makeText(this, "请先勾选需要打印标签的资产", Toast.LENGTH_SHORT).show()
                } else {
                    PdfGenerator.generateLabelsPdf(this, taskName, selected)
                }
            }
        }

        loadAssets()
    }

    override fun onResume() {
        super.onResume()
        loadAssets()
    }

    private fun loadAssets() {
        allAssets = LocalStore.getAssetsForTask(this, taskId).toMutableList()
        updateSummary()
        applyFilter()
    }

    private fun updateSummary() {
        val total = allAssets.size
        val matched = allAssets.count { it.status == AssetStatus.MATCHED }
        val reprint = allAssets.count { it.status == AssetStatus.LABEL_REPRINT }
        val mismatch = allAssets.count { it.status == AssetStatus.MISMATCH }
        val unchecked = allAssets.count { it.status == AssetStatus.UNCHECKED }

        tvSummaryTotal.text = "总资产：$total"
        tvSummaryMatched.text = "相符：$matched"
        tvSummaryReprint.text = "补打标签：$reprint"
        tvSummaryMismatch.text = "不相符：$mismatch"
        tvSummaryUnchecked.text = "未盘点：$unchecked"
    }

    private fun applyFilter() {
        val keyword = etSearch.text.toString().trim()
        val hasStatusFilter = selectedStatuses.isNotEmpty()

        val filtered = allAssets.filter { asset ->
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

        // 空状态显示控制
        if (filtered.isEmpty()) {
            recyclerView.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
        }

        // 同步“全选”复选框状态：只有当当前列表非空且全部选中时才勾上
        cbSelectAll.setOnCheckedChangeListener(null)
        cbSelectAll.isChecked = filtered.isNotEmpty() && filtered.all { it.selectedForPrint }
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            val current = adapter.currentItems
            current.forEach { it.selectedForPrint = isChecked }
            adapter.update(current)
        }
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
            == PackageManager.PERMISSION_GRANTED
        ) {
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

package com.example.assetinventory.ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.assetinventory.R
import com.example.assetinventory.data.LocalStore
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus

class AssetDetailActivity : AppCompatActivity() {

    private lateinit var tvCode: TextView
    private lateinit var tvName: TextView
    // 使用 include 布局内的 ID
    private lateinit var tvUserValue: TextView
    private lateinit var tvDeptValue: TextView
    private lateinit var tvLocationValue: TextView
    private lateinit var tvStartDateValue: TextView
    
    private lateinit var tvStatus: TextView
    private lateinit var btnMatched: Button
    private lateinit var btnMismatch: Button

    private var taskId: Long = -1L
    private var assetCode: String? = null
    private var currentAsset: Asset? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_detail)

        // 启用 Toolbar 返回
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "资产详情"

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        assetCode = intent.getStringExtra(EXTRA_ASSET_CODE)

        if (taskId <= 0L || assetCode.isNullOrEmpty()) {
            Toast.makeText(this, "资产信息缺失", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 绑定视图
        tvCode = findViewById(R.id.tvCode)
        tvName = findViewById(R.id.tvName)
        tvStatus = findViewById(R.id.tvStatus)
        btnMatched = findViewById(R.id.btnMatched)
        btnMismatch = findViewById(R.id.btnMismatch)
        
        // 绑定 include 内部的 TextView (简单粗暴法：先找容器再找ID，或直接全局找ID如果没重复)
        // 这里假设 rowUser 是 include 的 id，tvLabel/tvValue 是内部 id
        findViewById<TextView>(R.id.rowUser).findViewById<TextView>(R.id.tvLabel).text = "使用人"
        tvUserValue = findViewById<TextView>(R.id.rowUser).findViewById(R.id.tvValue)

        findViewById<TextView>(R.id.rowDept).findViewById<TextView>(R.id.tvLabel).text = "使用部门"
        tvDeptValue = findViewById<TextView>(R.id.rowDept).findViewById(R.id.tvValue)

        findViewById<TextView>(R.id.rowLocation).findViewById<TextView>(R.id.tvLabel).text = "存放地点"
        tvLocationValue = findViewById<TextView>(R.id.rowLocation).findViewById(R.id.tvValue)

        findViewById<TextView>(R.id.rowDate).findViewById<TextView>(R.id.tvLabel).text = "投用日期"
        tvStartDateValue = findViewById<TextView>(R.id.rowDate).findViewById(R.id.tvValue)

        // 原 Back 按钮逻辑移除，交给 Toolbar

        btnMatched.setOnClickListener {
            currentAsset?.let { asset ->
                AlertDialog.Builder(this)
                    .setTitle("确认相符")
                    .setMessage("是否需要补打资产标签？")
                    .setNegativeButton("是") { _: DialogInterface, _: Int ->
                        LocalStore.updateAssetStatus(this, taskId, asset.code, AssetStatus.LABEL_REPRINT)
                        Toast.makeText(this, "状态已设为：补打标签", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .setPositiveButton("否") { _: DialogInterface, _: Int ->
                        LocalStore.updateAssetStatus(this, taskId, asset.code, AssetStatus.MATCHED)
                        Toast.makeText(this, "状态已设为：相符", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .show()
            }
        }

        btnMismatch.setOnClickListener {
            currentAsset?.let { asset ->
                AssetEditActivity.start(this, taskId, asset.code)
            }
        }
    }
    
    // 处理 Toolbar 返回点击
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        assetCode?.let {
            val asset = LocalStore.findAssetByCode(this, taskId, it)
            if (asset == null) {
                Toast.makeText(this, "未找到资产信息", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            currentAsset = asset
            render(asset)
        }
    }

    private fun render(asset: Asset) {
        tvCode.text = "编码：${asset.code}"
        val categoryStr = if (asset.category.isNullOrEmpty()) "" else " (${asset.category})"
        tvName.text = "${asset.name}$categoryStr"
        
        tvUserValue.text = asset.user.orEmpty()
        tvDeptValue.text = asset.department.orEmpty()
        tvLocationValue.text = asset.location.orEmpty()
        tvStartDateValue.text = asset.startDate
        
        tvStatus.text = asset.status.displayName
        
        val (bgColorRes, textColorRes) = when (asset.status) {
            AssetStatus.UNCHECKED -> R.color.status_unchecked_bg to R.color.status_unchecked
            AssetStatus.MATCHED -> R.color.status_matched_bg to R.color.status_matched
            AssetStatus.MISMATCH -> R.color.status_mismatch_bg to R.color.status_mismatch
            AssetStatus.LABEL_REPRINT -> R.color.status_reprint_bg to R.color.status_reprint
        }
        tvStatus.backgroundTintList = ContextCompat.getColorStateList(this, bgColorRes)
        tvStatus.setTextColor(ContextCompat.getColor(this, textColorRes))
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_ASSET_CODE = "extra_asset_code"
    }
}

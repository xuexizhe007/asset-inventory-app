package com.example.assetinventory.ui

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.assetinventory.R
import com.example.assetinventory.data.LocalStore
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus

class AssetDetailActivity : AppCompatActivity() {

    private lateinit var tvCode: TextView
    private lateinit var tvName: TextView
    private lateinit var tvUser: TextView
    private lateinit var tvDept: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnMatched: Button
    private lateinit var btnMismatch: Button

    private var taskId: Long = -1L
    private var assetCode: String? = null
    private var currentAsset: Asset? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_detail)

        supportActionBar?.title = "资产详情"

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        assetCode = intent.getStringExtra(EXTRA_ASSET_CODE)

        if (taskId <= 0L || assetCode.isNullOrEmpty()) {
            Toast.makeText(this, "资产信息缺失", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvCode = findViewById(R.id.tvCode)
        tvName = findViewById(R.id.tvName)
        tvUser = findViewById(R.id.tvUser)
        tvDept = findViewById(R.id.tvDept)
        tvLocation = findViewById(R.id.tvLocation)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvStatus = findViewById(R.id.tvStatus)
        btnMatched = findViewById(R.id.btnMatched)
        btnMismatch = findViewById(R.id.btnMismatch)

        btnMatched.setOnClickListener {
            currentAsset?.let { asset ->
                AlertDialog.Builder(this)
                    .setTitle("确认相符")
                    .setMessage("是否需要补打资产标签？")
                    .setPositiveButton("是") { _: DialogInterface, _: Int ->
                        LocalStore.updateAssetStatus(this, taskId, asset.code, AssetStatus.LABEL_REPRINT)
                        Toast.makeText(this, "状态已设为：补打标签", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .setNegativeButton("否") { _: DialogInterface, _: Int ->
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

    override fun onResume() {
        super.onResume()
        // 重新从数据库读取最新状态
        assetCode?.let {
            val asset = LocalStore.findAssetByCode(this, taskId, it)
            if (asset == null) {
                Toast.makeText(this, "未找到资产信息", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            currentAsset = asset

            // 如果状态为不相符，说明在编辑页已确认修改，直接返回资产列表
            if (asset.status == AssetStatus.MISMATCH) {
                finish()
                return
            }

            render(asset)
        }
    }

    private fun render(asset: Asset) {
        tvCode.text = "资产编码：${asset.code}"
        tvName.text = "资产名称：${asset.name}"
        tvUser.text = "使用人：${asset.user.orEmpty()}"
        tvDept.text = "使用部门：${asset.department.orEmpty()}"
        tvLocation.text = "存放地点：${asset.location.orEmpty()}"
        tvStartDate.text = "投用日期：${asset.startDate}"
        tvStatus.text = "当前状态：${asset.status.displayName}"
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_ASSET_CODE = "extra_asset_code"
    }
}

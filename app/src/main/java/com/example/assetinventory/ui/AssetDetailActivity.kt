package com.example.assetinventory.ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.example.assetinventory.R
import com.example.assetinventory.data.LocalStore
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus

class AssetDetailActivity : AppCompatActivity() {

    private lateinit var btnBackTaskList: Button
    private lateinit var btnBack: Button
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

        btnBackTaskList = findViewById(R.id.btnBackTaskList)
        btnBack = findViewById(R.id.btnBack)
        tvCode = findViewById(R.id.tvCode)
        tvName = findViewById(R.id.tvName)
        tvUser = findViewById(R.id.tvUser)
        tvDept = findViewById(R.id.tvDept)
        tvLocation = findViewById(R.id.tvLocation)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvStatus = findViewById(R.id.tvStatus)
        btnMatched = findViewById(R.id.btnMatched)
        btnMismatch = findViewById(R.id.btnMismatch)

        btnBackTaskList.setOnClickListener {
            val intent = Intent(this, TaskListActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }

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

            // 进入详情时，如果状态不是“未盘点”，弹出状态提示
            checkAssetStatusAndShowTip(asset)

            render(asset)
        }
    }

    private fun checkAssetStatusAndShowTip(asset: Asset) {
        if (asset.status == AssetStatus.UNCHECKED) {
            // 未盘点资产，不需要提示
            return
        }

        val statusText = asset.status.displayName
        val fullText = "该资产当前状态为：$statusText"
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf(statusText)
        val end = start + statusText.length
        val color = getStatusColor(asset.status)
        spannable.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        AlertDialog.Builder(this)
            .setMessage(spannable)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun getStatusColor(status: AssetStatus): Int =
        when (status) {
            AssetStatus.UNCHECKED -> getColor(R.color.status_unchecked)
            AssetStatus.MATCHED -> getColor(R.color.status_matched)
            AssetStatus.MISMATCH -> getColor(R.color.status_mismatch)
            AssetStatus.LABEL_REPRINT -> getColor(R.color.status_reprint)
        }

    private fun render(asset: Asset) {
        tvCode.text = asset.code
        tvName.text = asset.name
        tvUser.text = asset.user.orEmpty()
        tvDept.text = asset.department.orEmpty()
        tvLocation.text = asset.location.orEmpty()
        tvStartDate.text = asset.startDate
        tvStatus.text = asset.status.displayName
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_ASSET_CODE = "extra_asset_code"
    }
}

package com.example.assetinventory.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.assetinventory.R
import com.example.assetinventory.data.TaskRepository
import com.example.assetinventory.model.AssetStatus
import com.example.assetinventory.model.Asset

class AssetDetailActivity : AppCompatActivity() {

    private var taskId: Long = -1
    private var assetCode: String? = null

    private lateinit var tvCode: TextView
    private lateinit var tvName: TextView
    private lateinit var tvUser: TextView
    private lateinit var tvDept: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnMatched: Button
    private lateinit var btnMismatch: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_detail)

        supportActionBar?.title = "资产详情"

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        assetCode = intent.getStringExtra(EXTRA_ASSET_CODE)

        tvCode = findViewById(R.id.tvCode)
        tvName = findViewById(R.id.tvName)
        tvUser = findViewById(R.id.tvUser)
        tvDept = findViewById(R.id.tvDept)
        tvLocation = findViewById(R.id.tvLocation)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvStatus = findViewById(R.id.tvStatus)
        btnMatched = findViewById(R.id.btnMatched)
        btnMismatch = findViewById(R.id.btnMismatch)

        val asset = getAsset()
        if (asset == null) {
            Toast.makeText(this, "未找到资产", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        render(asset)

        btnMatched.setOnClickListener {
            val a = getAsset() ?: return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("确认相符")
                .setMessage("是否需要补打资产标签？")
                .setPositiveButton("是") { _, _ ->
                    a.status = AssetStatus.LABEL_REPRINT
                    Toast.makeText(this, "状态已设为：补打标签", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("否") { _, _ ->
                    a.status = AssetStatus.MATCHED
                    Toast.makeText(this, "状态已设为：相符", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .show()
        }

        btnMismatch.setOnClickListener {
            val a = getAsset() ?: return@setOnClickListener
            AssetEditActivity.start(this, taskId, a.code)
            // 不留在详情页，直接关闭，返回列表（或编辑完成后返回列表）
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        getAsset()?.let { render(it) }
    }

    private fun getAsset(): Asset? {
        val task = TaskRepository.getTask(taskId) ?: return null
        val code = assetCode ?: return null
        return task.assets.firstOrNull { it.code == code }
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
        private const val EXTRA_TASK_ID = "extra_task_id"
        private const val EXTRA_ASSET_CODE = "extra_asset_code"

        fun start(context: Context, taskId: Long, assetCode: String) {
            val intent = Intent(context, AssetDetailActivity::class.java)
            intent.putExtra(EXTRA_TASK_ID, taskId)
            intent.putExtra(EXTRA_ASSET_CODE, assetCode)
            context.startActivity(intent)
        }
    }
}

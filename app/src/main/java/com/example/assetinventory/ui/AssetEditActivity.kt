package com.example.assetinventory.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.assetinventory.R
import com.example.assetinventory.data.TaskRepository
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus

class AssetEditActivity : AppCompatActivity() {

    private var taskId: Long = -1
    private var assetCode: String? = null

    private lateinit var tvCode: TextView
    private lateinit var tvName: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var etUser: EditText
    private lateinit var etDept: EditText
    private lateinit var etLocation: EditText
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_edit)

        supportActionBar?.title = "修改资产信息"

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        assetCode = intent.getStringExtra(EXTRA_ASSET_CODE)

        tvCode = findViewById(R.id.tvCode)
        tvName = findViewById(R.id.tvName)
        tvStartDate = findViewById(R.id.tvStartDate)
        etUser = findViewById(R.id.etUser)
        etDept = findViewById(R.id.etDept)
        etLocation = findViewById(R.id.etLocation)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)

        val asset = getAsset()
        if (asset == null) {
            Toast.makeText(this, "未找到资产", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvCode.text = "资产编码：${asset.code}"
        tvName.text = "资产名称：${asset.name}"
        tvStartDate.text = "投用日期：${asset.startDate}"
        etUser.setText(asset.user.orEmpty())
        etDept.setText(asset.department.orEmpty())
        etLocation.setText(asset.location.orEmpty())

        btnCancel.setOnClickListener {
            finish() // 放弃修改，返回资产列表
        }

        btnConfirm.setOnClickListener {
            val a = getAsset()
            if (a == null) {
                Toast.makeText(this, "资产不存在", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }
            a.user = etUser.text.toString().trim()
            a.department = etDept.text.toString().trim()
            a.location = etLocation.text.toString().trim()
            a.status = AssetStatus.MISMATCH
            Toast.makeText(this, "修改已保存，状态已设为：不相符", Toast.LENGTH_SHORT).show()
            finish() // 返回资产列表
        }
    }

    private fun getAsset(): Asset? {
        val task = TaskRepository.getTask(taskId) ?: return null
        val code = assetCode ?: return null
        return task.assets.firstOrNull { it.code == code }
    }

    companion object {
        private const val EXTRA_TASK_ID = "extra_task_id"
        private const val EXTRA_ASSET_CODE = "extra_asset_code"

        fun start(context: Context, taskId: Long, assetCode: String) {
            val intent = Intent(context, AssetEditActivity::class.java)
            intent.putExtra(EXTRA_TASK_ID, taskId)
            intent.putExtra(EXTRA_ASSET_CODE, assetCode)
            context.startActivity(intent)
        }
    }
}

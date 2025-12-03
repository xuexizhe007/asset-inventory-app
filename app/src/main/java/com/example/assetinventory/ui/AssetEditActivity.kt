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
import com.example.assetinventory.data.LocalStore
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus

class AssetEditActivity : AppCompatActivity() {

    private lateinit var btnBackTaskList: Button
    private lateinit var btnBack: Button
    private lateinit var tvCode: TextView
    private lateinit var tvName: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var etUser: EditText
    private lateinit var etDept: EditText
    private lateinit var etLocation: EditText
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button

    private var taskId: Long = -1L
    private var assetCode: String? = null
    private var currentAsset: Asset? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_edit)

        supportActionBar?.title = "修改资产信息"

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
        tvStartDate = findViewById(R.id.tvStartDate)
        etUser = findViewById(R.id.etUser)
        etDept = findViewById(R.id.etDept)
        etLocation = findViewById(R.id.etLocation)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)

        btnBackTaskList.setOnClickListener {
            val intent = Intent(this, TaskListActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }

        loadAsset()

        btnCancel.setOnClickListener {
            finish()
        }

        btnConfirm.setOnClickListener {
            val user = etUser.text.toString().trim()
            val dept = etDept.text.toString().trim()
            val loc = etLocation.text.toString().trim()

            LocalStore.updateAssetDetails(
                this,
                taskId = taskId,
                code = assetCode!!,
                user = user,
                department = dept,
                location = loc,
                status = AssetStatus.MISMATCH
            )

            Toast.makeText(this, "修改已保存，状态已设为：不相符", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadAsset() {
        val asset = LocalStore.findAssetByCode(this, taskId, assetCode!!)
        if (asset == null) {
            Toast.makeText(this, "未找到资产信息", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentAsset = asset
        tvCode.text = "资产编码：${asset.code}"
        tvName.text = "资产名称：${asset.name}"
        tvStartDate.text = "投用日期：${asset.startDate}"
        etUser.setText(asset.user.orEmpty())
        etDept.setText(asset.department.orEmpty())
        etLocation.setText(asset.location.orEmpty())
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

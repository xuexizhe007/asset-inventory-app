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
import com.example.assetinventory.data.AssetRepository
import com.example.assetinventory.model.AssetStatus

class AssetEditActivity : AppCompatActivity() {

    private lateinit var tvCode: TextView
    private lateinit var tvName: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var etUser: EditText
    private lateinit var etDept: EditText
    private lateinit var etLocation: EditText
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button

    private var assetCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_edit)

        supportActionBar?.title = "修改资产信息"

        tvCode = findViewById(R.id.tvCode)
        tvName = findViewById(R.id.tvName)
        tvStartDate = findViewById(R.id.tvStartDate)
        etUser = findViewById(R.id.etUser)
        etDept = findViewById(R.id.etDept)
        etLocation = findViewById(R.id.etLocation)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)

        assetCode = intent.getStringExtra(EXTRA_ASSET_CODE)
        val asset = assetCode?.let { AssetRepository.findByCode(it) }

        if (asset == null) {
            Toast.makeText(this, "未找到资产信息", Toast.LENGTH_SHORT).show()
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
            finish()
        }

        btnConfirm.setOnClickListener {
            asset.user = etUser.text.toString().trim()
            asset.department = etDept.text.toString().trim()
            asset.location = etLocation.text.toString().trim()
            asset.status = AssetStatus.MISMATCH

            Toast.makeText(this, "修改已保存，状态已设为：不相符", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        private const val EXTRA_ASSET_CODE = "extra_asset_code"

        fun start(context: Context, assetCode: String) {
            val intent = Intent(context, AssetEditActivity::class.java)
            intent.putExtra(EXTRA_ASSET_CODE, assetCode)
            context.startActivity(intent)
        }
    }
}

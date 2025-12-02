package com.example.assetinventory.ui

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.assetinventory.R
import com.example.assetinventory.data.AssetRepository
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

    private var assetCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_detail)

        supportActionBar?.title = "资产详情"

        tvCode = findViewById(R.id.tvCode)
        tvName = findViewById(R.id.tvName)
        tvUser = findViewById(R.id.tvUser)
        tvDept = findViewById(R.id.tvDept)
        tvLocation = findViewById(R.id.tvLocation)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvStatus = findViewById(R.id.tvStatus)
        btnMatched = findViewById(R.id.btnMatched)
        btnMismatch = findViewById(R.id.btnMismatch)

        assetCode = intent.getStringExtra(EXTRA_ASSET_CODE)

        val asset = assetCode?.let { AssetRepository.findByCode(it) }
        if (asset == null) {
            Toast.makeText(this, "未找到资产信息", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        render(asset)

        btnMatched.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("确认相符")
                .setMessage("是否需要补打资产标签？")
                .setPositiveButton("是") { _: DialogInterface, _: Int ->
                    asset.status = AssetStatus.LABEL_REPRINT
                    Toast.makeText(this, "状态已设为：补打标签", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("否") { _: DialogInterface, _: Int ->
                    asset.status = AssetStatus.MATCHED
                    Toast.makeText(this, "状态已设为：相符", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .show()
        }

        btnMismatch.setOnClickListener {
            AssetEditActivity.start(this, asset.code)
        }
    }

    override fun onResume() {
        super.onResume()
        assetCode?.let {
            val asset = AssetRepository.findByCode(it)
            if (asset != null) {
                render(asset)
            }
        }
    }

    private fun render(asset: com.example.assetinventory.model.Asset) {
        tvCode.text = "资产编码：${asset.code}"
        tvName.text = "资产名称：${asset.name}"
        tvUser.text = "使用人：${asset.user.orEmpty()}"
        tvDept.text = "使用部门：${asset.department.orEmpty()}"
        tvLocation.text = "存放地点：${asset.location.orEmpty()}"
        tvStartDate.text = "投用日期：${asset.startDate}"
        tvStatus.text = "当前状态：${asset.status.displayName}"
    }

    companion object {
        const val EXTRA_ASSET_CODE = "extra_asset_code"
    }
}

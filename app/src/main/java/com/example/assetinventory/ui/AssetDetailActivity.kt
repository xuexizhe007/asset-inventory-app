package com.example.assetinventory.ui

import android.content.DialogInterface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.assetinventory.R
import com.example.assetinventory.data.AssetRepository
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

    private var assetCode: String? = null
    private var asset: Asset? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AssetRepository.init(this)
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
        asset = assetCode?.let { AssetRepository.findAssetInCurrentTask(this, it) }

        if (asset == null) {
            Toast.makeText(this, "未找到资产信息", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        render()

        // 若资产已盘点过，提示可能重复盘点
        if (asset?.status != AssetStatus.UNCHECKED) {
            showAlreadyCheckedDialog()
        }

        btnMatched.setOnClickListener {
            val assetObj = asset ?: return@setOnClickListener
            // 调整按钮顺序：「是」在前，「否」在后
            val builder = AlertDialog.Builder(this)
                .setTitle("确认相符")
                .setMessage("是否需要补打资产标签？")
                .setNegativeButton("是") { _: DialogInterface, _: Int ->
                    assetObj.status = AssetStatus.LABEL_REPRINT
                    AssetRepository.onTaskChanged(this)
                    Toast.makeText(this, "状态已设为：补打标签", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setPositiveButton("否") { _: DialogInterface, _: Int ->
                    assetObj.status = AssetStatus.MATCHED
                    AssetRepository.onTaskChanged(this)
                    Toast.makeText(this, "状态已设为：相符", Toast.LENGTH_SHORT).show()
                    finish()
                }
            builder.show()
        }

        btnMismatch.setOnClickListener {
            val assetObj = asset ?: return@setOnClickListener
            AssetEditActivity.start(this, assetObj.code)
            // 修改完成后希望回到列表，而不是详情，因此这里把详情页关掉
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        assetCode?.let {
            asset = AssetRepository.findAssetInCurrentTask(this, it)
            if (asset == null) {
                finish()
            } else {
                render()
            }
        }
    }

    private fun render() {
        val a = asset ?: return
        tvCode.text = "资产编码：${a.code}"
        tvName.text = "资产名称：${a.name}"
        tvUser.text = "使用人：${a.user.orEmpty()}"
        tvDept.text = "使用部门：${a.department.orEmpty()}"
        tvLocation.text = "存放地点：${a.location.orEmpty()}"
        tvStartDate.text = "投用日期：${a.startDate}"
        tvStatus.text = "当前状态：${a.status.displayName}"
    }

    private fun showAlreadyCheckedDialog() {
        val a = asset ?: return
        val statusText = a.status.displayName
        val base = "该资产当前状态为：$statusText，请核查是否重复盘点。"
        val builder = SpannableStringBuilder(base)
        val startIndex = base.indexOf(statusText)
        if (startIndex >= 0) {
            val endIndex = startIndex + statusText.length
            val color = ContextCompat.getColor(this, a.status.colorResId)
            builder.setSpan(
                ForegroundColorSpan(color),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        AlertDialog.Builder(this)
            .setMessage(builder)
            .setPositiveButton(R.string.dialog_ok, null)
            .show()
    }

    companion object {
        const val EXTRA_ASSET_CODE = "extra_asset_code"
    }
}

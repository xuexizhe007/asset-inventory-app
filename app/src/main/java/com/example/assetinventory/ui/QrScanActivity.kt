package com.example.assetinventory.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.assetinventory.R
import com.example.assetinventory.data.LocalStore
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QrScanActivity : AppCompatActivity() {

    private lateinit var btnBackTaskList: Button
    private lateinit var btnBack: Button
    private lateinit var barcodeView: DecoratedBarcodeView

    private var handled = false
    private var taskId: Long = -1L
    private var taskName: String = ""

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            if (result == null || handled) return
            handled = true
            val code = result.text

            val asset = LocalStore.findAssetByCode(this@QrScanActivity, taskId, code)
            if (asset == null) {
                Toast.makeText(this@QrScanActivity, "未找到资产编码：$code", Toast.LENGTH_LONG).show()
                finish()
            } else {
                val intent = Intent(this@QrScanActivity, AssetDetailActivity::class.java)
                intent.putExtra(AssetDetailActivity.EXTRA_TASK_ID, taskId)
                intent.putExtra(AssetDetailActivity.EXTRA_ASSET_CODE, asset.code)
                startActivity(intent)
                finish()
            }
        }

        override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scan)

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: ""

        if (taskId <= 0L) {
            Toast.makeText(this, "任务信息缺失", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.title = "扫码盘点 - $taskName"

        btnBackTaskList = findViewById(R.id.btnBackTaskList)
        btnBack = findViewById(R.id.btnBack)
        barcodeView = findViewById(R.id.barcodeScanner)

        btnBackTaskList.setOnClickListener {
            val intent = Intent(this, TaskListActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }

        barcodeView.decodeContinuous(callback)
    }

    override fun onResume() {
        super.onResume()
        handled = false
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
    }
}

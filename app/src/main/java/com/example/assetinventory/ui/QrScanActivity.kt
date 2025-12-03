package com.example.assetinventory.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.assetinventory.R
import com.example.assetinventory.data.TaskRepository
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QrScanActivity : AppCompatActivity() {

    private var taskId: Long = -1
    private lateinit var barcodeView: DecoratedBarcodeView
    private var handled = false

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            if (result == null || handled) return
            handled = true
            val code = result.text
            val task = TaskRepository.getTask(taskId)
            val asset = task?.assets?.firstOrNull { it.code == code }
            if (asset == null) {
                Toast.makeText(this@QrScanActivity, "未找到资产编码：$code", Toast.LENGTH_LONG).show()
                finish()
            } else {
                AssetDetailActivity.start(this@QrScanActivity, taskId, asset.code)
                finish()
            }
        }

        override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scan)

        supportActionBar?.title = "扫码盘点"

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        barcodeView = findViewById(R.id.barcodeScanner)
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
    }
}

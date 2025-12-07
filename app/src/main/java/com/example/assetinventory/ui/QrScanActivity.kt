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
import com.journeyapps.barcodescanner.CameraSettings

class QrScanActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "com.example.assetinventory.EXTRA_TASK_ID"
        const val EXTRA_TASK_NAME = "com.example.assetinventory.EXTRA_TASK_NAME"
    }

    private lateinit var btnBackTaskList: Button
    private lateinit var btnBack: Button
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var btnFlashlight: Button
    private lateinit var btnZoomIn: Button
    private lateinit var btnZoomOut: Button

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
            Toast.makeText(this, "无效的任务ID", Toast.LENGTH_LONG).show()
            finish()
        }

        barcodeView = findViewById(R.id.barcode_scanner)
        barcodeView.decodeContinuous(callback)

        btnBackTaskList = findViewById(R.id.btn_back_task_list)
        btnBack = findViewById(R.id.btn_back)

        btnBackTaskList.setOnClickListener { finish() }
        btnBack.setOnClickListener { finish() }

        // 初始化放大和闪光灯控制按钮
        btnFlashlight = findViewById(R.id.btn_flashlight)
        btnZoomIn = findViewById(R.id.btn_zoom_in)
        btnZoomOut = findViewById(R.id.btn_zoom_out)

        // 切换闪光灯状态
        btnFlashlight.setOnClickListener {
            if (barcodeView.isTorchOn) {
                barcodeView.setTorchOff()
            } else {
                barcodeView.setTorchOn()
            }
        }

        // 放大
        btnZoomIn.setOnClickListener {
            val cameraSettings = barcodeView.getCameraSettings()
            cameraSettings.zoomFactor += 0.1f // 增加放大级别
            barcodeView.setCameraSettings(cameraSettings)
        }

        // 缩小
        btnZoomOut.setOnClickListener {
            val cameraSettings = barcodeView.getCameraSettings()
            cameraSettings.zoomFactor -= 0.1f // 减小放大级别
            barcodeView.setCameraSettings(cameraSettings)
        }
    }
}

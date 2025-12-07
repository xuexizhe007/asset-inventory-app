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
import com.journeyapps.barcodescanner.DecoratedBarcodeView.TorchListener

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

        // 设置闪光灯监听器
        barcodeView.setTorchListener(object : TorchListener {
            override fun onTorchOn() {
                // 闪光灯打开时执行的操作
            }

            override fun onTorchOff() {
                // 闪光灯关闭时执行的操作
            }
        })

        // 切换闪光灯状态
        btnFlashlight.setOnClickListener {
            if (barcodeView.isTorchOn) {
                barcodeView.setTorchOff()
            } else {
                barcodeView.setTorchOn()
            }
        }

        // 放大（更改缩放级别）
        btnZoomIn.setOnClickListener {
            // 缩放操作
            barcodeView.camera?.let { camera ->
                if (camera.parameters.isZoomSupported) {
                    val zoom = camera.parameters.zoom + 1
                    camera.parameters.zoom = zoom
                    camera.parameters = camera.parameters
                }
            }
        }

        // 缩小（更改缩放级别）
        btnZoomOut.setOnClickListener {
            // 缩放操作
            barcodeView.camera?.let { camera ->
                if (camera.parameters.isZoomSupported) {
                    val zoom = camera.parameters.zoom - 1
                    camera.parameters.zoom = zoom
                    camera.parameters = camera.parameters
                }
            }
        }
    }
}

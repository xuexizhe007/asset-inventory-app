package com.example.assetinventory.ui

import android.content.Intent
import android.hardware.Camera
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
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
    private lateinit var btnFlash: Button
    private lateinit var seekBarZoom: SeekBar
    private lateinit var tvTitle: TextView // 用于更新标题

    private var handled = false
    private var taskId: Long = -1L
    private var taskName: String = ""
    
    // 闪光灯状态
    private var isFlashOn = false

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            if (result == null || handled) return
            handled = true
            val code = result.text

            val asset = LocalStore.findAssetByCode(this@QrScanActivity, taskId, code)
            if (asset == null) {
                Toast.makeText(this@QrScanActivity, "未找到资产编码：$code", Toast.LENGTH_LONG).show()
                // 延迟一下重新允许扫码，否则Toast可能重叠
                barcodeView.postDelayed({ handled = false }, 2000)
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

        // 初始化视图
        btnBackTaskList = findViewById(R.id.btnBackTaskList)
        btnBack = findViewById(R.id.btnBack)
        barcodeView = findViewById(R.id.barcodeScanner)
        btnFlash = findViewById(R.id.btnFlash)
        seekBarZoom = findViewById(R.id.seekBarZoom)
        tvTitle = findViewById(R.id.tvTitle)

        // 设置标题
        tvTitle.text = "扫码 - $taskName"
        // 如果使用了系统 ActionBar，也可以设置一下
        supportActionBar?.hide() // 使用了自定义 Toolbar，隐藏系统 ActionBar

        // 按钮事件
        btnBackTaskList.setOnClickListener {
            val intent = Intent(this, TaskListActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }

        // --- 功能 1: 闪光灯控制 ---
        btnFlash.setOnClickListener {
            if (isFlashOn) {
                barcodeView.setTorchOff()
                isFlashOn = false
                btnFlash.text = "开启闪光灯"
            } else {
                barcodeView.setTorchOn()
                isFlashOn = true
                btnFlash.text = "关闭闪光灯"
            }
        }

        // --- 功能 2: 镜头缩放控制 ---
        // SeekBar 范围 0-100，映射到相机的 Zoom 级别
        seekBarZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateCameraZoom(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        barcodeView.decodeContinuous(callback)
    }

    /**
     * 更新相机焦距
     * 使用 ZXing 库的 changeCameraParameters 接口
     */
    private fun updateCameraZoom(progress: Int) {
        barcodeView.barcodeView.changeCameraParameters { params ->
            // Android Camera API (deprecated but used by ZXing Embedded)
            if (params.isZoomSupported) {
                val maxZoom = params.maxZoom
                // 计算目标 zoom 值
                val targetZoom = (maxZoom * progress) / 100
                
                if (params.zoom != targetZoom) {
                    params.zoom = targetZoom
                }
            }
            params
        }
    }

    override fun onResume() {
        super.onResume()
        handled = false
        barcodeView.resume()
        
        // 恢复时重置 UI 状态
        if (isFlashOn) {
            barcodeView.setTorchOn()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    // 处理按键事件，如音量键控制缩放（可选增强体验）
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
    }
}

package com.example.assetinventory.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.assetinventory.R
import com.example.assetinventory.data.AssetRepository
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QrScanActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private var handled = false

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            if (result == null || handled) return
            handled = true
            val code = result.text
            val asset = AssetRepository.findAssetInCurrentTask(this@QrScanActivity, code)
            if (asset == null) {
                Toast.makeText(this@QrScanActivity, "未找到资产编码：$code", Toast.LENGTH_LONG).show()
                finish()
            } else {
                val intent = android.content.Intent(this@QrScanActivity, AssetDetailActivity::class.java)
                intent.putExtra(AssetDetailActivity.EXTRA_ASSET_CODE, asset.code)
                startActivity(intent)
                finish()
            }
        }

        override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AssetRepository.init(this)
        setContentView(R.layout.activity_qr_scan)

        supportActionBar?.title = "扫码盘点"

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
}

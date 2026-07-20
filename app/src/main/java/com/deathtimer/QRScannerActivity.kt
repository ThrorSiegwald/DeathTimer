package com.deathtimer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScannerActivity : AppCompatActivity() {

    private lateinit var cameraPreview: PreviewView
    private lateinit var resultTextView: TextView
    private lateinit var addButton: Button
    private lateinit var cancelButton: Button
    private lateinit var cameraExecutor: ExecutorService

    private var scannedSeconds = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Сканирование QR"

        cameraPreview = findViewById(R.id.cameraPreview)
        resultTextView = findViewById(R.id.resultTextView)
        addButton = findViewById(R.id.addButton)
        cancelButton = findViewById(R.id.cancelButton)

        cameraExecutor = Executors.newSingleThreadExecutor()

        addButton.isEnabled = false
        addButton.setOnClickListener { addTime() }
        cancelButton.setOnClickListener { finish() }

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = cameraPreview.surfaceProvider
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            processImage(image, imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("QRScanner", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(image: InputImage, imageProxy: androidx.camera.core.ImageProxy) {
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    if (barcode.valueType == Barcode.TYPE_TEXT) {
                        val text = barcode.rawValue ?: ""
                        parseQRCode(text)
                        imageProxy.close()
                        return@addOnSuccessListener
                    }
                }
                imageProxy.close()
            }
            .addOnFailureListener { e ->
                Log.e("QRScanner", "Barcode scanning failed", e)
                imageProxy.close()
            }
    }

    private fun parseQRCode(text: String) {
        val trimmed = text.trim()
        if (trimmed.startsWith("add_time ", ignoreCase = true)) {
            val timePart = trimmed.substringAfter("add_time ").trim()
            val seconds = timePart.toIntOrNull()
            if (seconds != null && seconds > 0) {
                scannedSeconds = seconds
                runOnUiThread {
                    resultTextView.text = "Найдено: +${seconds} секунд"
                    resultTextView.setTextColor(ContextCompat.getColor(this, R.color.green))
                    addButton.isEnabled = true
                }
            } else {
                runOnUiThread {
                    resultTextView.text = "Неверный формат: $trimmed"
                    resultTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
                    addButton.isEnabled = false
                }
            }
        } else {
            runOnUiThread {
                resultTextView.text = "Неверный QR: $trimmed"
                resultTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
                addButton.isEnabled = false
            }
        }
    }

    private fun addTime() {
        if (scannedSeconds <= 0) return

        if (!PreferenceManager.isRunning(this)) {
            Toast.makeText(this, "Сначала запустите таймер", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_ADD_TIME
            putExtra(TimerService.EXTRA_SECONDS, scannedSeconds)
        }
        startService(serviceIntent)

        val remaining = PreferenceManager.getRemainingSeconds(this) + scannedSeconds
        PreferenceManager.saveRemainingSeconds(this, remaining)

        Toast.makeText(this, "+${scannedSeconds} секунд добавлено", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

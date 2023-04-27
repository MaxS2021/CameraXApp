package com.smastcomm.cameraxapp



import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smastcomm.cameraxapp.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDir: File
    private lateinit var cameraExecutor: ExecutorService
    private var cameraFacing = CameraSelector.DEFAULT_BACK_CAMERA
    lateinit var camera: CameraProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        outputDir = getOutputDir()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionGranted()) {
            //Toast.makeText(this, "Разрешение для Камеры получено", Toast.LENGTH_SHORT).show()
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSION
            )
        }

        binding.capture.setOnClickListener {
            takePhoto()
        }

        binding.flipCamera.setOnClickListener {
            if (cameraFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
                cameraFacing = CameraSelector.DEFAULT_FRONT_CAMERA

            } else {
                cameraFacing = CameraSelector.DEFAULT_BACK_CAMERA
            }
            //Log.d(Constants.TAG, "$cameraFacing")
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun getOutputDir(): File {
        val mediaDir = externalMediaDirs.firstOrNull().let {mFile->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdir()
            }
        }
        return if(mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun takePhoto() {
        val imageCapture = imageCapture?: return
        val photoFile = File(outputDir,
            SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.getDefault())
                .format(System.currentTimeMillis()) + ".png")

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOption, ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                val msg = "Фото сохранено  $savedUri"
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()

            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(Constants.TAG, "Ошибка: ${exception.message}", exception )
            }

        })


    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            val cameraProvaider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {mPreview ->
                mPreview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

            }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = cameraFacing //CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvaider.unbindAll()
                val camera = cameraProvaider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                //Log.d(Constants.TAG, "$cameraSelector")
                
                binding.toggleFlash.setOnClickListener {
                    if (camera.cameraInfo.hasFlashUnit()) {
                        if (camera.cameraInfo.torchState.value === 0) {
                            camera.cameraControl.enableTorch(true)
                            binding.toggleFlash.setImageResource(R.drawable.baseline_flash_off_24)
                        } else {
                            camera.cameraControl.enableTorch(false)
                            binding.toggleFlash.setImageResource(R.drawable.baseline_flash_on_24)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Вспышка не доступна",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    //setFlashIcon(camera)
                }

            } catch (e: java.lang.Exception) {
                Log.d(Constants.TAG, "ошибка запуска камеры", e )
            }
        }, ContextCompat.getMainExecutor(this))
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSION) {
            if (allPermissionGranted()) {
                startCamera()

            } else {
                Toast.makeText(this, "Разрешение Камеры НЕ получено", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }



    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
}
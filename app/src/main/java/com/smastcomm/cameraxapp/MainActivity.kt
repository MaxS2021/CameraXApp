package com.smastcomm.cameraxapp



import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smastcomm.cameraxapp.databinding.ActivityMainBinding
import java.io.File
import java.lang.Exception
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

    private lateinit var videoCapture: VideoCapture

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApi", "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        getPermission()

        outputDir = getOutputDir()
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.startVideo.setOnClickListener {
            binding.startVideo.setImageResource(R.drawable.ic_play_red)

            val videoFile = File(outputDir,
                SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.getDefault())
                    .format(System.currentTimeMillis()) + ".mp4")

            val outputFileOptions = VideoCapture.OutputFileOptions.Builder(videoFile).build()

            videoCapture.startRecording(outputFileOptions,ContextCompat.getMainExecutor(this),
                object: VideoCapture.OnVideoSavedCallback{
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    Log.d(Constants.TAG,"Видео записано")
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    Log.d(Constants.TAG,"Ошибка записи Видео: " + message)
                }

            })
        }

        binding.stoptVideo.setOnClickListener {
            binding.startVideo.setImageResource(R.drawable.ic_play_white)
            videoCapture.stopRecording()
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

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            val cameraProvaider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {mPreview ->
                mPreview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

            }
            imageCapture = ImageCapture.Builder().build()

            val point = Point()
            val size = display?.getRealSize(point)

            videoCapture = VideoCapture.Builder()
                .setTargetResolution(Size(point.x,point.y))
                .setAudioBitRate(320000)
                .setAudioSampleRate(44100)
                .setAudioChannelCount(2)
                .build()


            val cameraSelector = cameraFacing //CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvaider.unbindAll()
                val camera = cameraProvaider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture)
                //Log.d(Constants.TAG, "$cameraSelector")



                binding.capture.setOnClickListener {
                    //takePhoto()
                    val photoFile = File(outputDir,
                        SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.getDefault())
                            .format(System.currentTimeMillis()) + ".png")

                    val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture!!.takePicture(outputOption, ContextCompat.getMainExecutor(this),  //Executors.newCachedThreadPool()
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

                }

            } catch (e: Exception) {
                Log.d(Constants.TAG, "ошибка запуска камеры", e )
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun getPermission() {
        var permissionList = mutableListOf<String>()
        Constants.REQUIRED_PERMISSIONS.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(it)
            }
        }
        if (permissionList.size > 0) {
            requestPermissions(permissionList.toTypedArray(), Constants.REQUEST_CODE_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                getPermission()
            }
        }
    }
}
package com.camarazoom
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cesc.camerasdk.MeterCaptureActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private var currentPhotoPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(com.camarazoom.R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(com.camarazoom.R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        setupLaunchers()
    }

    private fun setupLaunchers() {
        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    processCapturedImage()
                } else {
                    Log.e("Camera", "Capture cancelled")
                }
            }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    prepareAndLaunchCamera()
                } else {
                    Toast.makeText(
                        this,
                        "Camera permission is required to capture images",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // ✅ Capture button click
    fun onbtntakePicture(view: View) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            prepareAndLaunchCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ✅ Prepare camera
    private fun prepareAndLaunchCamera() {
        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())
        val fileName = "kno_$timeStamp.jpg"
        currentPhotoPath = fileName

        val intent = Intent(this, MeterCaptureActivity::class.java)
        intent.putExtra("FILE_NAME", fileName)
        cameraLauncher.launch(intent)
    }

    // ✅ Process image safely
    private fun processCapturedImage() {
        try {
            val file = File(getExternalFilesDir(null), currentPhotoPath)
            if (!file.exists()) {
                Log.e("Camera", "Image file not found")
                return
            }
            // Decode bitmap efficiently
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val rotatedBitmap = getRotateImage(
                file.absolutePath,
                bitmap
            )
            val imageView = findViewById<ImageView>(R.id.perview)
            imageView.setImageBitmap(rotatedBitmap)
            // TODO use rotatedBitmap
            Log.e("Camera", "Image processed successfully")

            if (storeCameraPhotoInSDCard(rotatedBitmap ?: bitmap, currentPhotoPath)) {
                val mBitmap: Bitmap? = getImageFileFromSDCard("dis_" + currentPhotoPath)
                if (mBitmap != null) {
                    val targetImageView = findViewById<ImageView>(R.id.targetImageView)
                    targetImageView.setImageBitmap(mBitmap)
                }
            } else {
                Log.e("Camera", "Image Saving Failed...!!! Please try again ...!!!")
            }
        } catch (e: Exception) {
            Log.e("Camera", "Error processing image", e)
        }
    }

    // ✅ Handle image rotation
    @Throws(IOException::class)
    private fun getRotateImage(
        photoPath: String,
        bitmap: Bitmap?
    ): Bitmap? {

        if (bitmap == null) return null

        val exif = ExifInterface(photoPath)

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        return when (orientation) {

            ExifInterface.ORIENTATION_ROTATE_90 ->
                rotateImage(bitmap, 90f)

            ExifInterface.ORIENTATION_ROTATE_180 ->
                rotateImage(bitmap, 180f)

            ExifInterface.ORIENTATION_ROTATE_270 ->
                rotateImage(bitmap, 270f)

            else -> bitmap
        }
    }

    // ✅ Rotate bitmap
    private fun rotateImage(
        source: Bitmap,
        angle: Float
    ): Bitmap {

        val matrix = Matrix()
        matrix.postRotate(angle)

        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )
    }


    private fun storeCameraPhotoInSDCard(bitmap: Bitmap, currentDate: String): Boolean {
        val timeStamp1 = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val timeStamp = "." + timeStamp1
        @Suppress("DEPRECATION")
        val myDirectory = File(getExternalMediaDirs().firstOrNull() ?: getExternalFilesDir(null) ?: filesDir, timeStamp)

        if (!myDirectory.exists()) {
            myDirectory.mkdirs()
        }

        val outputFile = File(myDirectory, currentDate)
        val displayFile = File(myDirectory, "dis_" + currentDate)

        try {
            val fileOutputStream = FileOutputStream(outputFile)


            // Standard scaling for original image
            val scaled = Bitmap.createScaledBitmap(bitmap, 1024, 1536, true)

            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val dateTime = sdf.format(Date())

            // 1. Save Original Scaled Image with Watermark
            val cs = Canvas(scaled)
            val tPaint = Paint()
            tPaint.setTextSize(50f)
            tPaint.setColor(Color.YELLOW)
            tPaint.setStyle(Paint.Style.FILL)
            val height = tPaint.measureText("yY")
            cs.drawText(dateTime, 20f, height + cs.getHeight() - 90f, tPaint)

            scaled.compress(Bitmap.CompressFormat.JPEG, 25, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            outputFile.setReadOnly()

            // 2. Save Cropped Display Image ("dis_")
            // CameraX setZoomRatio() applies hardware zoom to BOTH preview and capture.
            // The captured image is already at the hardware zoom level (1x or 2x).
            // Crop the overlay box region directly — no second zoom factor needed.
            // Overlay: 80% of preview width, 50% height ratio, top at (h - boxH) / 3.
            val imageWidth = scaled.width
            val imageHeight = scaled.height

            val cropWidth = (imageWidth * 0.8f).toInt()
            val cropHeight = (cropWidth * 0.5f).toInt()
            val cropStartX = (imageWidth - cropWidth) / 2
            // Mirror ViewfinderOverlay formula: top = (viewH - boxH) / 3
            val cropStartY = (imageHeight - cropHeight) / 3

            // Clamp to prevent out-of-bounds on any device or image size
            val safeCropStartX = cropStartX.coerceIn(0, imageWidth - 1)
            val safeCropStartY = cropStartY.coerceIn(0, imageHeight - 1)
            val safeCropWidth = cropWidth.coerceAtMost(imageWidth - safeCropStartX)
            val safeCropHeight = cropHeight.coerceAtMost(imageHeight - safeCropStartY)

            val cropped = Bitmap.createBitmap(
                scaled,
                safeCropStartX,
                safeCropStartY,
                safeCropWidth,
                safeCropHeight
            )

            val displayOutputStream = FileOutputStream(displayFile)
            cropped.compress(Bitmap.CompressFormat.JPEG, 40, displayOutputStream)
            displayOutputStream.flush()
            displayOutputStream.close()

            return outputFile.exists() && displayFile.exists()
        } catch (e2: IOException) {
            e2.printStackTrace()
            return false
        } catch (e3: java.lang.Exception) {
            return false
        }
    }


    private fun getImageFileFromSDCard(filename: String): Bitmap? {

        val folderName = "." + SimpleDateFormat(
            "yyyyMMdd",
            Locale.getDefault()
        ).format(Date())

        @Suppress("DEPRECATION")
        val mediaDir = getExternalMediaDirs().firstOrNull() ?: getExternalFilesDir(null) ?: filesDir
        val imageFile = File("${mediaDir}/$folderName/$filename")

        return try {

            FileInputStream(imageFile).use { stream ->
                BitmapFactory.decodeStream(stream)
            }

        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }


}
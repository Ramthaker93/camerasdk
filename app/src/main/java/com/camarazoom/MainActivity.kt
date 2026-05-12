package com.camarazoom
import android.content.Intent
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cesc.camerasdk.MeterCaptureActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.jvm.java


class MainActivity : AppCompatActivity() {

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

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

        setupCameraLauncher()
    }

    // ✅ Camera launcher setup
    private fun setupCameraLauncher() {

        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == RESULT_OK) {
                    processCapturedImage()
                } else {
                    Log.e("Camera", "Capture cancelled")
                }
            }
    }

    // ✅ Capture button click
    fun onbtntakePicture(view: View) {
        prepareAndLaunchCamera()
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

            if (storeCameraPhotoInSDCard(bitmap, currentPhotoPath)) {
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
        val myDirectory = File(getExternalMediaDirs()[0], timeStamp)

        if (!myDirectory.exists()) {
            myDirectory.mkdirs()
        }

        val outputFile = File(myDirectory, currentDate)
        val displayFile = File(myDirectory, "dis_" + currentDate)

        try {
            outputFile.setReadOnly()
            val fileOutputStream = FileOutputStream(outputFile)


            // Standard scaling for original image
            val scaled = Bitmap.createScaledBitmap(bitmap, 1024, 1536, true)

            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val dateTime = sdf.format(Calendar.getInstance().getTime())

            // 1. Save Original Scaled Image with Watermark
            val cs = Canvas(scaled)
            val tPaint = Paint()
            tPaint.setTextSize(50f)
            tPaint.setColor(Color.YELLOW)
            tPaint.setStyle(Paint.Style.FILL)
            val height = tPaint.measureText("yY")
            cs.drawText(dateTime, 20f, height + cs.getHeight() - 90f, tPaint)

            Bitmap.createBitmap(scaled).compress(Bitmap.CompressFormat.JPEG, 25, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            // 2. Save Cropped Display Image ("dis_")
            // The 'scaled' bitmap is wide-angle (1.0x).
            // The user aligned the display in an 80% box while seeing a 1.5x zoomed preview.
            // We crop the area corresponding to that box: (80% / 1.5) of the full image width.
            val zoomFactor = 2.0f
            val boxWidthRatio = 0.8f

            val imageWidth = scaled.getWidth()
            val imageHeight = scaled.getHeight()

            // Step 1: visible region after fake zoom
            val visibleWidth = (imageWidth / zoomFactor).toInt()
            val visibleHeight = (imageHeight / zoomFactor).toInt()

            val visibleStartX = (imageWidth - visibleWidth) / 2
            val visibleStartY = (imageHeight - visibleHeight) / 2

            // Step 2: overlay box inside visible region
            val cropWidth = (visibleWidth * boxWidthRatio).toInt()
            val cropHeight = (cropWidth * 0.5f).toInt()

            // Step 3: match overlay positiong
            val cropStartX = visibleStartX + (visibleWidth - cropWidth) / 2
            val boxHeightRatio = 0.5f
            val verticalBias = (1f - boxHeightRatio) / 3f

            val cropStartY = visibleStartY + (visibleHeight * verticalBias).toInt()
            // Step 4: crop
            val cropped = Bitmap.createBitmap(
                scaled,
                cropStartX,
                cropStartY,
                cropWidth,
                cropHeight
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

        val imageFile = File(
            "${getExternalMediaDirs()[0]}/$folderName/$filename"
        )

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
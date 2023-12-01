package com.example.combineimage.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class Utility {
    companion object{
        fun formatFileSize(sizeInBytes: Long): String {
            val kiloByte = 1024
            val megaByte = kiloByte * 1024
            val gigaByte = megaByte * 1024

            return when {
                sizeInBytes >= gigaByte -> String.format("%.2f GB", sizeInBytes.toFloat() / gigaByte)
                sizeInBytes >= megaByte -> String.format("%.2f MB", sizeInBytes.toFloat() / megaByte)
                sizeInBytes >= kiloByte -> String.format("%.2f KB", sizeInBytes.toFloat() / kiloByte)
                else -> "$sizeInBytes Bytes"
            }
        }

        fun convertToBytes(size: Long, unit: String): Long {
            return when (unit.uppercase()) {
                "KB" -> size * 1024
                "MB" -> size * 1024 * 1024
                "GB" -> size * 1024 * 1024 * 1024
                else -> size
            }
        }

        fun getCurrentDateTime(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val currentTime = Date()
            return dateFormat.format(currentTime)
        }

        fun copyFile(src: File, dest: File) {
            FileInputStream(src).use { fis ->
                FileOutputStream(dest).use { os ->
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (fis.read(buffer).also { len = it } != -1) {
                        os.write(buffer, 0, len)
                    }
                }
            }
        }

        fun saveBitmapAsJpeg(bitmap: Bitmap, filePath: String):Boolean {
            val file = File(filePath)
            var outputStream: OutputStream? = null

            try {
                outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                return true
            } catch (e: Exception) {
                throw e
            } finally {
                outputStream?.close()
            }
        }

        /*fun saveBitmapAsJpeg(bitmap: Bitmap, fileName: String): Boolean {
            return try {
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                true
            } catch (e: Exception) {
                Log.d("MainActivity",e.message!!)
                e.printStackTrace()
                false
            }
        }*/

        fun showToast(context: Context,message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        fun showSnackbar(view: View, message: String) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
        }

        fun filePathToBitmap(filePath:String):Bitmap
        {
            return BitmapFactory.decodeFile(filePath)
        }

        fun uriToBitmap(contentResolver:ContentResolver, uri:Uri):Bitmap
        {
            return MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }

        fun getFileSize(filePath: String): Long {
            val file = File(filePath)
            return file.length()
        }

        fun fileObjectToBitmap(file:File):Bitmap
        {
            return filePathToBitmap(file.path)
        }

        fun addBorderToImage(bitmap: Bitmap, borderWidth: Int, borderColor: Int): Bitmap
        {
            if (borderWidth <= 0) {
                return bitmap // Return the original bitmap if borderWidth is 0 or negative
            }

            val borderedBitmap = Bitmap.createBitmap(
                bitmap.width + borderWidth * 2,
                bitmap.height + borderWidth * 2,
                bitmap.config
            )
            val canvas = Canvas(borderedBitmap)
            val paint = Paint().apply {
                color = borderColor
                style = Paint.Style.STROKE
                strokeWidth = borderWidth.toFloat()
            }
            val rect = RectF(
                borderWidth / 2f,
                borderWidth / 2f,
                borderedBitmap.width - borderWidth / 2f,
                borderedBitmap.height - borderWidth / 2f
            )
            canvas.drawBitmap(bitmap, borderWidth.toFloat(), borderWidth.toFloat(), null)
            canvas.drawRect(rect, paint)
            return borderedBitmap
        }

        fun combineImages(
            bitmaps: List<Bitmap>,
            spacing: Int,
            isVerticalCombine: Boolean
        ): Bitmap? {
            if (bitmaps.isEmpty()) {
                return null
            }

            // Calculate combined width and height
            val maxBitmapWidth = bitmaps.maxOf { it.width }
            val maxBitmapHeight = bitmaps.maxOf { it.height }
            val combinedWidth: Int
            val combinedHeight: Int

            if (isVerticalCombine) {
                combinedWidth = maxBitmapWidth
                combinedHeight = bitmaps.sumOf { it.height + spacing }
            } else {
                combinedWidth = bitmaps.sumOf { it.width + spacing }
                combinedHeight = maxBitmapHeight
            }

            // Create the combined bitmap
            val combinedBitmap = Bitmap.createBitmap(combinedWidth, combinedHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(combinedBitmap)

            var currentX = 0
            var currentY = 0

            for (bitmap in bitmaps) {
                if (isVerticalCombine) {
                    canvas.drawBitmap(bitmap, 0f, currentY.toFloat(), null)
                    currentY += bitmap.height + spacing
                } else {
                    canvas.drawBitmap(bitmap, currentX.toFloat(), 0f, null)
                    currentX += bitmap.width + spacing
                }
            }

            return combinedBitmap
        }
        fun truncateName(name: String, maxLength: Int): String {
            return if (name.length > maxLength) {
                "${name.substring(0, maxLength - 3)}..."
            } else {
                name
            }
        }
        fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
            val width = bitmap.width
            val height = bitmap.height

            val ratioBitmap = width.toFloat() / height.toFloat()
            val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

            var finalWidth = maxWidth
            var finalHeight = maxHeight
            if (ratioMax > ratioBitmap) {
                finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
            } else {
                finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
            }

            return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
        }

        fun reduceImageSize(inputPath: String, outputPath: String, desiredFileSizeBytes: Long) {
            // Load the input image file
            val inputBitmap = BitmapFactory.decodeFile(inputPath)

            // Compress the image with different quality levels
            val outputStream = ByteArrayOutputStream()
            var quality = 100 // Starting quality
            inputBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            while (outputStream.toByteArray().size > desiredFileSizeBytes && quality > 0) {
                outputStream.reset()
                quality -= 1 // Reduce quality by 1 at each iteration
                inputBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }

            // Create the output file and write the compressed image
            val fileOutputStream = FileOutputStream(outputPath)
            fileOutputStream.write(outputStream.toByteArray())
            fileOutputStream.flush()
            fileOutputStream.close()

            // Recycle the input bitmap to release memory
            inputBitmap.recycle()
        }

        fun deleteFile(filePath:String):Boolean
        {
            val file = File(filePath)
            return if(file.exists()) {
                file.delete()
            } else{
                true
            }
        }

        fun getDownloadFolderPath(): String? {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            return downloadDir?.absolutePath
        }

        fun Bitmap.rotateLeft(): Bitmap {
            val matrix = Matrix().apply { postRotate(-90f) }
            return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        }

        fun Bitmap.rotateRight(): Bitmap {
            val matrix = Matrix().apply { postRotate(90f) }
            return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        }

        fun Bitmap.mirrorHorizontally(): Bitmap {
            val matrix = Matrix().apply { postScale(-1f, 1f) }
            return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        }

        fun Bitmap.mirrorVertically(): Bitmap {
            val matrix = Matrix().apply { postScale(1f, -1f) }
            return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        }

        fun createFolderAtPath(path: String): File? {
            val folder = File(path)

            if (!folder.exists()) {
                val created = folder.mkdir()
                if (!created) {
                    // Failed to create the folder
                    return null
                }
            }

            return folder
        }

        fun isDarkTheme(context: Context): Boolean {
            return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }

        fun getInputStreamFromUri(context: Context, uri: Uri): InputStream? {
            return context.contentResolver.openInputStream(uri)
        }

        fun saveInputStreamToFile(inputStream: InputStream, filePath: String): File {
            val file = File(filePath)
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024) // 4 KB buffer size (you can adjust this)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            return file
        }

        fun triggerMediaScan(context: Context, file: File) {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/*"),
                null
            )
        }

        fun getRealPath(uri:Uri,context: Context):String{
            var realpath=""
            try{
                if(uri.scheme.equals("content",true))
                {
                    val projection = arrayOf("_data")
                    val cursor = context.contentResolver.query(uri,projection,null,null,null)
                    if(cursor!=null)
                    {
                        val idcolumn = cursor.getColumnIndexOrThrow("_data")
                        cursor.moveToFirst()
                        realpath=cursor.getString(idcolumn)
                        cursor.close()
                    }
                }
                else if(uri.scheme.equals("file",true))
                {
                    realpath=uri.path!!
                }
            }
            catch (ex:IllegalArgumentException)
            {
                Log.e("Utility",ex.toString())
                showToast(context,"${ex.message}")
            }
            return realpath
        }

        fun dismissKeyboard(activity: Activity) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val view: View? = activity.currentFocus
            if (view != null) {
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }
}
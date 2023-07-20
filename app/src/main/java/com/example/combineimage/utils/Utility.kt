package com.example.combineimage.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
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

        fun uriToFilePath(
            selectedVideoUri: Uri,
            contentResolver: ContentResolver
        ): String? {
            val filePath: String
            val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA)
            val cursor: Cursor? =
                contentResolver.query(selectedVideoUri, filePathColumn, null, null, null)
            cursor!!.moveToFirst()
            val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
            filePath = cursor.getString(columnIndex)
            cursor.close()
            return filePath
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

        fun saveBitmapAsJpeg(bitmap: Bitmap, fileName: String): Boolean {
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
        }

        fun showToast(context: Context,message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        fun filePathToBitmap(filePath:String):Bitmap
        {
            return BitmapFactory.decodeFile(filePath)
        }

        fun uriToBitmap(contentResolver:ContentResolver, uri:Uri):Bitmap
        {
            return MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }

        fun uriToFileObject(contentResolver:ContentResolver, uri:Uri):File{
            return File(uriToFilePath(uri,contentResolver)!!)
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


        fun bitmapToFile(bitmap: Bitmap): File? {
            // Get the external storage directory
            val storageDir = Environment.getExternalStorageDirectory()

            // Create a temporary file name
            val fileName = "temp_${getCurrentDateTime()}.jpg"

            // Create the file object
            val file = File(storageDir, fileName)

            // Create a file output stream
            var fileOutputStream: FileOutputStream? = null
            try {
                // Create the file output stream
                fileOutputStream = FileOutputStream(file)

                // Compress the bitmap to JPEG format and write it to the file output stream
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)

                // Flush and close the file output stream
                fileOutputStream.flush()
                fileOutputStream.close()

                return file
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            } finally {
                // Close the file output stream in case of an exception
                fileOutputStream?.close()
            }
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
    }
}
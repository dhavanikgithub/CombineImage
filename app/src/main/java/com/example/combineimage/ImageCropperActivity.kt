package com.example.combineimage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.combineimage.databinding.ActivityImageCropperBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.*

class ImageCropperActivity : AppCompatActivity() {
    private lateinit var binding:ActivityImageCropperBinding
    private val tag = "ImageCropperActivity"
    private lateinit var result:String
    private lateinit var fileUri:Uri

    private lateinit var cropActivityResultLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageCropperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        readIntent()
        val dest_uri = java.lang.StringBuilder(UUID.randomUUID().toString()).append(".jpg").toString()

        val options = UCrop.Options()
        options.setCompressionQuality(100)
        options.setFreeStyleCropEnabled(true)
        options.setShowCropGrid(true)
        options.withAspectRatio(0F,0F)

        val ucropIntent = UCrop.of(fileUri, Uri.fromFile(File(cacheDir, dest_uri)))
            .withOptions(options)
            .getIntent(this)

        // Initialize the ActivityResultLauncher
        cropActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                handleCropResult(result.resultCode, result.data)
            }

        // Start the UCrop activity using the ActivityResultLauncher
        cropActivityResultLauncher.launch(ucropIntent)

    }

    private fun handleCropResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val uri = UCrop.getOutput(data)
            val intent = Intent()
            intent.putExtra("RESULT", uri.toString())
            setResult(RESULT_OK, intent)
            finish()
        } else {
            val intent = Intent()
            setResult(RESULT_CANCELED)
            finish()
        }
        finish()
    }

    private fun readIntent()
    {
        if(intent.extras!=null)
        {
            result= intent.getStringExtra("DATA")!!
            fileUri = Uri.parse(result)
        }
    }



}
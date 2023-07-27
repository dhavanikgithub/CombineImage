package com.example.combineimage

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.combineimage.databinding.ActivityImageCropperBinding
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import java.io.File
import java.util.UUID

class ImageCropperActivity : AppCompatActivity() {
    private lateinit var binding:ActivityImageCropperBinding
    private val tag = "ImageCropperActivity"
    private lateinit var result:String
    private lateinit var fileUri:Uri

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

        UCrop.of(fileUri,Uri.fromFile(File(cacheDir,dest_uri)))
            .withOptions(options)
            .start(this)


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == RESULT_OK && requestCode==UCrop.REQUEST_CROP) {
            val uri = UCrop.getOutput(data!!)
            val intent = Intent()
            intent.putExtra("RESULT",uri.toString())
            setResult(-1,intent)
            finish()
        }
        else{
            val intent = Intent()
            setResult(-2,intent)
            finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
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
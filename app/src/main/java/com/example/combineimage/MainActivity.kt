package com.example.combineimage

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.combineimage.model.ImageData
import com.example.combineimage.utils.CustomProgressDialog
import com.example.combineimage.utils.Utility
import com.example.combineimage.utils.Utility.Companion.addBorderToImage
import com.example.combineimage.utils.Utility.Companion.getCurrentDateTime
import com.example.combineimage.utils.Utility.Companion.saveBitmapAsJpeg
import com.example.combineimage.utils.Utility.Companion.showToast
import com.example.combineimage.utils.Utility.Companion.uriToBitmap
import com.example.combineimage.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    private lateinit var binding: ActivityMainBinding

    private var resultImage:Bitmap?=null
    companion object {
        var selectedResizedImage:String?=null
        var adapter:ArrayAdapter<String>?=null
    }
    private var selectedImages: MutableList<ImageData> = mutableListOf()
    private var customProgressDialog: CustomProgressDialog?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(customProgressDialog==null)
        {
            customProgressDialog=CustomProgressDialog(this,this)
        }

        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)

        binding.btnPicImage.setOnClickListener {

            openImagePicker()
        }

        binding.switch1.setOnCheckedChangeListener { buttonView, isChecked ->
            combineImages()
        }

        binding.spaceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                combineImages()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Called when the user starts interacting with the SeekBar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Called when the user stops interacting with the SeekBar
                // Use the 'progress' variable to access the final value set by the user
            }
        })

        binding.borderSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                combineImages()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Called when the user starts interacting with the SeekBar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Called when the user stops interacting with the SeekBar
                // Use the 'progress' variable to access the final value set by the user
            }
        })

        binding.btnSave.setOnClickListener {
            if(resultImage!=null){
                if(saveBitmapAsJpeg(resultImage!!,getCurrentDateTime()+".jpg"))
                {
                    showToast(this,"Image Saved")
                }
                else{
                    showToast(this,"Saved Failed")
                }
            }
            else{
                showToast(this,"Nothing Image Found")
            }

        }

        binding.bottomNavigation.setOnItemSelectedListener(this)

        val items = ArrayList<String>()
        items.add(getString(R.string.none))

        adapter = ArrayAdapter(
            this,
            R.layout.simple_spinner_item,
            items
        )
        binding.drdResizeImage.setText(getString(R.string.none))
        adapter!!.setDropDownViewResource(R.layout.simple_spinner_item)
        binding.drdResizeImage.setAdapter(adapter)
        selectedResizedImage = getString(R.string.none)

        binding.drdResizeImage.setOnItemClickListener { parent, view, position, id ->
            selectedResizedImage = parent.getItemAtPosition(position).toString()
            combineImages()
        }

    }


    private fun combineImages() {
        if (selectedImages.size >= 2) {
            val bitmapList = mutableListOf<Bitmap>()

            for (i in 0 until selectedImages.size) {
                var bitmap = selectedImages[i].bitmap
                if(selectedResizedImage!=getString(R.string.none))
                {
                    val item = selectedResizedImage!!.toInt()-1
                    if(i!=item)
                    {
                        val newWidth = selectedImages[item].bitmap.width
                        val newHeight = selectedImages[item].bitmap.height
                        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
                    }
                }
                bitmapList.add(addBorderToImage(bitmap,binding.borderSeekBar.progress,Color.RED))
            }

            resultImage = if(!binding.switch1.isChecked) {
                Utility.combineImages(bitmapList,binding.spaceSeekBar.progress,true)
            } else{
                Utility.combineImages(bitmapList,binding.spaceSeekBar.progress,false)
            }
            if(binding.switch1.isChecked){
                binding.combinedImageView2.visibility=View.VISIBLE
                binding.ivCombine2.setImageBitmap(resultImage)
                binding.ivCombine.setImageBitmap(null)
            }
            else{
                binding.combinedImageView2.visibility=View.GONE
                binding.ivCombine.setImageBitmap(resultImage)
                binding.ivCombine2.setImageBitmap(null)
            }
        }
    }


    private val someActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Handle the result of the activity here
        if (result.resultCode == RESULT_OK) {
            MainScope().launch(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.Main)
                    {
                        customProgressDialog?.start(getString(R.string.LoadingText))
                    }
                    adapter?.clear()
                    adapter?.add("None")
                    if (result.data?.clipData != null) {
                        val clipData = result.data!!.clipData
                        for (i in 0 until clipData!!.itemCount) {
                            val imageUri = clipData.getItemAt(i).uri
                            selectedImages.add(ImageData(imageUri, uriToBitmap(contentResolver,imageUri)))
                        }
                    } else if (result.data?.data != null) {
                        val imageUri = result.data!!.data
                        if (imageUri != null) {
                            selectedImages.add(ImageData(imageUri, uriToBitmap(contentResolver,imageUri)))
                        }
                    }
                    var itemCount = 1
                    selectedImages.forEach { item ->
                        adapter!!.add("$itemCount")
                        itemCount++
                    }
                    withContext(Dispatchers.Main)
                    {
                        adapter!!.notifyDataSetChanged()
                    }
                    combineImages()
                    withContext(Dispatchers.Main)
                    {
                        customProgressDialog?.stop()
                    }
                }
                catch (ex:java.lang.Exception)
                {
                    withContext(Dispatchers.Main)
                    {
                        customProgressDialog?.stop()
                    }
                }

            }
        }
    }
    private fun openImagePicker() {
        selectedImages.clear()
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        someActivityResultLauncher.launch(intent)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.reduce -> {
                val intent = Intent(this, ReduceActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }
        }
        return false
    }
}


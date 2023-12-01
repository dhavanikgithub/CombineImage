package com.example.combineimage


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.combineimage.CombineImageActivity.Companion.REQUEST_CODE_CREATE_DIR
import com.example.combineimage.databinding.ActivityMainBinding
import com.example.combineimage.model.ImageData
import com.example.combineimage.utils.CustomProgressDialog
import com.example.combineimage.utils.Utility
import com.example.combineimage.utils.Utility.Companion.addBorderToImage
import com.example.combineimage.utils.Utility.Companion.getCurrentDateTime
import com.example.combineimage.utils.Utility.Companion.isDarkTheme
import com.example.combineimage.utils.Utility.Companion.resizeBitmap
import com.example.combineimage.utils.Utility.Companion.saveBitmapAsJpeg
import com.example.combineimage.utils.Utility.Companion.showToast
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.github.dhaval2404.colorpicker.model.ColorSwatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tankery.lib.circularseekbar.CircularSeekBar
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val tag = "MainActivity"
    private var resultImage:Bitmap?=null
    private var selectedImages: MutableList<ImageData> = mutableListOf()
    private var customProgressDialog: CustomProgressDialog?=null
    private var mDefaultColor = R.color.red
    private var isVerticalAlign:Boolean=true
    private lateinit var appFolder:String
    companion object {
        var selectedResizedImageWidth:String = "None"
        var selectedResizedImageHeight:String = "None"
        var adapter:ArrayAdapter<String>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageAlignCheck()

        binding.alignHorizontal.setOnClickListener {
            isVerticalAlign=false
            imageAlignCheck()
            combineImages()
        }
        binding.alignVertical.setOnClickListener {
            isVerticalAlign=true
            imageAlignCheck()
            combineImages()
        }
        if(!File("/storage/emulated/0/Pictures/CI_Pictures").exists())
        {
            createDirectoryInInternalStorage()
        }
        else{
            appFolder="/storage/emulated/0/Pictures/CI_Pictures"
        }

        /*binding.bottomNavigation.menu.setGroupCheckable(0, true, true)*/
        /*binding.bottomNavigation.setOnItemSelectedListener(this)*/

        val receivedImageList = CombineImageActivity.selectedImages

        if(customProgressDialog==null) {
            customProgressDialog=CustomProgressDialog(this,this)
        }

        customProgressDialog?.start("Combine in process.....")
        MainScope().launch(Dispatchers.IO)
        {
            try {
                if(receivedImageList.size>0) {
                    adapter?.clear()
                    adapter?.add(getString(R.string.None))
                    MainScope().launch(Dispatchers.IO) {
                        val items = ArrayList<String>()
                        items.add(getString(R.string.None))
                        for(i in 0 until receivedImageList.size)
                        {
                            val uri = receivedImageList[i].uri
                            selectedImages.add(
                                ImageData(
                                    uri,
                                    resizeBitmap(
                                        receivedImageList[i].bitmap,
                                        receivedImageList[i].bitmap.width/2,
                                        receivedImageList[i].bitmap.height/2
                                    )
                                )
                            )
                            items.add((i+1).toString())
                        }
                        withContext(Dispatchers.Main)
                        {
                            adapter = ArrayAdapter(
                                this@MainActivity,
                                R.layout.simple_spinner_item,
                                items
                            )
                            binding.drdSameWidth.setText(getString(R.string.None))
                            binding.drdSameHeight.setText(getString(R.string.None))
                            adapter?.setDropDownViewResource(R.layout.simple_spinner_item)
                            binding.drdSameWidth.setAdapter(adapter)
                            binding.drdSameHeight.setAdapter(adapter)
                            combineImages()
                        }
                    }
                }
                withContext(Dispatchers.Main)
                {
                    customProgressDialog?.stop()
                }
            }
            catch (ex:Exception)
            {
                withContext(Dispatchers.Main)
                {
                    customProgressDialog?.stop()
                }
            }

        }


        /*binding.btnPicImage.setOnClickListener {

            openImagePicker()
        }*/

        val colorCodes = resources.getStringArray(R.array.colors_code)
        binding.btnColorPic.setOnClickListener {

            MaterialColorPickerDialog
                .Builder(this)
                .setTitle("Pick Color")
                .setColors(colorCodes)
                .setColorShape(ColorShape.SQAURE)
                .setColorSwatch(ColorSwatch._300)
                .setDefaultColor(mDefaultColor)
                .setColorListener { color, colorHex ->
                    // Handle Color Selection
                    mDefaultColor = color
                    val shapeDrawable = ShapeDrawable(OvalShape())
                    shapeDrawable.paint.color = color
                    binding.btnColorPic.background = shapeDrawable

                    binding.borderCircularSeekBar.pointerColor=color
                    binding.borderCircularSeekBar.circleProgressColor=color
                    combineImages()
                }
                .show()
        }


        binding.spaceCircularSeekBar.setOnSeekBarChangeListener(object : CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(circularSeekBar: CircularSeekBar?, progress: Float, fromUser: Boolean) {
                combineImages()
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {

            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {

            }
        })

        binding.borderCircularSeekBar.setOnSeekBarChangeListener(object : CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(circularSeekBar: CircularSeekBar?, progress: Float, fromUser: Boolean) {
                combineImages()
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {

            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {

            }
        })

        binding.btnSave.setOnClickListener {
            if(resultImage!=null){
                if(!File("/storage/emulated/0/Pictures/CI_Pictures").exists())
                {
                    createDirectoryInInternalStorage()
                }
                if(saveBitmapAsJpeg(resultImage!!,"${appFolder}/CI_IMG_${getCurrentDateTime()}.jpg"))
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

        binding.drdSameWidth.setOnItemClickListener { parent, view, position, id ->
            selectedResizedImageWidth = parent.getItemAtPosition(position).toString()
            combineImages()
        }
        binding.drdSameHeight.setOnItemClickListener { parent, view, position, id ->
            selectedResizedImageHeight = parent.getItemAtPosition(position).toString()
            combineImages()
        }

    }


    private fun imageAlignCheck()
    {
        val isDarkTheme = isDarkTheme(this@MainActivity)
        val activeTextColor = Color.parseColor("#3592C4")
        val normalTextColor = if(isDarkTheme) {
            Color.parseColor("#AFAFAF")
        } else{
            Color.BLACK
        }
        if(isVerticalAlign)
        {
            val borderDrawable = ContextCompat.getDrawable(this, R.drawable.border)
            binding.alignHorizontal.background=borderDrawable
            binding.alignHorizontalText.setTextColor(normalTextColor)
            binding.alignHorizontalImage.setImageDrawable(getDrawable(R.drawable.ic_vertical_view))

            val activeBorderDrawable = ContextCompat.getDrawable(this, R.drawable.active_border)
            binding.alignVertical.background=activeBorderDrawable
            binding.alignVerticalText.setTextColor(activeTextColor)
            binding.alignVerticalImage.setImageDrawable(getDrawable(R.drawable.ic_active_vertical_view))
        }
        else{
            val borderDrawable = ContextCompat.getDrawable(this, R.drawable.border)
            binding.alignVertical.background=borderDrawable
            binding.alignVerticalText.setTextColor(normalTextColor)
            binding.alignVerticalImage.setImageDrawable(getDrawable(R.drawable.ic_vertical_view))

            val activeBorderDrawable = ContextCompat.getDrawable(this, R.drawable.active_border)
            binding.alignHorizontal.background=activeBorderDrawable
            binding.alignHorizontalText.setTextColor(activeTextColor)
            binding.alignHorizontalImage.setImageDrawable(getDrawable(R.drawable.ic_active_vertical_view))
        }
    }

    private fun combineImages() {
        if (selectedImages.size >= 2) {
            val bitmapList = mutableListOf<Bitmap>()
            
            for (i in 0 until selectedImages.size) {
                var bitmap = selectedImages[i].bitmap
                if(selectedResizedImageWidth!=getString(R.string.None))
                {
                    val item = selectedResizedImageWidth.toInt()-1
                    if(i!=item)
                    {
                        val newWidth = selectedImages[item].bitmap.width
                        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, bitmap.height, false)
                    }
                }
                if(selectedResizedImageHeight!=getString(R.string.None))
                {
                    val item = selectedResizedImageHeight.toInt()-1
                    if(i!=item)
                    {
                        val newHeight = selectedImages[item].bitmap.height
                        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, newHeight, false)
                    }
                }
                bitmapList.add(addBorderToImage(bitmap,binding.borderCircularSeekBar.progress.toInt(),mDefaultColor))
            }
            resultImage = Utility.combineImages(bitmapList,binding.spaceCircularSeekBar.progress.toInt(),isVerticalAlign)

            if(!isVerticalAlign){
                binding.combinedImageView2.visibility=View.VISIBLE
                /*binding.ivCombine2.setImageBitmap(resultImage)*/

                Glide.with(this)
                    .asBitmap()
                    .load(resultImage) // Pass the Bitmap you want to load
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            // Bitmap is loaded successfully, use 'resource' here
                            binding.ivCombine2.setImageBitmap(resource) // Display the Bitmap in an ImageView
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            // Handle failure to load the Bitmap
                        }
                    })
                binding.ivCombine.setImageBitmap(null)
            }
            else{
                binding.combinedImageView2.visibility=View.GONE
                /*binding.ivCombine.setImageBitmap(resultImage)*/
                Glide.with(this)
                    .asBitmap()
                    .load(resultImage) // Pass the Bitmap you want to load
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            // Bitmap is loaded successfully, use 'resource' here
                            binding.ivCombine.setImageBitmap(resource) // Display the Bitmap in an ImageView
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            // Handle failure to load the Bitmap
                        }
                    })
                binding.ivCombine2.setImageBitmap(null)
            }
        }
    }


    /*private val someActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Handle the result of the activity here
        if (result.resultCode == RESULT_OK) {
            MainScope().launch(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.Main)
                    {
                        customProgressDialog?.start(getString(R.string.LoadingText))
                    }
                    adapter?.clear()
                    adapter?.add(getString(R.string.None))
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
    }*/



//    private fun openImagePicker() {
//        selectedImages.clear()
//        val intent = Intent(Intent.ACTION_GET_CONTENT)
//        intent.type = "image/*"
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        someActivityResultLauncher.launch(intent)
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                try{
                    if(resultImage!=null){
                        val fileName = "CI_IMG_${getCurrentDateTime()}.jpg"
                        val file = File("$appFolder/$fileName")
                        if(file.exists())
                        {
                            Utility.triggerMediaScan(this, file)
                            showToast(this,"File already saved")
                            return false
                        }
                        if(saveBitmapAsJpeg(resultImage!!,file.absolutePath))
                        {
                            Utility.triggerMediaScan(this, file)
                            showToast(this,"File saved")
                        }
                        else{
                            showToast(this,"Failed to save")
                        }
                    }
                    else{
                        showToast(this,"Nothing Image Found")
                    }

                }
                catch (ex:Exception)
                {
                    throw ex
                    showToast(this,"Failed to save")
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun createDirectoryInInternalStorage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE_CREATE_DIR)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CREATE_DIR && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val dirName = "CI_Pictures"
                val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
                val dirUri = DocumentsContract.createDocument(
                    contentResolver,
                    parentDocumentUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    dirName
                )
                if (dirUri != null) {
                    // Directory created successfully
                    appFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath+"/CI_Pictures"
                    Log.i(tag,"Created Directory Path: $appFolder")
                } else {
                    // Failed to create the directory
                    Log.i(tag,"Failed to create the directory.")
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getDirectoryPathFromUri(uri: Uri): String? {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val documentId = it.getString(0)
                return "${Environment.getExternalStorageDirectory()}/$documentId"
            }
        }
        return null
    }

}

package com.example.combineimage


import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.combineimage.databinding.ActivityMainBinding
import com.example.combineimage.model.ImageData
import com.example.combineimage.utils.CustomProgressDialog
import com.example.combineimage.utils.Utility
import com.example.combineimage.utils.Utility.Companion.addBorderToImage
import com.example.combineimage.utils.Utility.Companion.getCurrentDateTime
import com.example.combineimage.utils.Utility.Companion.getDownloadFolderPath
import com.example.combineimage.utils.Utility.Companion.saveBitmapAsJpeg
import com.example.combineimage.utils.Utility.Companion.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tankery.lib.circularseekbar.CircularSeekBar
import top.defaults.colorpicker.ColorPickerPopup
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val tag = "MainActivity"
    private var resultImage:Bitmap?=null
    private var selectedImages: MutableList<ImageData> = mutableListOf()
    private var customProgressDialog: CustomProgressDialog?=null
    private var mDefaultColor = Color.RED
    private var isVerticalAlign:Boolean=true
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

        /*binding.bottomNavigation.menu.setGroupCheckable(0, true, true)*/
        /*binding.bottomNavigation.setOnItemSelectedListener(this)*/

        val receivedImageList = CombineImageActivity.selectedImages

        if(customProgressDialog==null) {
            customProgressDialog=CustomProgressDialog(this,this)
        }

        if(receivedImageList.size>0) {
            adapter?.clear()
            adapter?.add("None")
            MainScope().launch(Dispatchers.IO) {
                val items = ArrayList<String>()
                items.add("None")
                for(i in 0 until receivedImageList.size)
                {
                    val file = receivedImageList[i].file
                    val uri = receivedImageList[i].uri
                    selectedImages.add(ImageData(uri, receivedImageList[i].bitmap))
                    items.add((i+1).toString())
                }
                withContext(Dispatchers.Main)
                {
                    adapter = ArrayAdapter(
                        this@MainActivity,
                        R.layout.simple_spinner_item,
                        items
                    )
                    binding.drdSameWidth.setText("None")
                    binding.drdSameHeight.setText("None")
                    adapter!!.setDropDownViewResource(R.layout.simple_spinner_item)
                    binding.drdSameWidth.setAdapter(adapter)
                    binding.drdSameHeight.setAdapter(adapter)
                    combineImages()
                }
            }
        }

        /*binding.btnPicImage.setOnClickListener {

            openImagePicker()
        }*/

        binding.btnColorPic.setOnClickListener {
            ColorPickerPopup
                .Builder(this@MainActivity)
                .initialColor(Color.RED) // set initial color of the color picker dialog
                .enableBrightness(true) // enable color brightness slider or not
                .enableAlpha(true) // enable color alpha changer on slider or not
                .okTitle("Choose") // this is top right Choose button
                .cancelTitle("Cancel") // this is top left Cancel button which closes the
                .showIndicator(true) // this is the small box which shows the chosen color by user at the bottom of the cancel\ button
                .showValue(true) // this is the value which shows the selected color hex code the above all values can be made false to disable them on the color picker dialog.
                .build()
                .show(it, object : ColorPickerPopup.ColorPickerObserver()
                {
                        override fun onColorPicked(color: Int) {
                            mDefaultColor = color
                            val shapeDrawable = ShapeDrawable(OvalShape())
                            shapeDrawable.paint.color = color
                            binding.btnColorPic.background = shapeDrawable

                            binding.borderCircularSeekBar.pointerColor=color
                            binding.borderCircularSeekBar.circleProgressColor=color
                            combineImages()
                        }
                })
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

        /*binding.spaceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        })*/

        /*binding.borderSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        })*/

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

        binding.drdSameWidth.setOnItemClickListener { parent, view, position, id ->
            selectedResizedImageWidth = parent.getItemAtPosition(position).toString()
            combineImages()
        }
        binding.drdSameHeight.setOnItemClickListener { parent, view, position, id ->
            selectedResizedImageHeight = parent.getItemAtPosition(position).toString()
            combineImages()
        }

    }

    private fun isDarkTheme(context: Context): Boolean {
        return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
    private fun imageAlignCheck()
    {
        val isDarkTheme = isDarkTheme(this@MainActivity)
        var normalTextColor = Color.BLACK
        var activeTextColor = Color.parseColor("#3592C4")
        if(isDarkTheme)
        {
            normalTextColor = Color.parseColor("#AFAFAF")
        }
        else{
            normalTextColor = Color.BLACK
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
                if(selectedResizedImageWidth!="None")
                {
                    val item = selectedResizedImageWidth.toInt()-1
                    if(i!=item)
                    {
                        val newWidth = selectedImages[item].bitmap.width
                        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, bitmap.height, false)
                    }
                }
                if(selectedResizedImageHeight!="None")
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
    }*/

    /*override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.reduce -> {
                val intent = Intent(this, ReduceActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return false
    }*/


//    private fun openImagePicker() {
//        selectedImages.clear()
//        val intent = Intent(Intent.ACTION_GET_CONTENT)
//        intent.type = "image/*"
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        someActivityResultLauncher.launch(intent)
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.example.combineimage.R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            com.example.combineimage.R.id.action_save -> {
                try{
                    if(resultImage!=null){
                        val fileName = getCurrentDateTime()+".jpg"
                        val file = File(getDownloadFolderPath()+"/"+fileName)
                        if(file.exists())
                        {
                            showToast(this,"File already saved")
                            return false
                        }
                        if(saveBitmapAsJpeg(resultImage!!,fileName))
                        {
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
                    showToast(this,"Failed to save")
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}

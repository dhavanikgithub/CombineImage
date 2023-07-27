package com.example.combineimage


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.combineimage.databinding.ActivityReduceBinding
import com.example.combineimage.model.ImageProperty
import com.example.combineimage.utils.CustomProgressDialog
import com.example.combineimage.utils.Utility.Companion.convertToBytes
import com.example.combineimage.utils.Utility.Companion.copyFile
import com.example.combineimage.utils.Utility.Companion.filePathToBitmap
import com.example.combineimage.utils.Utility.Companion.formatFileSize
import com.example.combineimage.utils.Utility.Companion.getDownloadFolderPath
import com.example.combineimage.utils.Utility.Companion.getFileSize
import com.example.combineimage.utils.Utility.Companion.showToast
import com.google.android.material.navigation.NavigationBarView
import com.nareshchocha.filepickerlibrary.models.*
import com.nareshchocha.filepickerlibrary.ui.FilePicker
import com.nareshchocha.filepickerlibrary.utilities.appConst.Const
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class ReduceActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    private lateinit var binding: ActivityReduceBinding
    private var selectedImages: MutableList<ImageProperty> = mutableListOf()
    private var customProgressDialog: CustomProgressDialog?=null
    private var compressedFile:File?=null

    companion object {
        var selectedMeasure:String?=null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReduceBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if(customProgressDialog==null)
        {
            customProgressDialog=CustomProgressDialog(this,this)
        }
        binding.txtOSize.text=""
        binding.txtCSize.text=""
        binding.txtCWidthHeight.text=""
        binding.txtOWidthHeight.text=""

        binding.bottomNavigation.menu.setGroupCheckable(1, true, true)

        for (i in 0 until binding.bottomNavigation.menu.size()) {
            val item = binding.bottomNavigation.menu.getItem(i)
            item.isChecked = false
        }

        binding.btnBrowse.setOnClickListener {
            openImagePicker()
        }

        binding.drdSizeMeasure.setOnItemClickListener { parent, view, position, id ->
            selectedMeasure = parent.getItemAtPosition(position).toString()
        }

        binding.btnProcess.setOnClickListener {

            MainScope().launch(Dispatchers.IO) {
                try {
                    if(binding.txtInput.text.toString()!="" && selectedImages.size==1)
                    {
                        withContext(Dispatchers.Main)
                        {
                            customProgressDialog?.start(getString(R.string.LoadingText))
                        }
                        /*val outputPath=Utility.getDownloadFolderPath()+"/"+Utility.getCurrentDateTime()+".jpg"*/
                        val imgSize = binding.txtInput.text.toString().toLong()
                        if(selectedMeasure==null)
                        {
                            return@launch
                        }
                        val imgSizeFormat = selectedMeasure
                        val sizeBytes = convertToBytes(imgSize, imgSizeFormat!!)
                        var quality = 100
                        /*reduceImageSize(selectedImages[0].filePath, outputPath,sizeBytes)*/
                        compressedFile = Compressor.compress(this@ReduceActivity, File(selectedImages[0].filePath)) {
                            quality(quality)
                            resolution(selectedImages[0].width, selectedImages[0].height)
                            size(sizeBytes)
                        }
                        while (compressedFile!!.length()>sizeBytes)
                        {
                            quality--
                            if(quality==0)
                            {
                                break
                            }
                            compressedFile = Compressor.compress(this@ReduceActivity, File(selectedImages[0].filePath)) {
                                quality(quality)
                                resolution(selectedImages[0].width, selectedImages[0].height)
                                size(sizeBytes)
                            }
                        }
                        val compressBitmap = filePathToBitmap(compressedFile!!.absolutePath)
                        val compressBitmapWidth = compressBitmap.width
                        val compressBitmapHeight = compressBitmap.height
                        val compressSize = formatFileSize(compressedFile!!.length())


                        withContext(Dispatchers.Main)
                        {
                            binding.txtCSize.text= getString(R.string.printSize,compressSize)
                            Log.i("ReduceActivity","Compress File Size: $compressSize")
                            binding.txtCWidthHeight.text=getString(R.string.printDimension,compressBitmapWidth,compressBitmapHeight)
                            Log.i("ReduceActivity","Compress File Dimension: $compressBitmapWidth X $compressBitmapHeight")
                            Log.i("ReduceActivity","Compress File Path: ${compressedFile!!.absolutePath}")
                            Glide.with(this@ReduceActivity)
                                .load(compressedFile!!.absolutePath)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(binding.ivCompressed)
                            customProgressDialog?.stop()
                        }
                    }
                }
                catch (ex: Exception)
                {
                    withContext(Dispatchers.Main)
                    {
                        customProgressDialog?.stop()
                    }
                }
            }

        }

        binding.bottomNavigation.setOnItemSelectedListener(this)

        val items = ArrayList<String>()
        items.add(resources.getStringArray(R.array.spinner_items)[0])
        items.add(resources.getStringArray(R.array.spinner_items)[1])

        val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, items)
        binding.drdSizeMeasure.setText(resources.getStringArray(R.array.spinner_items)[0])
        adapter.setDropDownViewResource(R.layout.simple_spinner_item)
        binding.drdSizeMeasure.setAdapter(adapter)
        selectedMeasure=resources.getStringArray(R.array.spinner_items)[0]

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                try{
                    val file = File(getDownloadFolderPath()+"/"+compressedFile!!.name)
                    if(file.exists())
                    {
                        showToast(this,"File already saved")
                        return false
                    }
                    copyFile(compressedFile!!, file)
                    showToast(this,"File saved")
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.combine -> {
                val intent = Intent(this, CombineImageActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }
        }
        return false
    }

    private fun openImagePicker() {
        val intent = FilePicker.Builder(this)
            .addPickMedia(
                PickMediaConfig(
                    popUpIcon = R.drawable.ic_photo,// DrawableRes Id
                    popUpText = "Image",
                    allowMultiple = false,// set Multiple pick file
                    maxFiles = 1,// max files working only in android latest version
                    mPickMediaType = PickMediaType.ImageOnly,
                    askPermissionTitle = null, // set Permission ask Title
                    askPermissionMessage = null,// set Permission ask Message
                    settingPermissionTitle = null,// set Permission setting Title
                    settingPermissionMessage = null,// set Permission setting Message
                ),
            )
            .setPopUpConfig(
                PopUpConfig(
                    chooserTitle = "Choose Image",
                    mPopUpType = PopUpType.BOTTOM_SHEET,// PopUpType.BOTTOM_SHEET Or PopUpType.DIALOG
                    mOrientation = RecyclerView.HORIZONTAL // RecyclerView.VERTICAL or RecyclerView.HORIZONTAL
                )
            )
            .build()
        captureImageResultLauncher.launch(intent)

    }

    private val  captureImageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            MainScope().launch(Dispatchers.IO) {
                try{
                    val filePath= it.data!!.getStringExtra(Const.BundleExtras.FILE_PATH)
                    Log.i("ReduceActivity","Selected Image Path: $filePath")
                    withContext(Dispatchers.Main)
                    {
                        selectedImages.clear()
                        binding.ivOriginal.setImageBitmap(null)
                        binding.ivCompressed.setImageBitmap(null)
                        binding.txtOSize.text=""
                        binding.txtCSize.text=""
                        binding.txtCWidthHeight.text=""
                        binding.txtOWidthHeight.text=""
                    }
                    val bitmap = filePathToBitmap(filePath!!)
                    /*binding.ivOriginal.setImageBitmap(bitmap)*/
                    selectedImages.add(ImageProperty(filePath,bitmap.width,bitmap.height))
                    val sizeInBytes:Long = getFileSize(filePath)
                    val height = bitmap.height
                    val width = bitmap.width
                    withContext(Dispatchers.Main)
                    {
                        Glide.with(this@ReduceActivity)
                            .load(filePath)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(binding.ivOriginal)
                        binding.txtOSize.text=getString(R.string.printSize,formatFileSize(sizeInBytes))
                        binding.txtOWidthHeight.text=getString(R.string.printDimension,width,height)
                    }
                }
                catch (ex:java.lang.Exception)
                {
                    Log.i("ReduceActivity","Debug1")
                    ex.printStackTrace()
                }
            }
        }
    }
}
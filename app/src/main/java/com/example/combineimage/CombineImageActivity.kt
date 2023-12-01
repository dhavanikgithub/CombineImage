package com.example.combineimage

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.combineimage.adapter.MyAdapter
import com.example.combineimage.databinding.ActivityCombineImageBinding
import com.example.combineimage.model.ItemsDataClass
import com.example.combineimage.utils.Utility
import com.example.combineimage.utils.Utility.Companion.filePathToBitmap
import com.example.combineimage.utils.Utility.Companion.getInputStreamFromUri
import com.example.combineimage.utils.Utility.Companion.getRealPath
import com.example.combineimage.utils.Utility.Companion.isDarkTheme
import com.example.combineimage.utils.Utility.Companion.saveInputStreamToFile
import com.example.combineimage.utils.Utility.Companion.showSnackbar
import com.example.combineimage.utils.Utility.Companion.uriToBitmap
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.Snackbar
import com.sangcomz.fishbun.FishBun
import com.sangcomz.fishbun.adapter.image.impl.GlideAdapter
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*


class CombineImageActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener,
    MyAdapter.ItemClickListener {
    private lateinit var binding: ActivityCombineImageBinding
    private lateinit var myAdapter: MyAdapter
    private val tag = "CombineImageActivity"
    private var clickedItemPosition:Int=-1
    private lateinit var clickedItemBitmap:Bitmap
    private lateinit var appFolder:String


    companion object
    {
        var selectedImages = ArrayList<ItemsDataClass>()
        val REQUEST_CODE_CREATE_DIR = 100
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCombineImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnClearAll.imageTintList = ContextCompat.getColorStateList(this, R.color.white)
        val isDarkTheme = isDarkTheme(this@CombineImageActivity)
        binding.btnClearAll.setPadding(30)
        binding.btnClearAll.elevation=20F
        if(isDarkTheme)
        {
            val solidColorDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(getColor(R.color.fab_dark_color))
            }
            binding.btnClearAll.background=solidColorDrawable

        }
        else{
            val solidColorDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(getColor(R.color.fab_light_color))
            }
            binding.btnClearAll.background=solidColorDrawable
        }
        selectedImages.clear()

        if(!File("/storage/emulated/0/Pictures/CI_Pictures").exists())
        {
            createDirectoryInInternalStorage()
        }
        else{
            appFolder="/storage/emulated/0/Pictures/CI_Pictures"
        }

        binding.btnBrowse.setOnClickListener {
            openImagePicker()
        }

        binding.btnCombine.setOnClickListener {
            if(selectedImages.size>1)
            {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            else{
                showSnackbar(binding.root,"At least 2 or more images required")
            }
        }
        myAdapter = MyAdapter(this@CombineImageActivity,selectedImages,this@CombineImageActivity)
        binding.rvCombineImage.setHasFixedSize(true)
        binding.rvCombineImage.adapter = myAdapter

        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)

        binding.bottomNavigation.setOnItemSelectedListener(this)

        binding.btnClearAll.setOnClickListener {
            if(selectedImages.size>0)
            {
                selectedImages.clear()
                myAdapter.notifyDataSetChanged()
            }

        }

        var deletedItem: ItemsDataClass?

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT){
            override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val sourcePosition = source.absoluteAdapterPosition
                val targetPosition = target.absoluteAdapterPosition
                Collections.swap(selectedImages,sourcePosition,targetPosition)
                myAdapter.swapItems(sourcePosition,targetPosition)
                Log.i(tag,"Swapped item from selectedImages[$sourcePosition] to selectedImages[$targetPosition]")
                return true

            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                when(direction)
                {
                    ItemTouchHelper.LEFT ->{
                        deletedItem = selectedImages[position]
                        selectedImages.removeAt(position)
                        myAdapter.notifyItemRemoved(position)
                        val rootView: View = findViewById(android.R.id.content)
                        Snackbar.make(this@CombineImageActivity,rootView,deletedItem!!.filename,Snackbar.LENGTH_LONG).setAction("Undo"
                        ) {
                            selectedImages.add(position, deletedItem!!)
                            myAdapter.notifyItemInserted(position)
                        }.show()
                    }
                }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(Color.parseColor("#C42B1C"))
                    .addSwipeLeftActionIcon(R.drawable.ic_delete_sweep)
                    .create()
                    .decorate()
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

        })
        itemTouchHelper.attachToRecyclerView(binding.rvCombineImage)


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

    private fun openImagePicker() {
        val isdark = isDarkTheme(this)
        var backgroundColor = Color.parseColor("#FF03A9F4")
        var textColor = Color.parseColor("#FFFFFFFF")
        if(isdark)
        {
            backgroundColor = Color.parseColor("#FFDC143C")
        }
        FishBun.with(this)
            .setImageAdapter(GlideAdapter())
            .setMinCount(1)
            .setActionBarTitle("Select Images")
            .setActionBarColor(backgroundColor,backgroundColor)
            .setActionBarTitleColor(textColor)
            .setIsShowCount(true)
            .setMaxCount(5)
            .setAllViewTitle("All")
            .setButtonInAlbumActivity(true)
            .exceptGif(false)
            .textOnImagesSelectionLimitReached("You can't select any more.")
            .textOnNothingSelected("Please Select the images")
            .setPickerCount(5)
            .hasCameraInPickerPage(true)
            .setSelectCircleStrokeColor(backgroundColor)
            .setDoneButtonDrawable(ContextCompat.getDrawable(this, R.drawable.ic_check))
            .startAlbumWithActivityResultCallback(openImagePickerActivityResultLauncher)

    }

    private val openImagePickerActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            MainScope().launch(Dispatchers.IO) {
                try{
                    val intent = result.data
                    val filePaths = intent!!.getParcelableArrayListExtra<Uri>("intent_path")
                    Log.i(tag,"File list size: ${filePaths!!.size}")
                    Log.i(tag, "File Path: ${getRealPath(filePaths[0],this@CombineImageActivity)}")

                    for (i in 0 until filePaths.size)
                    {
                        val realpath = getRealPath(filePaths[i],this@CombineImageActivity)
                        val file = File(realpath)
                        if(!isItemAlreadySelected(file.name))
                        {
                            val bitmap = filePathToBitmap(realpath)
                            val item = ItemsDataClass(file.name,bitmap,realpath,file, filePaths[i])
                            selectedImages.add(item)
                        }
                    }
                    withContext(Dispatchers.Main)
                    {
                        myAdapter.notifyDataSetChanged()
                    }
                }
                catch (exNull:NullPointerException)
                {
                    Log.e(tag,"Null Pointer Exception: ${exNull.message}")
                }
                catch (e:Exception)
                {
                    Log.e(tag,"Exception: ${e.message}")
                }

            }
        }
    }

    /*private val someActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Handle the result of the activity here
        if (result.resultCode == RESULT_OK) {
            MainScope().launch(Dispatchers.IO) {
                try{
                    val filePaths = result.data?.getStringArrayListExtra(Const.BundleExtras.FILE_PATH_LIST)
                    if(filePaths==null) {
                        val filePath = result.data?.getStringExtra(Const.BundleExtras.FILE_PATH)
                        val file = File(filePath!!)

                        if(!isItemAlreadySelected(file.name))
                        {
                            val bitmap = filePathToBitmap(filePath)
                            val item = ItemsDataClass(file.name,bitmap,filePath,file, Uri.fromFile(file))
                            selectedImages.add(item)

                        }
                        else{
                            showSnackbar(binding.root,"Image already selected")
                        }
                    }
                    else{
                        Log.i(tag,"File list size: ${filePaths!!.size}")
                        Log.i(tag, filePaths[0])

                        for (i in 0 until filePaths.size)
                        {
                            val file = File(filePaths[i])
                            if(!isItemAlreadySelected(file.name))
                            {
                                val bitmap = filePathToBitmap(filePaths[i])
                                val item = ItemsDataClass(file.name,bitmap,filePaths[i],file, Uri.fromFile(file))
                                selectedImages.add(item)
                            }
                        }
                    }
                    withContext(Dispatchers.Main)
                    {
                        myAdapter.notifyDataSetChanged()
                    }
                }
                catch (exNull:NullPointerException)
                {
                    Log.e(tag,"Null Pointer Exception: ${exNull.message}")
                }
                catch (e:Exception)
                {
                    Log.e(tag,"Exception: ${e.message}")
                }

            }
        }
    }*/

    private fun isItemAlreadySelected(filenameToCheck: String): Boolean {
        return selectedImages.any { it.filename == filenameToCheck }
    }
    override fun onDeleteItemClicked(position: Int) {
        val deletedItem:ItemsDataClass = selectedImages[position]
        selectedImages.removeAt(position)
        myAdapter.notifyItemRemoved(position)
        val rootView: View = findViewById(android.R.id.content)
        Snackbar.make(this@CombineImageActivity,rootView,deletedItem.filename,Snackbar.LENGTH_LONG)
            .setAction("Undo"
        ) {
            selectedImages.add(position, deletedItem)
            myAdapter.notifyItemInserted(position)
        }.show()
    }
    override fun onCropItemClicked(position: Int) {
        clickedItemPosition=position
        val intent = Intent(this,ImageCropperActivity::class.java)
        intent.putExtra("DATA", selectedImages[position].uri.toString())
        imageCropActivityResultLauncher.launch(intent)
    }

    private val imageCropActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Handle the result of the activity here
        if (result.resultCode == RESULT_OK) {
            val uri = Uri.parse(result.data!!.getStringExtra("RESULT")!!)
            Log.i(tag,uri.toString())
            clickedItemBitmap = uriToBitmap(contentResolver,uri)

            val cacheFolderPath: String = this.cacheDir.absolutePath
            val filePath = cacheFolderPath+"/"+ selectedImages[clickedItemPosition].filename
            val file = File(filePath)
            if(file.exists())
            {
                Utility.deleteFile(filePath)
            }
            val inputStream = getInputStreamFromUri(this,uri)
            saveInputStreamToFile(inputStream!!,filePath)
            selectedImages[clickedItemPosition].bitmap = clickedItemBitmap
            selectedImages[clickedItemPosition].uri = uri
            selectedImages[clickedItemPosition].filepath = filePath
            selectedImages[clickedItemPosition].file = file
            selectedImages[clickedItemPosition].filename = selectedImages[clickedItemPosition].filename
            myAdapter.notifyItemChanged(clickedItemPosition)
        }
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
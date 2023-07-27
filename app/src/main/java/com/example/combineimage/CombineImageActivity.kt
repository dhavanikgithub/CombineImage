package com.example.combineimage

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.combineimage.adapter.MyAdapter
import com.example.combineimage.databinding.ActivityCombineImageBinding
import com.example.combineimage.model.ItemsDataClass
import com.example.combineimage.utils.Utility
import com.example.combineimage.utils.Utility.Companion.filePathToBitmap
import com.example.combineimage.utils.Utility.Companion.uriToBitmap
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.Snackbar
import com.nareshchocha.filepickerlibrary.models.*
import com.nareshchocha.filepickerlibrary.ui.FilePicker
import com.nareshchocha.filepickerlibrary.utilities.appConst.Const
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*


class CombineImageActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener,
    MyAdapter.ItemClickListener {
    private lateinit var binding: ActivityCombineImageBinding
    private lateinit var myAdapter: MyAdapter
    private val tag = "CombineImageActivity"

    companion object
    {
        var selectedImages = ArrayList<ItemsDataClass>()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCombineImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                showSnackbar("At least 2 or more images required")
            }
        }
        myAdapter = MyAdapter(this@CombineImageActivity,selectedImages,this@CombineImageActivity)
        binding.rvCombineImage.setHasFixedSize(true)
        binding.rvCombineImage.adapter = myAdapter

        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)

        binding.bottomNavigation.setOnItemSelectedListener(this)

        var deletedItem:ItemsDataClass? = null

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT){
            override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val sourcePosition = source.adapterPosition
                val targetPosition = target.adapterPosition
                Collections.swap(selectedImages,sourcePosition,targetPosition)
                myAdapter.swapItems(sourcePosition,targetPosition)
                Log.i(tag,"Swapped item from selectedImages[$sourcePosition] to selectedImages[$targetPosition]")
                return true

            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
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

    private fun openImagePicker() {
        val intent = FilePicker.Builder(this)
            .addPickMedia(
                PickMediaConfig(
                    popUpIcon = R.drawable.ic_photo,// DrawableRes Id
                    popUpText = "Image",
                    allowMultiple = true,// set Multiple pick file
                    maxFiles = 5,// max files working only in android latest version
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
        someActivityResultLauncher.launch(intent)
    }

    private val someActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Handle the result of the activity here
        if (result.resultCode == RESULT_OK) {
            MainScope().launch(Dispatchers.IO) {
                try{
                    val filePaths = result.data?.getStringArrayListExtra(Const.BundleExtras.FILE_PATH_LIST)
                    if(filePaths==null)
                    {
                        val filePath = result.data?.getStringExtra(Const.BundleExtras.FILE_PATH)
                        val file = File(filePath!!)
                        val bitmap = filePathToBitmap(filePath)

                        if(!isItemAlreadySelected(file.name))
                        {
                            val item = ItemsDataClass(file.name,bitmap,filePath,file, Uri.fromFile(file))
                            selectedImages.add(item)
                            withContext(Dispatchers.Main)
                            {
                                myAdapter.notifyItemInserted(selectedImages.size-1)
                            }

                        }
                        else{
                            showSnackbar("Image already selected")
                        }
                        return@launch
                    }
                    Log.i(tag,"File list size: ${filePaths!!.size}")
                    Log.i(tag, filePaths[0])

                    for (i in 0 until filePaths.size)
                    {
                        val file = File(filePaths[i])
                        val bitmap = filePathToBitmap(filePaths[i])
                        if(!isItemAlreadySelected(file.name))
                        {
                            val item = ItemsDataClass(file.name,bitmap,filePaths[i],file, Uri.fromFile(file))
                            selectedImages.add(item)
                            withContext(Dispatchers.Main)
                            {
                                myAdapter.notifyItemInserted(selectedImages.size-1)
                            }
                        }
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

    private fun showSnackbar(message: String) {
        val rootView: View = findViewById(android.R.id.content)
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
    }

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

    private var clickedItemPosition:Int=-1
    private lateinit var clickedItemBitmap:Bitmap
    override fun onCropItemClicked(position: Int) {
        clickedItemPosition=position
        val intent = Intent(this,ImageCropperActivity::class.java)
        intent.putExtra("DATA", selectedImages[position].uri.toString())
        imageCropActivityResultLauncher.launch(intent)
    }

    private val imageCropActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Handle the result of the activity here
        if (result.resultCode == -1) {
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
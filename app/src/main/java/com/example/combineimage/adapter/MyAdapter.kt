package com.example.combineimage.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.combineimage.CombineImageActivity
import com.example.combineimage.R
import com.example.combineimage.model.ItemsDataClass
import com.example.combineimage.utils.Utility
import com.example.combineimage.utils.Utility.Companion.mirrorHorizontally
import com.example.combineimage.utils.Utility.Companion.mirrorVertically
import com.example.combineimage.utils.Utility.Companion.rotateLeft
import com.example.combineimage.utils.Utility.Companion.rotateRight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class MyAdapter(private  val context:Context, private var itemsList: MutableList<ItemsDataClass>, val itemClickListener: ItemClickListener) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    private val tag = "MyAdapter"
    private lateinit var itemImageView:ImageView

    interface ItemClickListener {
        fun onDeleteItemClicked(position: Int)
        fun onCropItemClicked(position: Int)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val itemName : TextView = itemView.findViewById(R.id.itemName)
        val itemImage : ImageView = itemView.findViewById(R.id.itemImage)
        val itemCrop : ImageView = itemView.findViewById(R.id.itemCrop)
        val itemRotateLeft : ImageView = itemView.findViewById(R.id.itemRotateLeft)
        val itemRotateRight : ImageView = itemView.findViewById(R.id.itemRotateRight)
        val itemMirriorHorizontal : ImageView = itemView.findViewById(R.id.itemMirriorHorizontal)
        val itemMirriorVertical : ImageView = itemView.findViewById(R.id.itemMirriorVertical)
        val itemDelete : ImageView = itemView.findViewById(R.id.itemDelete)
        val itemSize : TextView = itemView.findViewById(R.id.itemSize)
        val itemDimension : TextView = itemView.findViewById(R.id.itemDimension)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.combine_image_recycleview_itemlayout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        itemImageView = holder.itemImage
        holder.itemName.text = itemsList[position].filename

        Glide.with(context)
            .asBitmap()
            .load(itemsList[position].bitmap) // Pass the Bitmap you want to load
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // Bitmap is loaded successfully, use 'resource' here
                    holder.itemImage.setImageBitmap(resource) // Display the Bitmap in an ImageView
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    // Handle failure to load the Bitmap
                }
            })
        /*holder.itemImage.setImageBitmap(itemsList[position].bitmap)*/
        MainScope().launch(Dispatchers.IO) {
            val size = Utility.formatFileSize(itemsList[position].file.length())
            val height = itemsList[position].bitmap.height
            val width = itemsList[position].bitmap.width
            withContext(Dispatchers.Main)
            {
                holder.itemSize.text = context.getString(R.string.printSize,size)
                holder.itemDimension.text = context.getString(R.string.printDimension,width,height)
            }

        }
        holder.itemCrop.setOnClickListener {
            itemClickListener.onCropItemClicked(holder.adapterPosition)
        }
        holder.itemRotateLeft.setOnClickListener {
            itemsList[holder.adapterPosition].bitmap=itemsList[holder.adapterPosition].bitmap.rotateLeft()
            notifyItemChanged(holder.adapterPosition)
        }
        holder.itemRotateRight.setOnClickListener {
            itemsList[holder.adapterPosition].bitmap=itemsList[holder.adapterPosition].bitmap.rotateRight()
            notifyItemChanged(holder.adapterPosition)
        }
        holder.itemMirriorHorizontal.setOnClickListener {
            itemsList[holder.adapterPosition].bitmap=itemsList[holder.adapterPosition].bitmap.mirrorHorizontally()
            notifyItemChanged(holder.adapterPosition)
        }
        holder.itemMirriorVertical.setOnClickListener {
            itemsList[holder.adapterPosition].bitmap=itemsList[holder.adapterPosition].bitmap.mirrorVertically()
            notifyItemChanged(holder.adapterPosition)
        }
        holder.itemDelete.setOnClickListener {
            itemClickListener.onDeleteItemClicked(holder.adapterPosition)
            /*holder.adapterPosition*/
            /*itemsList.removeAt(holder.adapterPosition)
            Log.i(tag,"itemsList Item Removed at Index: $position")
            Log.i(tag,"itemsList Size: ${itemsList.size}")
            Log.i(tag,"selectedImages Item Removed at Index: $position")
            Log.i(tag,"selectedImages Size: ${itemsList.size}")
            notifyDataSetChanged()*/
        }

    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    fun updateData(newItemList:MutableList<ItemsDataClass>) {
        itemsList =  newItemList
        notifyDataSetChanged()
    }
    // Perform the swap in the data source
    fun swapItems(fromPosition: Int, toPosition: Int) {
        /*Collections.swap(itemsList, fromPosition, toPosition)*/

        notifyItemMoved(fromPosition, toPosition) // Notify the RecyclerView about the change
    }



}
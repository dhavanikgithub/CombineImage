package com.example.combineimage.model

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

data class ItemsDataClass(
    var filename: String,
    var bitmap: Bitmap,
    var filepath: String,
    var file: File,
    var uri:Uri
)
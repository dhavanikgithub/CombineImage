package com.example.combineimage

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.combineimage.databinding.ActivityPermissionBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class PermissionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)

        if(permissionCheck()) {
            Intent(this, CombineImageActivity::class.java).apply {
                startActivity(this)
                finish()
            }
        } else{
            binding.btnStart.isEnabled=false
        }

        binding.btnStart.setOnClickListener {
            Intent(this, CombineImageActivity::class.java).apply {
                startActivity(this)
                finish()
            }
        }

        binding.btnAllow1.setOnClickListener {
            if (binding.txtAllow1.text.toString().uppercase()==getString(R.string.Allow))
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionListener = object : PermissionListener {
                        override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                            binding.txtAllow1.text = getString(R.string.Done)
                            binding.txtAllow1.setTextColor(getColor(R.color.permissionDoneText))
                            binding.btnStart.isEnabled = permissionCheck()
                        }

                        override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                            token?.continuePermissionRequest()
                        }

                        override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                            settingsActivityResultLauncher.launch(intent)
                        }
                    }
                    Dexter.withContext(this)
                        .withPermission(android.Manifest.permission.READ_MEDIA_IMAGES) // Replace with the desired permission
                        .withListener(permissionListener)
                        .check()
                }
                else{
                    val permissionListener = object : PermissionListener {
                        override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                            binding.txtAllow1.text = getString(R.string.Done)
                            binding.txtAllow1.setTextColor(getColor(R.color.permissionDoneText))
                            binding.btnStart.isEnabled = permissionCheck()
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permission: PermissionRequest?,
                            token: PermissionToken?
                        ) {
                            token?.continuePermissionRequest()
                        }

                        override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                            settingsActivityResultLauncher.launch(intent)
                        }
                    }
                    Dexter.withContext(this)
                        .withPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .withListener(permissionListener)
                        .check()
                }
            }

        }

        binding.btnAllow2.setOnClickListener {

            if (binding.txtAllow2.text.toString()==getString(R.string.Allow))
            {
                Dexter.withContext(this@PermissionActivity)
                    .withPermission(android.Manifest.permission.CAMERA)
                    .withListener(object : PermissionListener {
                        override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                            binding.txtAllow2.text = getString(R.string.Done)
                            binding.txtAllow2.setTextColor(getColor(R.color.permissionDoneText))
                            binding.btnStart.isEnabled = permissionCheck()
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permission: PermissionRequest?,
                            token: PermissionToken?
                        ) {
                            token?.continuePermissionRequest()
                        }

                        override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                            settingsActivityResultLauncher.launch(intent)
                        }
                    })
                    .check()
            }

        }
    }

    fun permissionCheck():Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val readStoragePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val writeStoragePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val readImagePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED

        if (cameraPermission) {
            binding.txtAllow2.text = getString(R.string.Done)
            binding.txtAllow2.setTextColor(getColor(R.color.permissionDoneText))
        } else {
            binding.txtAllow2.text = getString(R.string.Allow)
            binding.txtAllow2.setTextColor(getColor(R.color.permissionAllowText))
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if(readImagePermission)
            {
                binding.txtAllow1.text = getString(R.string.Done)
                binding.txtAllow1.setTextColor(getColor(R.color.permissionDoneText))
            }
            else{
                binding.txtAllow1.text = getString(R.string.Allow)
                binding.txtAllow1.setTextColor(getColor(R.color.permissionAllowText))
            }
        } else{
            if(readStoragePermission && writeStoragePermission)
            {
                binding.txtAllow1.text = getString(R.string.Done)
                binding.txtAllow1.setTextColor(getColor(R.color.permissionDoneText))
            }
            else{
                binding.txtAllow1.text = getString(R.string.Allow)
                binding.txtAllow1.setTextColor(getColor(R.color.permissionAllowText))
            }
        }

        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cameraPermission && readImagePermission
        } else{
            cameraPermission && readStoragePermission && writeStoragePermission
        }
    }

    val settingsActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(permissionCheck())
        {
            binding.btnStart.isEnabled=true
        }
    }
}
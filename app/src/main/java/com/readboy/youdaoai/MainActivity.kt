package com.readboy.youdaoai

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.tbruyelle.rxpermissions2.RxPermissions
import com.youdao.ocr.question.OCRListener
import com.youdao.ocr.question.OCRParameters
import com.youdao.ocr.question.OcrErrorCode
import com.youdao.ocr.question.SearchingImageQuestion
import com.youdao.sdk.app.EncryptHelper
import com.zxy.tiny.Tiny
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    var options: Tiny.FileCompressOptions? = null
    var dialogprogress: ProgressDialog?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dialogprogress=ProgressDialog(this)
        dialogprogress?.setMessage("处理中")
        dialogprogress?.isIndeterminate = true
        dialogprogress?.setCancelable(false)
        initPermission()
        options = Tiny.FileCompressOptions()
        take_photo.setOnClickListener {
            takePhoto()
        }
        take_pic.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "选择图片"), TAKE_PIC)
        }
    }

    private val FILE_PROVIDER_AUTHORITY = "com.readboy.youdaoai.fileprovider"
    var mImageUri: Uri? = null
    var file: File? = null
    val TAKE_PHOTO = 189
    val TAKE_PIC = 190
    @SuppressLint("CheckResult")
    private fun initPermission() {
        val rxPermissions = RxPermissions(this)
        rxPermissions.setLogging(true)
        rxPermissions.requestEach(
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.GET_TASKS,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.VIBRATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.REQUEST_INSTALL_PACKAGES

        ).subscribe { permission ->
            if (permission.granted) {
                // 用户已经同意该权限

            } else if (permission.shouldShowRequestPermissionRationale) {
                // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框

            } else {
                // 用户拒绝了该权限，并且选中『不再询问』

            }
        }
    }

    //开启相机
    fun takePhoto() {
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)//打开相机的Intent
        if (takePhotoIntent.resolveActivity(packageManager) != null) {//这句作用是如果没有相机则该应用不会闪退，要是不加这句则当系统没有相机应用的时候该应用会闪退
            file = createImageFile()
            if (file != null) {
                mImageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    /*7.0以上要通过FileProvider将File转化为Uri*/
                    FileProvider.getUriForFile(
                        this,
                        FILE_PROVIDER_AUTHORITY,
                        file!!
                    )
                } else {
                    /*7.0以下则直接使用Uri的fromFile方法将File转化为Uri*/
                    Uri.fromFile(file)
                }
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);//将用于输出的文件Uri传递给相机
                startActivityForResult(takePhotoIntent, TAKE_PHOTO)//打开相机
            }
        }
    }


    @SuppressLint("SimpleDateFormat")
    fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        var imageFile: File? = null
        try {
            imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return imageFile
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            TAKE_PHOTO -> {
                if (resultCode == -1) {//如果是0，证明用户取消了拍照
                    dialogprogress?.show()
                    Tiny.getInstance().source(file).asFile().withOptions(options)
                        .compress { isSuccess, outfile, t ->
                            if (isSuccess) {
                                val base64 = EncryptHelper.getBase64(utils.File2byte(outfile))
                                val tps = OCRParameters.Builder().timeout(100000)
                                    .packageName("com.readboy.youdaoai").build()
                                SearchingImageQuestion.getInstance(tps)
                                    .startSearching(base64, object : OCRListener {
                                        override fun onError(error: OcrErrorCode) {
                                            runOnUiThread {
                                                content.text =
                                                    error.code.toString() + ":" + error.name
                                                dialogprogress?.dismiss()
                                            }
                                        }

                                        override fun onResult(result: String) {
                                            runOnUiThread {
                                                content.text = result
                                                dialogprogress?.dismiss()
                                            }
                                        }
                                    })
                            }else{
                                dialogprogress?.dismiss()
                            }
                        }


                }

            }
            TAKE_PIC -> {

                if (resultCode == -1) {//如果是0，证明用户取消了
                    dialogprogress?.show()

                    Tiny.getInstance().source(data?.data).asFile().withOptions(options)
                        .compress { isSuccess, outfile, t ->
                            if (isSuccess) {
                                val base64 = EncryptHelper.getBase64(utils.File2byte(outfile))
                                val tps = OCRParameters.Builder().timeout(100000)
                                    .packageName("com.readboy.youdaoai").build()
                                SearchingImageQuestion.getInstance(tps)
                                    .startSearching(base64, object : OCRListener {
                                        override fun onError(error: OcrErrorCode) {
                                            runOnUiThread {
                                                content.text =
                                                    error.code.toString() + ":" + error.name
                                                dialogprogress?.dismiss()
                                            }
                                        }

                                        override fun onResult(result: String) {
                                            runOnUiThread {
                                                content.text = result
                                                dialogprogress?.dismiss()
                                            }
                                        }
                                    })
                            }else{
                                dialogprogress?.dismiss()
                            }
                        }

                }
            }
        }

    }
}

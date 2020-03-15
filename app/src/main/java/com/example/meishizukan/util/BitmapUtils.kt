package com.example.meishizukan.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.lang.Exception

object BitmapUtils {
    /*
    * 画像ファイルUriからBitmapを取得
    *
    * @param コンテキスト
    * @param 画像ファイルUri
    * @return Bitmap
    * */
    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor
        return try{
            BitmapFactory.decodeFileDescriptor(fileDescriptor)
        }catch (e:Exception){
            null
        }finally {
            parcelFileDescriptor?.close()
        }
    }

    /*
    * BitmapをBinaryに変換
    *
    * @param Bitmap
    * @return Binary
    * */
    fun convertBitmapToBinary(bitmap:Bitmap):ByteArray{
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG,100,byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }
}
package com.example.meishizukan.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
    * ビットマップをバイナリに変換(JPEG)
    *
    * @param ビットマップ
    * @return バイナリ
    * */
    fun convertBitmapToBinaryJPEG(bitmap:Bitmap):ByteArray{
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    /*
    * ビットマップをバイナリに変換(PNG)
    *
    * @param ビットマップ
    * @return バイナリ
    * */
    fun convertBitmapToBinaryPNG(bitmap: Bitmap):ByteArray{
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG,0,byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    /*
    * バイナリをビットマップに変換
    *
    * @param バイナリ
    * @return ビットマップ
    * */
    fun convertBinaryToBitmap(binary:ByteArray):Bitmap{
        return BitmapFactory.decodeByteArray(binary,0,binary.size)
    }

    /*
    * ビットマップを指定角度回転
    *
    * @param ビットマップ
    * @param 回転角度
    * @return ビットマップ
    * */
    fun rotateBitmap(bitmap: Bitmap,angle:Float): Bitmap {
        val matrix = Matrix()
       matrix.postRotate(angle)

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
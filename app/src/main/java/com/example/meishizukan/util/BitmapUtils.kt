package com.example.meishizukan.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import java.io.*
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
        return try{
            MediaStore.Images.Media.getBitmap(context.contentResolver,uri)
        }catch (e:Exception){
            Log.d("GET_BITMAP_ERROR",e.message)
            null
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

    /*
    * ビットマップを外部ストレージに保存
    *
    * @param コンテキスト
    * @param ビットマップ
    * @return 保存先URI
    * */
    fun saveBitmapToExternalStorage(context: Context, bitmap: Bitmap): Uri {
        val savedBitmapUri = MediaStore.Images.Media.insertImage(
            context.contentResolver,
            bitmap,
            null,
            null
        )

        return Uri.parse(savedBitmapUri)
    }

    /*
    * ビットマップを内部ストレージにファイル出力
    *
    * @param コンテキスト
    * @param ファイル名
    * @param ビットマップ
    * */
    fun saveBitmapToInternalStorage(context: Context,filename:String,bitmap: Bitmap){
        File(context.filesDir,filename).outputStream().use {
            fileOutputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG,0,fileOutputStream)
        }
    }

    /*
    * 内部ストレージからビットマップを取得する
    *
    * @param コンテキスト
    * @param ファイル名
    * @return ビットマップ
    * */
    fun getBitmapFromInternalStorage(context:Context,filename: String):Bitmap{
        File(context.filesDir,filename).inputStream().use {
            fileInputStream ->
            return convertBinaryToBitmap(fileInputStream.readBytes())
        }
    }
}
package com.example.meishizukan.dto

class Photo(
    id:Int,
    bitmapIndex:Int,
    bitmapBinary:ByteArray,
    createdOn:String
) {
    private val id = id
    private val bitmapIndex = bitmapIndex
    private val bitmapBinary = bitmapBinary
    private val createdOn = createdOn

    fun getId():Int{ return id }
    fun getBitmapIndex():Int{ return bitmapIndex }
    fun getBitmapBinary():ByteArray{ return bitmapBinary }
    fun getCreatedOn():String{ return createdOn }
}
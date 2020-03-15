package com.example.meishizukan.dto

class Photo(
    id:Int,
    hashedBinary:ByteArray,
    binary:ByteArray,
    createdOn:String
) {
    private val id = id
    private val hashedBinary = hashedBinary
    private val binary = binary
    private val createdOn = createdOn

    fun getId():Int{ return id }
    fun getHashedBinary():ByteArray{ return hashedBinary }
    fun getBinary():ByteArray{ return binary }
    fun getCreatedOn():String{ return createdOn }
}
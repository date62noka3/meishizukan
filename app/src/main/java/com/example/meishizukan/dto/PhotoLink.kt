package com.example.meishizukan.dto

class PhotoLink(
    id:Int,
    photoId:Int,
    personId:Int
) {
    private val id = id
    private val photoId = photoId
    private val personId = personId

    fun getId():Int{ return id }
    fun getPhotoId():Int{ return photoId }
    fun getPersonId():Int{ return personId }
}
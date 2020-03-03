package com.example.meishizukan.dto

class Organization(
    id:Int,
    name:String
) {
    private val id = id
    private val name = name

    fun getId():Int{ return id }
    fun getName():String{ return name }
}
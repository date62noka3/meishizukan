package com.example.meishizukan.dto

class Person(
    id:Int,
    name:String,
    phoneticName:String,
    sex:Int,
    organizationName:String,
    note:String
) {
    private val id = id
    private val name = name
    private val phoneticName = phoneticName
    private val sex = sex
    private val organizationName = organizationName
    private val note = note

    fun getId():Int{ return id }
    fun getName():String{ return name }
    fun getPhoneticName():String{ return phoneticName }
    fun getSex():Int{ return sex }
    fun getOrganizationName():String{ return organizationName }
    fun getNote():String{ return note }
}
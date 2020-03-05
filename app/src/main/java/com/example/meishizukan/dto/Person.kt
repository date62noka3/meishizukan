package com.example.meishizukan.dto

class Person(
    id:Int,
    name:String,
    phoneticName:String,
    sex:Int,
    birthday:String,
    organizationId:Int,
    note:String
) {
    private val id = id
    private val name = name
    private val phoneticName = phoneticName
    private val sex = sex
    private val organizationId = organizationId
    private val note = note

    fun getId():Int{ return id }
    fun getName():String{ return name }
    fun getPhoneticName():String{ return phoneticName }
    fun getSex():Int{ return sex }
    fun getOrganizationId():Int{ return organizationId }
    fun getNote():String{ return note }
}
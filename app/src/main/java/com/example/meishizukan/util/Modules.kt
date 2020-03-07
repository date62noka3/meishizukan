package com.example.meishizukan.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

object Modules {
    val phoneticNameRegex = Regex("^([\\u30A1-\\u30FA])+\$")

    fun getCurrentDate():String{
        val simpleDateFormat = SimpleDateFormat("M / d (EE)",Locale.JAPAN)
        return simpleDateFormat.format(Date())
    }
}
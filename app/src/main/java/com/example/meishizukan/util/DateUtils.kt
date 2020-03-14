package com.example.meishizukan.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    /*
    * 現在の日付を取得
    *
    * @return 現在の日付
    * */
    fun getCurrentDate():String{
        val simpleDateFormat = SimpleDateFormat("M / d (EE)", Locale.JAPAN)
        return simpleDateFormat.format(Date())
    }
}
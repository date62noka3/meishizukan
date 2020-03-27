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

    /*
    * 日付文字列から年を取得
    *
    * @param yyyy-MM-dd形式の日付文字列
    * @return 年
    * */
    fun getYear(date:String):Int{
        return date.substring(startIndex = 0,endIndex = 4).toInt()
    }

    /*
    * 日付文字列から月を取得
    *
    * @param yyyy-MM-dd形式の日付文字列
    * @return 月
    * */
    fun getMonth(date:String):Int{
        return date.substring(5,7).toInt()
    }

    /*
    * 日付文字列から日を取得
    *
    * @param yyyy-MM-dd形式の日付文字列
    * @return 日
    * */
    fun getDay(date:String):Int{
        return date.substring(8,10).toInt()
    }
}
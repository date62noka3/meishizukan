package com.example.meishizukan.util

import junit.framework.TestCase.assertEquals
import org.junit.Test
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

class DateUtilsTester{
    /*
    * 日付から年を数値で取得できるかをテスト
    * */
    @Test
    fun testGetYear(){
        assertEquals(2020,DateUtils.getYear("2020-04-03"))
    }

    /*
    * 日付から月を数値で取得できるかをテスト
    * */
    @Test
    fun testGetMonth(){
        assertEquals(4,DateUtils.getMonth("2020-04-03"))
    }

    /*
    * 日付から日を数値で取得できるかをテスト
    * */
    @Test
    fun testGetDay(){
        assertEquals(3,DateUtils.getDay("2020-04-03"))
    }
}
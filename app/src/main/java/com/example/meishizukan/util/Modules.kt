package com.example.meishizukan.util

import android.text.format.DateUtils
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

object Modules {
    val phoneticNameRegex = Regex("^([\\u30A1-\\u30F3])+\$") //ァ-ン

    private val deltaBetweenHiraganaAndKatakana = 'ア' - 'あ'
    /*
    * ひらがなをカタカナに変換
    *
    * @param 文字列
    * @return ひらがなをカタカナに変換した文字列
    * */
    fun hiraganaToKatakana(str:String):String{
        val sb = StringBuilder()

        for(char in str){
            if(Character.UnicodeBlock.of(char) == Character.UnicodeBlock.HIRAGANA){
                sb.append(char + deltaBetweenHiraganaAndKatakana) //カタカナに変換して追加
            }else{
                sb.append(char)
            }
        }

        return sb.toString()
    }

    fun getCurrentDate():String{
        val simpleDateFormat = SimpleDateFormat("M / d (EE)",Locale.JAPAN)
        return simpleDateFormat.format(Date())
    }
}
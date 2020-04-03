package com.example.meishizukan.util

import android.text.format.DateUtils
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

object PhoneticName {
    val phoneticNameRegex = Regex("^([ァ-ン]|[ぁ-ん])+\$")

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
}

class PhoneticNameTester{
    /*
    * 平仮名を片仮名に変換する処理をテスト
    * */
    @Test
    fun testHiraganaToKatakana(){
        assertEquals("サトウ",PhoneticName.hiraganaToKatakana("さとう"))
        assertEquals("サトウ",PhoneticName.hiraganaToKatakana("サトウ"))
        assertEquals("サトウタケル",PhoneticName.hiraganaToKatakana("サトウたける"))
        assertEquals("サトウ健",PhoneticName.hiraganaToKatakana("サトウ健"))
    }
}
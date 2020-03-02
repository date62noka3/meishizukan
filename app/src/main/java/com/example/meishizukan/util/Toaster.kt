package com.example.meishizukan.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object Toaster {
    /*
    * トーストを作成
    *
    * @param コンテキスト
    * @param 表示文字列
    * @param 文字サイズ
    * @param 文字色
    * @param 背景色
    * @param 表示時間
    * @return トースト
    * */
    fun createToast(
            context: Context,
            text:String,
            textSize:Float = 18F,
            textColor: Int = Color.parseColor("#000000"),
            backgroundColor:Int = Color.parseColor("#F0F0F0"),
            displayTime:Int = Toast.LENGTH_SHORT
        ):Toast{
        val toast = Toast.makeText(context,text,displayTime)

        val view = toast.view as LinearLayout
        val background = view.background.mutate() as GradientDrawable
        background.setColor(backgroundColor) //背景色

        val textView = view.getChildAt(0) as TextView
        textView.textSize = textSize //文字サイズ
        textView.setTextColor(textColor) //文字色

        return toast
    }
}
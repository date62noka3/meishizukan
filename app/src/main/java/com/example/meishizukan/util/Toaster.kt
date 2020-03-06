package com.example.meishizukan.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.ContextCompat.getColor
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.meishizukan.R

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
    @SuppressLint("ShowToast")
    fun createToast(
        context: Context,
        text:String,
        textSize:Float = 18F,
        textColor: Int = getColor(context, R.color.textColor),
        backgroundColor:Int = getColor(context, R.color.backgroundColor),
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
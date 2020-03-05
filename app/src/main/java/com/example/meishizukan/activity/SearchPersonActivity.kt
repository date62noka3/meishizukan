package com.example.meishizukan.activity

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.meishizukan.R
import com.example.meishizukan.util.DbHelper
import com.example.meishizukan.util.Modules
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_search_person.*

private const val KEYCODE_ENTER = 66

class SearchPersonActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_person)

        //AdMob初期化
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder()
            .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
            .build()
        adView.loadAd(adRequest)
        adView.adListener = object: AdListener() {
            override fun onAdLoaded() {}
            override fun onAdFailedToLoad(errorCode : Int) {}
            override fun onAdOpened() {}
            override fun onAdClicked() {}
            override fun onAdLeftApplication() {}
            override fun onAdClosed() {}
        }

        //入力値の有無を判定
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(searchEditText.text.isNullOrEmpty()){
                    clearEditTextButton.visibility = View.INVISIBLE
                }else{
                    clearEditTextButton.visibility = View.VISIBLE
                }
            }
        })

        val inputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        //エンターキー押下を検知
        searchEditText.setOnKeyListener{
                v, keyCode, event ->
            Log.d("PRESSED_KEY",keyCode.toString())

            if(keyCode == KEYCODE_ENTER){
                if(searchEditText.text.isEmpty()){
                    inputMethodManager.hideSoftInputFromWindow(v.windowToken,0) //キーボードを非表示
                }
                if(searchEditText.text.isNotEmpty()){
                    inputMethodManager.hideSoftInputFromWindow(v.windowToken,0) //キーボードを非表示
                    //TODO 検索
                }
                true
            }else {
                false
            }
        }

        //入力欄のレイアウトを更新
        searchEditText.setOnFocusChangeListener{
                v, hasFocus ->
            v ?: return@setOnFocusChangeListener
            initSearchEditTextBackground(hasFocus = hasFocus)
        }

        var isKeyboardShown = false
        //入力欄のレイアウトを更新
        fun onKeyboardVisibilityChanged() {
            searchEditText.isCursorVisible = isKeyboardShown
            initSearchEditTextBackground(isKeyboardShown = isKeyboardShown)
        }

        //キーボードの表示状態を判定
        rootConstraintLayout.viewTreeObserver.addOnGlobalLayoutListener{
            val r = Rect()
            rootConstraintLayout.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootConstraintLayout.rootView.height

            val keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) {
                if (!isKeyboardShown) {
                    isKeyboardShown = true
                    onKeyboardVisibilityChanged()
                    Log.d("KEYBOARD_STATUS","OPENED")
                }
            }
            else {
                if (isKeyboardShown) {
                    isKeyboardShown = false
                    onKeyboardVisibilityChanged()
                    Log.d("KEYBOARD_STATUS","CLOSED")
                }
            }
        }

        //入力値をクリア
        clearEditTextButton.setOnClickListener{
            searchEditText.setText("")
        }

        //メニューを前面にし、クリックとを検知させる
        drawerLayout.addDrawerListener(
            object : DrawerLayout.DrawerListener{
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
                override fun onDrawerStateChanged(newState: Int) {}
                override fun onDrawerOpened(drawerView: View) {
                    menuRootConstraintLayout.bringToFront()
                }
                override fun onDrawerClosed(drawerView: View) {
                    rootConstraintLayout.bringToFront()
                }
            }
        )

        //メニューを表示
        menuButton.setOnClickListener{
            drawerLayout.openDrawer(Gravity.LEFT)
        }

        //メニュー表示時後ろにクリックを通さないため
        menuRootConstraintLayout.setOnClickListener{}

        addPersonButton.setOnClickListener{
            val intent = Intent(this,InputPersonalInfoActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume(){
        super.onResume()
        adView.resume()

        //現在の日付を表示
        dateTextView.text = Modules.getCurrentDate()
    }

    override fun onDestroy(){
        adView.destroy()
        super.onDestroy()
    }

    /*
    * searchEditTextの背景を初期化
    * */
    private fun initSearchEditTextBackground(hasFocus:Boolean=false, isKeyboardShown:Boolean=false){
        if(hasFocus || isKeyboardShown){
            searchEditText.setBackgroundResource(R.drawable.search_edittext_active_background)
        }else{
            searchEditText.setBackgroundResource(R.drawable.search_edittext_background)
        }
    }
}

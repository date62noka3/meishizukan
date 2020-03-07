package com.example.meishizukan.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.drawerlayout.widget.DrawerLayout
import com.example.meishizukan.R
import com.example.meishizukan.dto.Person
import com.example.meishizukan.util.DbContracts
import com.example.meishizukan.util.DbHelper
import com.example.meishizukan.util.Modules
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_search_person_view.*

object Sex{
    const val NOT_KNOWN = 0
    const val MALE = 1
    const val FEMALE = 2
}

private const val KEYCODE_ENTER = 66

class SearchPersonActivity : AppCompatActivity() {

    private val dbHelper = DbHelper(this)
    private lateinit var readableDB:SQLiteDatabase

    private lateinit var sexTypes:Array<String>
    private lateinit var firstRowOfJapaneseSyllabary:Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_person_view)

        readableDB = dbHelper.readableDatabase

        sexTypes = resources.getStringArray(R.array.sex_types)
        firstRowOfJapaneseSyllabary =  resources.getStringArray(R.array.first_row_of_japanese_syllabary)

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
                if(searchEditText.text.isBlank()){
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

            //エンターキー押下で検索を実行
            if(keyCode == KEYCODE_ENTER){
                inputMethodManager.hideSoftInputFromWindow(v.windowToken,0) //キーボードを非表示

                searchPerson()

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

        searchButton.setOnClickListener{
            inputMethodManager.hideSoftInputFromWindow(it.windowToken,0) //キーボードを非表示

            searchPerson()
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
            drawerLayout.openDrawer(menuRootConstraintLayout,true)
        }

        //メニュー表示時後ろにクリックを通さないため
        menuRootConstraintLayout.setOnClickListener{}

        addPersonButton.setOnClickListener{
            val intent = Intent(this,PersonalInfoViewActivity::class.java)
            intent.putExtra("PERSON_ID",1)
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

    private val getAllPersonsSQL = "SELECT ${BaseColumns._ID}," +
            "${DbContracts.Persons.COLUMN_NAME}," +
            "${DbContracts.Persons.COLUMN_PHONETIC_NAME}," +
            "${DbContracts.Persons.COLUMN_SEX}," +
            "${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}," +
            DbContracts.Persons.COLUMN_NOTE +
            " FROM ${DbContracts.Persons.TABLE_NAME}"
    /*
    * 人物リストを取得
    *
    * @param 実行するSQL
    * @return 人物リスト
    * */
    private fun readPersons(sql:String):MutableList<Person>{
        val cursor = readableDB.rawQuery(sql,null)
        val persons = mutableListOf<Person>()

        if(cursor.count == 0){
            cursor.close()
            return persons
        }

        while(cursor.moveToNext()){
            persons.add(Person(
                id = cursor.getInt(0),
                name = cursor.getString(1),
                phoneticName = cursor.getString(2),
                sex = cursor.getInt(3),
                organizationName = cursor.getString(4),
                note = cursor.getString(5)
            ))
        }

        cursor.close()

        return persons
    }

    /*
    * 人物を検索
    * */
    private fun searchPerson(){
        val keyword = searchEditText.text.toString()

        val persons = mutableListOf<Person>()

        //人物を検索
        if(keyword.isBlank()){ //空白の場合全てを表示
            persons.addAll(readPersons(getAllPersonsSQL))
        }else{ //曖昧検索
            //キーワードがカタカナであれば名前においてフリガナで検索する
            val sql = "SELECT ${BaseColumns._ID}," +
                    "${DbContracts.Persons.COLUMN_NAME}," +
                    "${DbContracts.Persons.COLUMN_PHONETIC_NAME}," +
                    "${DbContracts.Persons.COLUMN_SEX}," +
                    "${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}," +
                    DbContracts.Persons.COLUMN_NOTE +
                    " FROM ${DbContracts.Persons.TABLE_NAME}" +
                    " WHERE " +
                    if(keyword.matches(Modules.phoneticNameRegex)){
                        DbContracts.Persons.COLUMN_PHONETIC_NAME
                    }else{
                        DbContracts.Persons.COLUMN_NAME
                    } +
                    " LIKE '%$keyword%'" +
                    " OR ${DbContracts.Persons.COLUMN_ORGANIZATION_NAME} LIKE '%$keyword%'"

            persons.addAll(readPersons(sql))
        }

        //リストビューを更新
        updateListView(persons)
    }

    /*
    * リストビューを更新
    * */
    private fun updateListView(persons:MutableList<Person>){
        //アイテムをクリア
        personListLinearLayout.removeAllViews()

        var prevPhoneticNameFirstChar = ' ' //ふりがな先頭1文字

        persons.forEach{
            val phoneticNameFirstChar = it.getPhoneticName()[0]
            if(prevPhoneticNameFirstChar != phoneticNameFirstChar){
                //50音セパレーターを追加
                this.layoutInflater.inflate(R.layout.person_listview_separator,personListLinearLayout)
                val separator = personListLinearLayout.getChildAt(personListLinearLayout.childCount - 1) as ConstraintLayout
                val separatorTextView = separator.findViewById<TextView>(R.id.separatorTextView)
                separatorTextView?.text = phoneticNameFirstChar.toString()

                prevPhoneticNameFirstChar = phoneticNameFirstChar
            }

            //人物アイテムを追加
            this.layoutInflater.inflate(R.layout.person_listview_item,personListLinearLayout)
            val item = personListLinearLayout.getChildAt(personListLinearLayout.childCount - 1) as ConstraintLayout
            val phoneticNameTextView = item.findViewById<TextView>(R.id.phoneticNameTextView)
            phoneticNameTextView?.text = it.getPhoneticName()
            val nameTextView = item.findViewById<TextView>(R.id.nameTextView)
            nameTextView?.text = it.getName()
            val organizationNameTextView = item.findViewById<TextView>(R.id.organizationNameTextView)
            organizationNameTextView?.text = it.getOrganizationName()
            val sexTextView = item.findViewById<TextView>(R.id.sexTextView)
            sexTextView?.text = sexTypes[it.getSex()]
            when(it.getSex()){
                Sex.NOT_KNOWN -> { sexTextView?.setBackgroundResource(R.color.notKnownBackgroundColor) }
                Sex.MALE -> { sexTextView?.setBackgroundResource(R.color.maleBackgroundColor) }
                Sex.FEMALE -> { sexTextView?.setBackgroundResource(R.color.femaleBackgroundColor) }
            }
        }
    }
}

package com.example.meishizukan.activity

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.BaseColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
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

private object Sex{
    const val NOT_KNOWN = 0
    const val MALE = 1
    const val FEMALE = 2
}

private const val NO_MEANS_REQUEST_CODE = 0

private const val KEYCODE_ENTER = 66

class SearchPersonActivity : AppCompatActivity() {

    private val dbHelper = DbHelper(this)
    private lateinit var readableDB:SQLiteDatabase

    private lateinit var sexTypes:Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_person_view)

        readableDB = dbHelper.readableDatabase

        sexTypes = resources.getStringArray(R.array.sex_types)

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
            startActivityForResult(intent,NO_MEANS_REQUEST_CODE)
        }

        val addPersonsOnScrollDelay = 500L //スクロール発火で人物を追加する際の周期
        val addPersonsOnScrollHandler = Handler()
        //一番下までスクロールされたらリストビューを更新する
        personListScrollView.viewTreeObserver.addOnScrollChangedListener {
            addPersonsOnScrollHandler.postDelayed({
                if (personListScrollView.getChildAt(0).bottom
                    <= (personListScrollView.height + personListScrollView.scrollY)) {
                        addPersonsToListView()
                }
            },addPersonsOnScrollDelay) //負荷を軽減するため次のスクロールでの処理まで時間を置く
        }

        loadingDialogView.bringToFront()
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

    private val researchDelay = 1000L //再検索の遅延時間
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //1度検索していて、遷移先から戻った際には再検索をかける
        //データが変わっている、または削除されている可能性があるため
        if(isSearchedBeforeTransition && requestCode == NO_MEANS_REQUEST_CODE) {
            showLoadingDialog()

            searchPersonHandler.postDelayed({
                searchPerson()

                hideLoadingDialog()
            },researchDelay)

            isSearchedBeforeTransition = false
        }
    }

    /*
    * ローディング画面を表示
    * */
    private fun showLoadingDialog(){
        loadingDialogView.visibility = View.VISIBLE
    }

    /*
    * ローディング画面を非表示
    * */
    private fun hideLoadingDialog(){
        loadingDialogView.visibility = View.INVISIBLE
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
            " FROM ${DbContracts.Persons.TABLE_NAME}" +
            " ORDER BY ${DbContracts.Persons.COLUMN_PHONETIC_NAME}"
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

    private var isSearchedBeforeTransition = false //別アクティビティに遷移する前に検索したか否か
    private val searchPersonHandler = Handler()
    private val waitingToDisplay = mutableListOf<Person>() //表示待ちの人物リスト
    /*
    * 人物を検索
    * */
    private fun searchPerson(){
        waitingToDisplay.clear() //待機人物リストをクリア

        val keyword = searchEditText.text.toString()

        val persons = mutableListOf<Person>()

        //検索を非同期実行
        searchPersonHandler.post {
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
                        " OR ${DbContracts.Persons.COLUMN_ORGANIZATION_NAME} LIKE '%$keyword%'" +
                        " ORDER BY ${DbContracts.Persons.COLUMN_PHONETIC_NAME}"

                persons.addAll(readPersons(sql))
            }

            waitingToDisplay.addAll(persons)

            personListLinearLayout.removeAllViews() //人物アイテムを全てクリア

            addPersonsToListView()

            scrollToTop()
        }

        isSearchedBeforeTransition = true
    }

    /*
    * 一番上までスクロール
    * */
    private fun scrollToTop(){
        personListScrollView.scrollY = 0
    }

    /*
    * リストビューに人物を追加
    * */
    private fun addPersonToListView(person: Person){
        this.layoutInflater.inflate(R.layout.person_listview_item,personListLinearLayout)
        val item = personListLinearLayout.getChildAt(personListLinearLayout.childCount - 1) as ConstraintLayout

        val phoneticNameTextView = item.findViewById<TextView>(R.id.phoneticNameTextView)
        phoneticNameTextView?.text = person.getPhoneticName()
        val nameTextView = item.findViewById<TextView>(R.id.nameTextView)
        nameTextView?.text = person.getName()
        val organizationNameTextView = item.findViewById<TextView>(R.id.organizationNameTextView)
        organizationNameTextView?.text = person.getOrganizationName()
        val sexTextView = item.findViewById<TextView>(R.id.sexTextView)
        sexTextView?.text = sexTypes[person.getSex()]
        when(person.getSex()){
            Sex.NOT_KNOWN -> { sexTextView?.setBackgroundResource(R.color.notKnownBackgroundColor) }
            Sex.MALE -> { sexTextView?.setBackgroundResource(R.color.maleBackgroundColor) }
            Sex.FEMALE -> { sexTextView?.setBackgroundResource(R.color.femaleBackgroundColor) }
        }
    }

    /*
    * リストビューにセパレーターを追加
    *
    * @param セパレーターテキスト
    * */
    private fun addSeparatorToListView(separatorText:String){
        this.layoutInflater.inflate(
            R.layout.person_listview_separator,
            personListLinearLayout
        )
        val separator =
            personListLinearLayout.getChildAt(personListLinearLayout.childCount - 1) as ConstraintLayout

        val separatorTextView =
            separator.findViewById<TextView>(R.id.separatorTextView)
        separatorTextView?.text = separatorText
    }

    private val additionalLimit = 15 //1回の更新で追加表示するアイテム数の上限
    private var currentJapaneseSyllabaryRegex = "" //現在のア段正規表現
    private var prevPhoneticNameFirstChar = "" //前回のふりがな先頭1文字
    /*
    * リストビューに人物リストをまとめて追加
    *
    * @return 追加件数
    * */
    private fun addPersonsToListView():Int{
        if(waitingToDisplay.count() == 0){
            return 0
        }

        //追加する前に余白として追加していたViewを削除
        if(0 < personListLinearLayout.childCount) {
            personListLinearLayout.removeViewAt(personListLinearLayout.childCount - 1)
        }

        val japaneseSyllabaryRegex = resources.getStringArray(R.array.japanese_syllabary_regex) //50音のア段正規表現　どの列に属するかを判定するため
        val japaneseSyllabaryText = resources.getStringArray(R.array.japanese_syllabary_text) //セパレーターに表示するア段文字列
        var lastIndexOfDisplayedItem = 0 //最後に表示したアイテムのインデックス

        waitingToDisplay.forEachIndexed {
            i,person ->

            if(additionalLimit <= i)
                return@forEachIndexed

            val phoneticNameFirstChar = person.getPhoneticName()[0].toString()

            if(prevPhoneticNameFirstChar != phoneticNameFirstChar) {
                japaneseSyllabaryRegex.forEachIndexed { j, regex ->

                    if (phoneticNameFirstChar.matches(Regex(regex))
                        && currentJapaneseSyllabaryRegex != regex) {
                        addSeparatorToListView(japaneseSyllabaryText[j]) //50音セパレーターを追加

                        currentJapaneseSyllabaryRegex = regex
                    }
                }
            }

            //人物アイテムを追加
            addPersonToListView(person)

            prevPhoneticNameFirstChar = phoneticNameFirstChar
            lastIndexOfDisplayedItem = i
        }

        //表示した人物を待機リストから削除
        for(i in lastIndexOfDisplayedItem downTo 0){
            waitingToDisplay.removeAt(i)
        }

        if(waitingToDisplay.isEmpty()){
            //一番下に余白を作るため空のViewを追加する
            this.layoutInflater.inflate(R.layout.person_listview_empty_item, personListLinearLayout)
        }else{
            //次に一番下までスクロールした際に見せるローディング画面(view)
            this.layoutInflater.inflate(R.layout.person_listview_loading_item, personListLinearLayout)
        }

        return lastIndexOfDisplayedItem + 1
    }
}

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
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.drawerlayout.widget.DrawerLayout
import com.example.meishizukan.R
import com.example.meishizukan.dto.Person
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_search_person_view.*
import androidx.core.content.ContextCompat.getColor
import com.example.meishizukan.test.PersonDb
import com.example.meishizukan.util.*
import com.example.meishizukan.util.PhoneticName.hiraganaToKatakana
import com.google.android.gms.ads.RequestConfiguration
import junit.framework.TestCase.assertEquals
import kotlinx.android.synthetic.main.activity_search_person_view.adView
import kotlinx.android.synthetic.main.activity_search_person_view.deleteButton
import kotlinx.android.synthetic.main.activity_search_person_view.footerOptionBar
import kotlinx.android.synthetic.main.activity_search_person_view.headerMenu
import kotlinx.android.synthetic.main.activity_search_person_view.selectedItemCountTextView
import org.junit.Test

private object Sex{
    const val NOT_KNOWN = 0
    const val MALE = 1
    const val FEMALE = 2
}

private const val NO_MEANS_REQUEST_CODE = 0

private const val KEYCODE_ENTER = 66

class SearchPersonViewActivity : AppCompatActivity() {

    private val dbHelper = DbHelper(this)
    private lateinit var readableDb:SQLiteDatabase
    private lateinit var writableDb:SQLiteDatabase

    private lateinit var sexTypes:Array<String>

    private val removePersons = mutableListOf<Int>() //削除対象人物リスト<人物ID>

    private lateinit var personDb:PersonDb //人物のテストデータクラス

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme) //スプラッシュ表示用テーマから戻す
        setContentView(R.layout.activity_search_person_view)

        readableDb = dbHelper.readableDatabase
        writableDb = dbHelper.writableDatabase

        sexTypes = resources.getStringArray(R.array.sex_types)

        //AdMob初期化
        val testDevices = mutableListOf<String>()
        testDevices.add(AdRequest.DEVICE_ID_EMULATOR)
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDevices)
            .build()
        MobileAds.initialize(this) {}
        MobileAds.setRequestConfiguration(requestConfiguration)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {}
            override fun onAdFailedToLoad(errorCode: Int) {}
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
            if(keyCode == KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP){
                inputMethodManager.hideSoftInputFromWindow(v.windowToken,0) //キーボードを非表示

                search()

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
            if(!isKeyboardShown) {
                searchEditText.clearFocus() //selectAllOnFocusを走らせるため
            }
            initSearchEditTextBackground(isKeyboardShown = isKeyboardShown)
        }

        //キーボードの表示状態を判定
        rootConstraintLayout.viewTreeObserver.addOnGlobalLayoutListener{
            val r = Rect()
            rootConstraintLayout.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootConstraintLayout.rootView.height

            val keypadHeight = screenHeight - r.bottom

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

        //検索
        searchButton.setOnClickListener{
            inputMethodManager.hideSoftInputFromWindow(it.windowToken,0) //キーボードを非表示

            search()
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
                    if(isKeyboardShown){
                        inputMethodManager.hideSoftInputFromWindow(drawerView.windowToken,0) //キーボードを非表示
                    }
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

        //メニュー表示時、後ろにクリックを通さないため
        menuRootConstraintLayout.setOnClickListener{}

        //メニューを閉じる
        searchPersonViewButton.setOnClickListener{
            drawerLayout.closeDrawer(menuRootConstraintLayout,true)
        }

        //全ての写真一覧画面を表示
        allPhotosViewButton.setOnClickListener{
            Toaster.createToast(
                context = this,
                text = getString(R.string.message_on_click_all_photos_view_button)
            ).show()
        }

        //人物情報画面を新規追加で表示
        addPersonButton.setOnClickListener{
            val intent = Intent(this,PersonalInfoViewActivity::class.java)
            startActivityForResult(intent,NO_MEANS_REQUEST_CODE)
        }

        //人物リストビューの一番上までスクロール
        headerMenu.setOnClickListener{
            //直前に強いスクロールがあった場合、スムーズでないとひっくり返せない
            scrollToTop(true)
        }

        //オプションバー背後のリストアイテムにクリックを通さないようにする
        footerOptionBar.setOnClickListener{}

        //人物リストビューにおいて一番下までスクロールされたかを判定し、追加検索を行う
        personListScrollView.viewTreeObserver.addOnScrollChangedListener{
            val loadingItem = personListLinearLayout.findViewById<LinearLayout>(R.id.rootLinearLayout)
            loadingItem?:return@addOnScrollChangedListener

            //ローディング画面(view)までスクロールされたら一番下までスクロールし、ロック
            if(personListLinearLayout.bottom - loadingItem.height
                <= personListScrollView.height + personListScrollView.scrollY
                && !scrollViewIsLocked){

                showLoadingItem()

                personListScrollView.smoothScrollBy(0,loadingItem.height)

                lockScrollView()
            }

            personListScrollView.postDelayed({
                //前回の追加検索が0件だった場合追加検索を行わない
                if(prevAdditionalSearchPersonsCount == 0){
                    return@postDelayed
                }

                //一番下までスクロールされたら追加検索し、ロックを解除
                if(personListLinearLayout.bottom
                    <= personListScrollView.height + personListScrollView.scrollY
                    && scrollViewIsLocked){

                    additionalSearch()

                    unlockScrollView()
                }
            },500)
        }

        //選択されている人物を削除
        deleteButton.setOnClickListener{
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_dialog_title))
                .setMessage(getString(R.string.message_on_confirm_delete_person))
                .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                    deletePersons()

                    footerOptionBar.visibility = View.INVISIBLE
                    addPersonButton.visibility = View.VISIBLE

                    search()

                    Toaster.createToast(
                        context = this,
                        text = getString(R.string.message_on_deleted_persons),
                        textColor = getColor(this,R.color.toastTextColorOnSuccess),
                        backgroundColor = getColor(this,R.color.toastBackgroundColorOnSuccess),
                        displayTime = Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton(getString(R.string.negative_button_text)) { _, _ -> }
                .setCancelable(false)
                .show()
        }

        //人物の選択を解除する
        cancelButton.setOnClickListener{
            removePersons.clear()

            footerOptionBar.visibility = View.INVISIBLE
            addPersonButton.visibility = View.VISIBLE
            searchButton.setBackgroundResource(R.drawable.search_button_background)

            search()
        }

        val progressTextView = loadingAnimationView.findViewById<TextView>(R.id.progressTextView)
        progressTextView?.text = getString(R.string.progress_text_on_search)

        //すべての写真リンク、写真、人物を削除
        writableDb.delete("photos_links","_id <> 10000",null)
        writableDb.delete("photos","_id <> 10000",null)
        writableDb.delete("persons","_id <> 10000",null)

        //テスト用データの人物を追加
        personDb = PersonDb(this)
        personDb.insert()
    }

    override fun onResume(){
        super.onResume()
        adView.resume()

        //現在の日付を表示
        dateTextView.text = DateUtils.getCurrentDate()
    }

    override fun onDestroy(){
        super.onDestroy()

        adView.destroy()
        readableDb.close()
        writableDb.close()
        dbHelper.close()
    }

    private val researchDelay = 1000L //再検索の遅延時間
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //1度検索していて、遷移先から戻った際には再検索をかける
        //データが変わっている、または削除されている可能性があるため
        if(isSearchedBeforeTransition && requestCode == NO_MEANS_REQUEST_CODE) {
            showLoadingDialog()

            searchHandler.postDelayed({
                search()

                hideLoadingDialog()
            },researchDelay)

            isSearchedBeforeTransition = false
        }
    }

    /*
    * 人物を削除
    * */
    private fun deletePersons(){
        for(personId in removePersons){
            deleteLinkedPhotos(personId)
            writableDb.delete(DbContracts.Persons.TABLE_NAME,"${BaseColumns._ID} = $personId",null)
            Log.d("DELETED_PERSON_ID",personId.toString())
        }

        removePersons.clear()
    }

    /*
    * 人物にリンクしている写真のリンクを解除する
    *
    * @param 人物ID
    * */
    private fun deleteLinkedPhotos(personId:Int){
        writableDb.delete(DbContracts.PhotosLinks.TABLE_NAME,"${DbContracts.PhotosLinks.COLUMN_PERSON_ID} = $personId",null)
    }

    /*
    * 一番上までスクロール
    *
    * @param スムーズなスクロールをするか
    * */
    private fun scrollToTop(smooth:Boolean){
        if(smooth) {
            personListScrollView.smoothScrollTo(0, 0)
        }else{
            personListScrollView.scrollTo(0, 0)
        }
    }

    private var scrollViewIsLocked = false
    /*
    * スクロールビューをロック(スクロールさせない)
    * */
    private fun lockScrollView(){
        personListScrollView.setOnTouchListener{ _, _ -> true }
        scrollViewIsLocked = true
    }

    /*
    * スクロールビューのロックを解除(スクロール受け付ける)
    * */
    private fun unlockScrollView(){
        personListScrollView.setOnTouchListener(null)
        scrollViewIsLocked = false
    }

    /*
    * ローディング画面を表示
    * */
    private fun showLoadingDialog(){
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) //タッチ無効化
        loadingAnimationView.bringToFront()
        loadingAnimationView.visibility = View.VISIBLE
    }

    /*
    * ローディング画面を非表示
    * */
    private fun hideLoadingDialog(){
        rootConstraintLayout.bringToFront()
        loadingAnimationView.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) //タッチ無効化解除
    }

    /*
    * 人物リストビューのローディングアイテムを表示
    * */
    private fun showLoadingItem(){
        val loadingItem = personListLinearLayout.findViewById<LinearLayout>(R.id.rootLinearLayout)
        loadingItem.visibility = View.VISIBLE
    }

    /*
    * 検索結果無し画面(view)を表示
    * */
    private fun showNoResultsTextView(){
        noResultsTextView.visibility = View.VISIBLE
    }

    /*
    * 検索結果無し画面(view)を非表示
    * */
    private fun hideNoResultsTextView(){
        noResultsTextView.visibility = View.GONE
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

    /*
    * 人物リストを取得
    *
    * @param 実行するSQL
    * @return 人物リスト
    * */
    private fun readPersons(sql:String):MutableList<Person>{
        val cursor = readableDb.rawQuery(sql,null)
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

    private val limit = 15 //1回で追加表示するアイテム数の上限
    private var offset = 0 //検索位置
    /*
    * 検索SQLを作成
    *
    * @param 検索キーワード
    * @return 検索SQL
    * */
    private fun createSearchSql(keyword:String):String{
        return if(keyword.isBlank()){ //全てを取得
            "SELECT ${BaseColumns._ID}," +
                    "${DbContracts.Persons.COLUMN_NAME}," +
                    "${DbContracts.Persons.COLUMN_PHONETIC_NAME}," +
                    "${DbContracts.Persons.COLUMN_SEX}," +
                    "${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}," +
                    DbContracts.Persons.COLUMN_NOTE +
                    " FROM ${DbContracts.Persons.TABLE_NAME}" +
                    " ORDER BY ${DbContracts.Persons.COLUMN_PHONETIC_NAME}" +
                    " LIMIT $limit OFFSET $offset"
        }else { //人物名と組織名で曖昧検索する
            //キーワードに漢字が含まれていなければ名前(フリガナ)で検索し、
            //含まれていなければ名前(漢字)で検索する
            "SELECT ${BaseColumns._ID}," +
                    "${DbContracts.Persons.COLUMN_NAME}," +
                    "${DbContracts.Persons.COLUMN_PHONETIC_NAME}," +
                    "${DbContracts.Persons.COLUMN_SEX}," +
                    "${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}," +
                    DbContracts.Persons.COLUMN_NOTE +
                    " FROM ${DbContracts.Persons.TABLE_NAME}" +
                    " WHERE " +
                    if (hiraganaToKatakana(keyword).matches(PhoneticName.phoneticNameRegex)) {
                        "${DbContracts.Persons.COLUMN_PHONETIC_NAME} LIKE '%${hiraganaToKatakana(keyword)}%'"
                    } else {
                        "${DbContracts.Persons.COLUMN_NAME} LIKE '%$keyword%'"
                    } +
                    " OR ${DbContracts.Persons.COLUMN_ORGANIZATION_NAME} LIKE '%$keyword%'" +
                    " ORDER BY ${DbContracts.Persons.COLUMN_PHONETIC_NAME}" +
                    " LIMIT $limit OFFSET $offset"
        }
    }

    private var isSearchedBeforeTransition = false //別アクティビティに遷移する前に検索したか否か
    private val searchHandler = Handler()
    private var prevSQL = ""
    /*
    * 人物を検索
    * */
    private fun search(){
        //人物選択中は検索を返す
        if(removePersons.isNotEmpty()){
            return
        }

        currentJapaneseSyllabaryRegex = "" //現在のア段正規表現をクリア
        prevPhoneticNameFirstChar = "" //前回のふりがな1文字目をクリア
        prevAdditionalSearchPersonsCount = -1 //前回の追加検索件数をクリア
        offset = 0 //オフセットをクリア

        personListLinearLayout.removeAllViews() //人物アイテムを全てクリア

        //スクロールに時間がかかり、人物追加が走ってしまうためfalse
        scrollToTop(false)

        val keyword = searchEditText.text.toString()

        val sql = createSearchSql(keyword)

        val addPersonsCount = addPersonsToListView(readPersons(sql))

        if(keyword.isBlank()){ //全検索の場合且つDBのpersonsレコードが0件の場合
            noResultsTextView.text = getString(R.string.no_persons_text)
        }else{
            noResultsTextView.text = getString(R.string.no_results_textview_text)
        }

        if(addPersonsCount == 0){
            showNoResultsTextView()
        }else{
            hideNoResultsTextView()
        }

        prevSQL = sql

        isSearchedBeforeTransition = true
    }

    private var prevAdditionalSearchPersonsCount:Int = -1 //前回の追加検索件数
    /*
    * 人物を追加検索する
    * */
    private fun additionalSearch(){
        offset += limit //検索位置を移動

        //前回のオフセットを削除し、次のオフセットを付与
        val removeStartIndex = prevSQL.indexOf("OFFSET")
        prevSQL = prevSQL.removeRange(removeStartIndex,prevSQL.length)
        prevSQL = prevSQL.plus("OFFSET $offset")

        Log.d("ADDITIONAL_SEARCH_SQL",prevSQL)

        prevAdditionalSearchPersonsCount = addPersonsToListView(readPersons(prevSQL))
    }

    /*
    * 名前が空か否かを取得
    *
    * @param 名前
    * @return 名前が空か否か
    * */
    private fun personNameIsBlank(name:String):Boolean{
        return name == nameSplit.toString()
    }

    private val nameSplit = ','
    /*
    * リストビューに人物を追加
    *
    * @param 人物
    * */
    private fun addPersonToListView(person: Person){
        this.layoutInflater.inflate(R.layout.person_listview_item,personListLinearLayout)
        val item = personListLinearLayout.getChildAt(personListLinearLayout.childCount - 1) as ConstraintLayout
        item.tag = person.id.toString()
        item.setOnClickListener(ItemOnClickListener())
        item.setOnLongClickListener(ItemOnLongClickListener())

        val nameTextView = item.findViewById<TextView>(R.id.nameTextView)
        val phoneticNameTextView = item.findViewById<TextView>(R.id.phoneticNameTextView)
        var name = person.name
        //名前 ( 漢字 )があれば通常通り表示し、なければemptyを表示
        if(!personNameIsBlank(name)){
            //名前 ( 漢字 )において、姓のみ、名のみの可能性を考慮している
            name = name.replace(nameSplit,' ')
            nameTextView?.text = if(name[0] == ' '){
                name.replace(" ","")
            }else{
                name
            }

            phoneticNameTextView?.text = person.phoneticName.replace(nameSplit,' ')
            phoneticNameTextView?.setTextColor(getColor(this,R.color.textColor))
        }else{
            nameTextView?.text = person.phoneticName.replace(nameSplit,' ')
            phoneticNameTextView?.text = getString(R.string.empty)
            phoneticNameTextView?.setTextColor(getColor(this,R.color.emptyTextColor))
        }

        //組織名があれば通常通り表示し、なければemptyを表示
        val organizationNameTextView = item.findViewById<TextView>(R.id.organizationNameTextView)
        if(person.organizationName.isNotEmpty()){
            organizationNameTextView?.text = person.organizationName
            organizationNameTextView?.setTextColor(getColor(this,R.color.textColor))
        }else{
            organizationNameTextView?.text = getString(R.string.empty)
            organizationNameTextView?.setTextColor(getColor(this,R.color.emptyTextColor))
        }

        val sexTextView = item.findViewById<TextView>(R.id.sexTextView)
        sexTextView?.text = sexTypes[person.sex]
        when(person.sex){
            Sex.NOT_KNOWN -> { sexTextView?.setBackgroundResource(R.color.notKnownBackgroundColor) }
            Sex.MALE -> { sexTextView?.setBackgroundResource(R.color.maleBackgroundColor) }
            Sex.FEMALE -> { sexTextView?.setBackgroundResource(R.color.femaleBackgroundColor) }
        }
    }

    /*
    * 人物リストビューのアイテム、クリックリスナ
    *
    * 写真一覧画面に遷移する
    * */
    private inner class ItemOnClickListener:View.OnClickListener{
        override fun onClick(v: View?) {
            v?:return
            val view = v as ConstraintLayout

            //人物を選択中は遷移しない
            if(removePersons.isNotEmpty()){
                view.performLongClick()
            }else {
                val personId = view.tag.toString().toInt()
                val intent =
                    Intent(this@SearchPersonViewActivity, PhotosViewActivity::class.java)
                intent.putExtra("PERSON_ID", personId)
                startActivityForResult(intent, NO_MEANS_REQUEST_CODE)
            }
        }
    }

    /*
    * 選択項目数を表示
    * */
    private fun displaySelectedItemCount(){
        selectedItemCountTextView.text = removePersons.count().toString()
            .plus(getString(R.string.selected_person_count_text))
    }
    /*
    * 人物リストビューのアイテム、ロングクリックリスナ
    *
    * 削除対象人物リストに追加・削除する
    * */
    private inner class ItemOnLongClickListener:View.OnLongClickListener{
        override fun onLongClick(v: View?): Boolean {
            v?:return false

            val view = v as ConstraintLayout
            val personId = view.tag.toString().toInt()
            val checkedImageView = view.findViewById<ImageView>(R.id.checkedImageView)

            if(checkedImageView.visibility == View.GONE){
                checkedImageView.visibility = View.VISIBLE
                removePersons.add(personId)
            }else if(checkedImageView.visibility == View.VISIBLE){
                checkedImageView.visibility = View.GONE
                removePersons.remove(personId)
            }

            displaySelectedItemCount()

            if(removePersons.isNotEmpty()){
                footerOptionBar.visibility = View.VISIBLE
                addPersonButton.visibility = View.INVISIBLE
                searchButton.setBackgroundResource(R.drawable.disabled_search_button_background)
            }else{
                footerOptionBar.visibility = View.INVISIBLE
                addPersonButton.visibility = View.VISIBLE
                searchButton.setBackgroundResource(R.drawable.search_button_background)
            }

            return true
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

    private var currentJapaneseSyllabaryRegex = "" //現在のア段正規表現
    private var prevPhoneticNameFirstChar = "" //前回のふりがな先頭1文字
    /*
    * リストビューに人物リストをまとめて追加
    *
    * @param 人物リスト
    * @return 追加件数
    * */
    private fun addPersonsToListView(persons:MutableList<Person>):Int{
        if(persons.count() == 0){
            //ローディング画面(view)を削除
            if(0 < personListLinearLayout.childCount) {
                personListLinearLayout.removeViewAt(personListLinearLayout.childCount - 1)
            }

            if(0 < prevAdditionalSearchPersonsCount) {
                //一番下に余白を作る
                this.layoutInflater.inflate(
                    R.layout.person_listview_empty_item,
                    personListLinearLayout
                )
            }

            return 0
        }

        //追加する前に前回追加したローディング画面(view)を一旦削除
        if(0 < personListLinearLayout.childCount) {
            personListLinearLayout.removeViewAt(personListLinearLayout.childCount - 1)
        }

        val japaneseSyllabaryRegex = resources.getStringArray(R.array.japanese_syllabary_regex) //50音のア段正規表現　どの列に属するかを判定するため
        val japaneseSyllabaryText = resources.getStringArray(R.array.japanese_syllabary_text) //セパレーターに表示するア段文字列

        persons.forEachIndexed {
            i,person ->

            if(limit <= i)
                return@forEachIndexed

            val phoneticNameFirstChar = person.phoneticName[0].toString()

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
        }

        //まだ、追加検索する必要がある場合はローディング画面を下に配置
        if(persons.count() == limit) {
            this.layoutInflater.inflate(
                R.layout.person_listview_loading_item,
                personListLinearLayout
            )
        }else{
            //一番下に余白を作る
            this.layoutInflater.inflate(R.layout.person_listview_empty_item, personListLinearLayout)
        }

        return persons.count()
    }

    /*
    * 検索SQLをテスト1
    *
    * 全検索
    * */
    @Test
    fun testCreateSearchSql01(){
        val sql = "SELECT ${BaseColumns._ID}," +
                "${DbContracts.Persons.COLUMN_NAME}," +
                "${DbContracts.Persons.COLUMN_PHONETIC_NAME}," +
                "${DbContracts.Persons.COLUMN_SEX}," +
                "${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}," +
                DbContracts.Persons.COLUMN_NOTE +
                " FROM ${DbContracts.Persons.TABLE_NAME}" +
                " ORDER BY ${DbContracts.Persons.COLUMN_PHONETIC_NAME}" +
                " LIMIT $limit OFFSET $offset"
        assertEquals(sql,createSearchSql(""))
    }

    /*
    * 検索SQLをテスト2
    *
    * キーワードが平仮名のみの場合、カタカナに変換して名前(フリガナ)で検索する
    * 組織名はキーワードをそのまま使って検索する
    * */
    @Test
    fun testCreateSearchSql02(){
        val sql = "SELECT ${BaseColumns._ID}," +
                "${DbContracts.Persons.COLUMN_NAME}," +
                "${DbContracts.Persons.COLUMN_PHONETIC_NAME}," +
                "${DbContracts.Persons.COLUMN_SEX}," +
                "${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}," +
                DbContracts.Persons.COLUMN_NOTE +
                " FROM ${DbContracts.Persons.TABLE_NAME}" +
                " WHERE ${DbContracts.Persons.COLUMN_PHONETIC_NAME} LIKE '%コスガ%'" +
                " OR ${DbContracts.Persons.COLUMN_ORGANIZATION_NAME} LIKE '%こすが%'" +
                " ORDER BY ${DbContracts.Persons.COLUMN_PHONETIC_NAME}" +
                " LIMIT $limit OFFSET $offset"
        assertEquals(sql,createSearchSql("こすが"))
    }

    /*
    * 検索SQLをテスト3
    *
    * キーワードに漢字含まれる場合、名前(漢字)で検索し
    * 組織名も検索する
    * */
    @Test
    fun testCreateSearchSql03(){
        val sql = "SELECT ${BaseColumns._ID}," +
                "${DbContracts.Persons.COLUMN_NAME}," +
                "${DbContracts.Persons.COLUMN_PHONETIC_NAME}," +
                "${DbContracts.Persons.COLUMN_SEX}," +
                "${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}," +
                DbContracts.Persons.COLUMN_NOTE +
                " FROM ${DbContracts.Persons.TABLE_NAME}" +
                " WHERE ${DbContracts.Persons.COLUMN_NAME} LIKE '%たかはし工業%'" +
                " OR ${DbContracts.Persons.COLUMN_ORGANIZATION_NAME} LIKE '%たかはし工業%'" +
                " ORDER BY ${DbContracts.Persons.COLUMN_PHONETIC_NAME}" +
                " LIMIT $limit OFFSET $offset"
        assertEquals(sql,createSearchSql("たかはし工業"))
    }

    /*
    * 人物名の空判定をテスト
    *
    * 人物名はカンマで姓と名を区切っており、カンマだけの場合空と見なしている
    * */
    @Test
    fun testPersonNameIsBlank(){
        assertEquals(true,personNameIsBlank(","))
        assertEquals(false,personNameIsBlank("たかはし,"))
        assertEquals(false,personNameIsBlank(",ゆうき"))
        assertEquals(false,personNameIsBlank("たかはし,ゆうき"))
    }
}

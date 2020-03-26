package com.example.meishizukan.activity

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.BaseColumns
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.meishizukan.R
import com.example.meishizukan.dto.Photo
import com.example.meishizukan.util.BitmapUtils.convertBinaryToBitmap
import com.example.meishizukan.util.DateUtils.getDay
import com.example.meishizukan.util.DateUtils.getMonth
import com.example.meishizukan.util.DateUtils.getYear
import com.example.meishizukan.util.DbContracts
import com.example.meishizukan.util.DbHelper
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.android.synthetic.main.activity_all_photos_view.*
import androidx.core.content.ContextCompat.getColor

private const val GET_PHOTOS_IN_APP_REQUEST_CODE = 2

private const val DISPLAY_TYPE_ALL = 0
private const val DISPLAY_TYPE_YEAR = 1
private const val DISPLAY_TYPE_MONTH = 2
private const val DISPLAY_TYPE_DAY = 3

private const val DISPLAYED_PLACE_LEFT = 0
private const val DISPLAYED_PLACE_MIDDLE = 1
private const val DISPLAYED_PLACE_RIGHT = 2

class AllPhotosViewActivity : AppCompatActivity() {

    private val dbHelper = DbHelper(this)
    private lateinit var readableDB:SQLiteDatabase

    private var isSelecting = false //選択中かいなか

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_photos_view)

        readableDB = dbHelper.readableDatabase

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
        adView.adListener = object: AdListener() {
            override fun onAdLoaded() {}
            override fun onAdFailedToLoad(errorCode : Int) {}
            override fun onAdOpened() {}
            override fun onAdClicked() {}
            override fun onAdLeftApplication() {}
            override fun onAdClosed() {}
        }

        val requestCode = intent.getIntExtra("REQUEST_CODE",0)

        //すべて表示する
        if(requestCode == GET_PHOTOS_IN_APP_REQUEST_CODE){ //のちにトップの写真一覧画面が実装されるため分岐がある
            searchHandler.post{
                changeActiveDisplayTypeButton()
                search()
            }
        }

        //すべて表示する
        allButton.setOnClickListener{
            if(displayType == DISPLAY_TYPE_ALL){
                return@setOnClickListener
            }

            searchHandler.post{
                displayType = DISPLAY_TYPE_ALL
                changeActiveDisplayTypeButton()
                search()
            }
        }

        //年別表示する
        yearButton.setOnClickListener{
            if(displayType == DISPLAY_TYPE_YEAR){
                return@setOnClickListener
            }

            searchHandler.post{
                displayType = DISPLAY_TYPE_YEAR
                changeActiveDisplayTypeButton()
                search()
            }
        }

        //月別表示する
        monthButton.setOnClickListener{
            if(displayType == DISPLAY_TYPE_MONTH){
                return@setOnClickListener
            }

            searchHandler.post{
                displayType = DISPLAY_TYPE_MONTH
                changeActiveDisplayTypeButton()
                search()
            }
        }

        //日別表示する
        dayButton.setOnClickListener{
            if(displayType == DISPLAY_TYPE_DAY){
                return@setOnClickListener
            }

            searchHandler.post{
                displayType = DISPLAY_TYPE_DAY
                changeActiveDisplayTypeButton()
                search()
            }
        }

        //スクロールビューを先頭にスクロールする
        headerMenu.setOnClickListener{
            scrollToTop(true)
        }

        //前の画面に戻る
        backButton.setOnClickListener{
            super.onBackPressed()
        }

        //写真を選択・選択解除する
        selectButton.setOnClickListener{
            if(isSelecting){
                //チェックボタンを非表示
                selectedPhotos.keys.forEach{
                    val imageView = findViewById<ImageView>(it)
                    val parent = imageView.parent as ConstraintLayout
                    val checkedImageView = parent.findViewById<ImageView>(R.id.checkedImageView)
                    checkedImageView.visibility = View.INVISIBLE
                }

                selectedPhotos.clear()

                selectButton.text = getString(R.string.select_button_text)
                footerOptionBar.visibility = View.INVISIBLE
                footerMenu.visibility = View.VISIBLE
            }else{
                selectButton.text = getString(R.string.select_button_cancel_text)
                displaySelectedPhotoCount()
                footerMenu.visibility = View.INVISIBLE
                footerOptionBar.visibility = View.VISIBLE
            }
            isSelecting = !isSelecting
        }

        //写真IDを保持し、アクティビティを終了する
        finishButton.setOnClickListener{
            if(selectedPhotos.isEmpty()){
                return@setOnClickListener
            }

            val intent = Intent()
            intent.putExtra("SELECTED_PHOTOS_ID",selectedPhotos.values.toIntArray())
            setResult(Activity.RESULT_OK,intent)
            finish()
        }
    }

    override fun onResume(){
        super.onResume()
        adView.resume()
    }

    override fun onDestroy() {
        adView.destroy()
        readableDB.close()
        dbHelper.close()
        super.onDestroy()
    }

    /*
    * ローディング画面を表示
    * */
    private fun showLoadingDialog(){
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) //タッチ無効化
        loadingDialogView.bringToFront()
        loadingDialogView.visibility = View.VISIBLE
    }

    /*
    * ローディング画面を非表示
    * */
    private fun hideLoadingDialog(){
        rootConstraintLayout.bringToFront()
        loadingDialogView.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) //タッチ無効化解除
    }

    /*
    * 一番上までスクロール
    *
    * @param スムーズなスクロールをするか
    * */
    private fun scrollToTop(smooth:Boolean){
        if(smooth) {
            photoListScrollView.smoothScrollTo(0, 0)
        }else{
            photoListScrollView.scrollTo(0, 0)
        }
    }

    /*
    * 一番下にスクロール
    *
    * @param スムーズなスクロールをするか
    * */
    private fun scrollToBottom(smooth:Boolean){
        photoListScrollView.postDelayed( {
            if (smooth) {
                photoListScrollView.smoothScrollTo(0, photoListLinearLayout.bottom)
            } else {
                photoListScrollView.scrollTo(0, photoListLinearLayout.bottom)
            }

            hideLoadingDialog()
        },300L)
    }

    /*
    * 写真リストを取得
    *
    * @param 実行するSQL
    * @return 写真リスト
    * */
    private fun readPhotos(sql:String):MutableList<Photo>{
        val cursor = readableDB.rawQuery(sql,null)
        val photos = mutableListOf<Photo>()

        if(cursor.count == 0){
            cursor.close()
            return photos
        }

        while(cursor.moveToNext()){
            photos.add(Photo(
                id = cursor.getInt(0),
                hashedBinary = cursor.getString(1).toByteArray(),
                binary = cursor.getBlob(2),
                createdOn = cursor.getString(3)
            ))
        }

        cursor.close()

        return photos
    }

    /*
    * 検索SQLを作成
    *
    * @return 検索SQL
    * */
    private fun createSearchSQL():String{
        return "SELECT ${BaseColumns._ID}," +
                    "${DbContracts.Photos.COLUMN_HASHED_BINARY}," +
                    "${DbContracts.Photos.COLUMN_BINARY}," +
                    DbContracts.Photos.COLUMN_CREATED_ON +
                    " FROM ${DbContracts.Photos.TABLE_NAME}" +
                    " ORDER BY ${DbContracts.Photos.COLUMN_CREATED_ON}"
    }

    private val searchHandler = Handler()
    private var prevDisplayedPhotoDate = "0000-00-00" //前回表示した写真の追加日付
    /*
    * 写真を検索
    * */
    private fun search(){
        showLoadingDialog()

        searchHandler.postDelayed({
            prevDisplayedPhotoDate = "0000-00-00"
            displayedPhotoCount = 0
            prevDisplayedPlace = DISPLAYED_PLACE_RIGHT

            photoListLinearLayout.removeAllViews() //写真アイテムを全てクリア

            val sql = createSearchSQL()

            val photos = readPhotos(sql)
            photos.forEach {
                displayPhoto(it)
            }

            displayPhotoCount()

            scrollToBottom(false)

        },500)
    }

    /*
    * リストビューにセパレーターを追加
    *
    * @param セパレーターテキスト
    * */
    private fun addSeparatorToListView(separatorText:String){
        this.layoutInflater.inflate(
            R.layout.photo_listview_separator,
            photoListLinearLayout
        )
        val separator =
            photoListLinearLayout.getChildAt(photoListLinearLayout.childCount - 1) as ConstraintLayout

        val separatorTextView =
            separator.findViewById<TextView>(R.id.separatorTextView)
        separatorTextView?.text = separatorText
    }

    private var displayedPhotoCount = 0
    private var displayType = DISPLAY_TYPE_ALL
    private var prevDisplayedPlace = DISPLAYED_PLACE_RIGHT
    /*
    * 写真をリストビューに表示
    *
    * @param 写真
    * */
    private fun displayPhoto(photo: Photo){
        //表示形式がすべて以外且つ前回表示した写真の追加日付と異なる場合、セパレータを追加する
        //前回表示した写真の追加日付と異なる場合
        val separatorText = when(displayType){
            DISPLAY_TYPE_YEAR -> {
                if(getYear(prevDisplayedPhotoDate) != getYear(photo.getCreatedOn())){
                    prevDisplayedPhotoDate = photo.getCreatedOn()

                    "${getYear(photo.getCreatedOn())}年"
                }
                else null
            }
            DISPLAY_TYPE_MONTH -> {
                if(getMonth(prevDisplayedPhotoDate) != getMonth(photo.getCreatedOn())){
                    prevDisplayedPhotoDate = photo.getCreatedOn()

                    "${getYear(photo.getCreatedOn())}年${getMonth(photo.getCreatedOn())}月"
                }
                else null
            }
            DISPLAY_TYPE_DAY -> {
                if(getDay(prevDisplayedPhotoDate) != getDay(photo.getCreatedOn())){
                    prevDisplayedPhotoDate = photo.getCreatedOn()

                    "${getYear(photo.getCreatedOn())}年${getMonth(photo.getCreatedOn())}月" +
                            "${getDay(photo.getCreatedOn())}日"
                }
                else null
            }
            else -> null
        }

        if(separatorText != null){
            addSeparatorToListView(separatorText)
            prevDisplayedPlace = DISPLAYED_PLACE_RIGHT
        }

        if(prevDisplayedPlace == DISPLAYED_PLACE_RIGHT){
            this.layoutInflater.inflate(R.layout.photo_listview_item,photoListLinearLayout)
        }

        val photoListViewItem = photoListLinearLayout.getChildAt(photoListLinearLayout.childCount - 1)

        val photoImageView: ImageView? = when(prevDisplayedPlace){
            DISPLAYED_PLACE_RIGHT -> {
                prevDisplayedPlace = DISPLAYED_PLACE_LEFT

                val leftConstraintLayout = photoListViewItem.findViewById<ConstraintLayout>(R.id.leftConstraintLayout)
                leftConstraintLayout.findViewById(R.id.photoImageView)
            }
            DISPLAYED_PLACE_LEFT -> {
                prevDisplayedPlace = DISPLAYED_PLACE_MIDDLE

                val middleConstraintLayout = photoListViewItem.findViewById<ConstraintLayout>(R.id.middleConstraintLayout)
                middleConstraintLayout.findViewById(R.id.photoImageView)
            }
            DISPLAYED_PLACE_MIDDLE -> {
                prevDisplayedPlace = DISPLAYED_PLACE_RIGHT

                val rightConstraintLayout = photoListViewItem.findViewById<ConstraintLayout>(R.id.rightConstraintLayout)
                rightConstraintLayout.findViewById(R.id.photoImageView)
            }
            else -> {
                null
            }
        }

        photoImageView?:return

        val bitmap = convertBinaryToBitmap(photo.getBinary())
        photoImageView.setImageBitmap(bitmap)
        photoImageView.setOnClickListener(ItemOnClickListener())

        //選択、選択解除の処理で使う
        photoImageView.id = View.generateViewId()
        photoImageView.tag = photo.getId().toString()

        displayedPhotoCount++
    }

    /*
    * 写真数を表示
    * */
    private fun displayPhotoCount(){
        this.layoutInflater.inflate(R.layout.photo_listview_item_count_textview,photoListLinearLayout)
        val photoCountTextViewLayout = photoListLinearLayout.getChildAt(photoListLinearLayout.childCount - 1) as ConstraintLayout
        val photoCountTextView = photoCountTextViewLayout.getChildAt(0) as TextView
        photoCountTextView.text = "写真 : ".plus(displayedPhotoCount).plus("枚")
    }

    /*
    * 写真リストビューのアイテム、クリックリスナ
    * */
    private inner class ItemOnClickListener:View.OnClickListener{
        override fun onClick(v: View?) {
            if(!isSelecting) {
                return
            }

            v?:return

            val imageView = v as ImageView
            val parent = imageView.parent as ConstraintLayout
            val checkedImageView = parent.findViewById<ImageView>(R.id.checkedImageView)

            val photoImageViewId = imageView.id
            val photoId = imageView.tag.toString().toInt()
            if(selectedPhotos.keys.contains(photoImageViewId)){
                selectedPhotos.remove(photoImageViewId)
                checkedImageView.visibility = View.INVISIBLE
            }else {
                selectedPhotos.put(photoImageViewId,photoId)
                checkedImageView.visibility = View.VISIBLE
            }

            if(selectedPhotos.isEmpty()){
                finishButton.setBackgroundResource(R.drawable.disabled_save_button_background)
            }else{
                finishButton.setBackgroundResource(R.drawable.save_button_background)
            }

            displaySelectedPhotoCount()
        }
    }

    /*
    * アクティブな表示形式ボタンを変更する
    * */
    private fun changeActiveDisplayTypeButton(){
        allButton.setBackgroundResource(R.drawable.inactive_footer_menu_button_background)
        allButtonLabel.setTextColor(getColor(this,R.color.textColor))
        yearButton.setBackgroundResource(R.drawable.inactive_footer_menu_button_background)
        yearButtonLabel.setTextColor(getColor(this,R.color.textColor))
        monthButton.setBackgroundResource(R.drawable.inactive_footer_menu_button_background)
        monthButtonLabel.setTextColor(getColor(this,R.color.textColor))
        dayButton.setBackgroundResource(R.drawable.inactive_footer_menu_button_background)
        dayButtonLabel.setTextColor(getColor(this,R.color.textColor))

        when(displayType){
            DISPLAY_TYPE_ALL -> {
                allButton.setBackgroundResource(R.drawable.active_footer_menu_button_background)
                allButtonLabel.setTextColor(getColor(this,R.color.activeButtonTextColor))
            }
            DISPLAY_TYPE_YEAR -> {
                yearButton.setBackgroundResource(R.drawable.active_footer_menu_button_background)
                yearButtonLabel.setTextColor(getColor(this,R.color.activeButtonTextColor))
            }
            DISPLAY_TYPE_MONTH -> {
                monthButton.setBackgroundResource(R.drawable.active_footer_menu_button_background)
                monthButtonLabel.setTextColor(getColor(this,R.color.activeButtonTextColor))
            }
            DISPLAY_TYPE_DAY -> {
                dayButton.setBackgroundResource(R.drawable.active_footer_menu_button_background)
                dayButtonLabel.setTextColor(getColor(this,R.color.activeButtonTextColor))
            }
        }
    }

    private val selectedPhotos = hashMapOf<Int,Int>() //選択された写真のリスト Key:写真イメージビューID, Value:写真ID
    /*
    * 選択された写真の枚数を表示
    * */
    private fun displaySelectedPhotoCount(){
        selectedItemCountTextView.text = selectedPhotos.count().toString().plus(getString(R.string.selected_photo_count_text))
    }
}

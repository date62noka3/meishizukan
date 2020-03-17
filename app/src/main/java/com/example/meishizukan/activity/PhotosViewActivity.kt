package com.example.meishizukan.activity

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.meishizukan.R
import com.example.meishizukan.dto.Person
import com.example.meishizukan.util.DbContracts
import com.example.meishizukan.util.DbHelper
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_photos_view.*
import androidx.core.content.ContextCompat.getColor
import androidx.core.graphics.drawable.toBitmap
import com.example.meishizukan.dto.Photo
import com.example.meishizukan.dto.PhotoLink
import com.example.meishizukan.util.BitmapUtils
import com.example.meishizukan.util.BitmapUtils.rotateBitmap
import com.example.meishizukan.util.OnSwipeGestureListener
import com.google.android.gms.ads.RequestConfiguration
import java.security.MessageDigest

private const val OPEN_CAMERA_REQUEST_CODE  = 0

class PhotosViewActivity : AppCompatActivity() {

    private var personId = 0
    private val newPhotoId = 0
    private val newPhotoLinkId = 0

    private var displayedPhotoImageViewIdOnFullScreen = -1 //全画面表示されている写真イメージビューID
    private val rotateLeftAngle = -90F //全画面表示で画像を回転させるときの回転角度(左回転)
    private val rotateRightAngle = 90F //全画面表示で画像を回転させるときの回転角度(右回転)

    private val dbHelper = DbHelper(this)
    private lateinit var readableDB: SQLiteDatabase
    private lateinit var writableDB: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photos_view)

        readableDB = dbHelper.readableDatabase
        writableDB = dbHelper.writableDatabase

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

        backButton.setOnClickListener{
            super.onBackPressed()
        }

        backgroundOnOpenedSelection.setOnClickListener{
            hideAddPhotoButtons()
        }

        addPhotoButton.setOnClickListener{
            if(addPhotoButtonsLinearLayout.visibility == View.INVISIBLE) {
                showAddPhotoButtons()
            }else{
                hideAddPhotoButtons()
            }
        }

        cameraButton.setOnClickListener{
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if(intent.resolveActivity(packageManager) != null){
                startActivityForResult(intent,OPEN_CAMERA_REQUEST_CODE)
            }
        }

        galleryButton.setOnClickListener{

        }

        addedPhotosButton.setOnClickListener{

        }

        closeButton.setOnClickListener{
            hideFullScreenView()
        }

        fullScreenView.setOnClickListener{
            if(fullScreenViewHeaderOptionBar.visibility == View.VISIBLE){
                hideFullScreenViewOptionBar()
            }else{
                showFullScreenViewOptionBar()
            }
        }

        /*
        * 全画面表示のスワイプリスナ
        *
        * 右にスワイプで次の写真を表示する
        * 左にスワイプで前の写真を表示する
        * */
        class SwipeListener:OnSwipeGestureListener(){
            override fun onSwipeBottom() {}
            override fun onSwipeTop() {}
            override fun onSwipeLeft() {
                val nextPhotoImageViewId = getNextPhotoImageViewId()
                if(nextPhotoImageViewId == -1){
                    return
                }
                val nextPhotoImageView = findViewById<ImageView>(nextPhotoImageViewId)
                fullScreenViewPhotoImageView.setImageDrawable(nextPhotoImageView.drawable)

                val currentPage = displayedPhotoImageViews.indexOf(nextPhotoImageViewId) + 1
                displayPageNumber(currentPage)

                displayedPhotoImageViewIdOnFullScreen = nextPhotoImageViewId
            }
            override fun onSwipeRight() {
                val prevPhotoImageViewId = getPrevPhotoImageViewId()
                if(prevPhotoImageViewId == -1) {
                    return
                }
                val prevPhotoImageView = findViewById<ImageView>(prevPhotoImageViewId)
                fullScreenViewPhotoImageView.setImageDrawable(prevPhotoImageView.drawable)

                val currentPage = displayedPhotoImageViews.indexOf(prevPhotoImageViewId) + 1
                displayPageNumber(currentPage)

                displayedPhotoImageViewIdOnFullScreen = prevPhotoImageViewId
            }
        }
        val gestureDetector = GestureDetector(this,SwipeListener())
        fullScreenView.setOnTouchListener { v, event -> gestureDetector.onTouchEvent(event) }

        rotateLeftButton.setOnClickListener{
            val rotatedBitmap = rotateBitmap(fullScreenViewPhotoImageView.drawable.toBitmap(),rotateLeftAngle)
            fullScreenViewPhotoImageView.setImageBitmap(rotatedBitmap)
        }

        rotateRightButton.setOnClickListener{
            val rotatedBitmap = rotateBitmap(fullScreenViewPhotoImageView.drawable.toBitmap(),rotateRightAngle)
            fullScreenViewPhotoImageView.setImageBitmap(rotatedBitmap)
        }

        fullScreenViewDownloadButton.setOnClickListener{
            
        }

        personId = intent.getIntExtra("PERSON_ID",0)

        personalInfoViewButton.setOnClickListener{
            val intent = Intent(this,PersonalInfoViewActivity::class.java)
            intent.putExtra("PERSON_ID",personId)
            startActivity(intent)
            finish()
        }

        val person = loadPersonalInfo(personId)
        if(person != null) {
            setPersonalInfoToTextView(person)

            //人物にリンクしている写真をリストビューに表示
            val allLinkedPhotosCursor = searchAllLinkedPhotos()
            if(allLinkedPhotosCursor.count == 0){
                displayPhotoCount()
                return
            }

            while(allLinkedPhotosCursor.moveToNext()){
                val photoId = allLinkedPhotosCursor.getInt(0)

                val linkedPhotoCursor = searchPhoto(photoId)
                if(linkedPhotoCursor.count == 0){
                    return
                }

                linkedPhotoCursor.moveToNext()
                val binary = linkedPhotoCursor.getBlob(1)
                val bitmap = BitmapUtils.convertBinaryToBitmap(binary)
                displayPhoto(bitmap)
            }
        }
    }

    override fun onResume(){
        super.onResume()
        adView.resume()
    }

    override fun onDestroy(){
        adView.destroy()
        readableDB.close()
        writableDB.close()
        super.onDestroy()
    }

    private val messageDigest = MessageDigest.getInstance("MD5")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == OPEN_CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null){
            data.extras?:return
            val data = data.extras.get("data")
            data?:return
            val bitmap = data as Bitmap
            val binary = BitmapUtils.convertBitmapToBinaryJPEG(bitmap)
            val hashedBinary = messageDigest.digest(binary)

            val cursor = searchPhoto(hashedBinary)

            val photoId = if(cursor.count == 0){
                val photo = Photo(
                    id = newPhotoId,
                    hashedBinary = hashedBinary,
                    binary = binary,
                    createdOn = "" //DBの方でCURRENT_DATEが設定される
                )

                addPhoto(photo)
            }else{
                cursor.moveToNext()

                //既存写真のIDを取得
                cursor.getInt(0)
            }
            cursor.close()

            val photoLink = PhotoLink(
                id = newPhotoLinkId,
                photoId = photoId,
                personId = personId
            )

            addPhotoLink(photoLink)

            displayPhoto(bitmap)
        }
    }

    /*
    * 写真の値セットを取得
    *
    * @param 写真
    * @return 写真の値セット
    * */
    private fun getContentValues(photo: Photo):ContentValues{
        return ContentValues().apply {
            put(DbContracts.Photos.COLUMN_HASHED_BINARY,photo.getHashedBinary())
            put(DbContracts.Photos.COLUMN_BINARY,photo.getBinary())
        }
    }

    /*
    * 写真リンクの値セットを取得
    *
    * @param 写真リンク
    * @return 写真リンクの値セット
    * */
    private fun getContentValues(photoLink: PhotoLink):ContentValues{
        return ContentValues().apply {
            put(DbContracts.PhotosLinks.COLUMN_PHOTO_ID,photoLink.getPhotoId())
            put(DbContracts.PhotosLinks.COLUMN_PERSON_ID,photoLink.getPersonId())
        }
    }

    /*
    * 写真をDBで検索
    * (写真追加処理で既存するものかを確認している)
    *
    * @param ハッシュ化したバイナリ
    * @return 検索結果カーソル
    * */
    private fun searchPhoto(hashedBinary:ByteArray): Cursor {
        val sql = "SELECT ${BaseColumns._ID} FROM ${DbContracts.Photos.TABLE_NAME}" +
                " WHERE ${DbContracts.Photos.COLUMN_HASHED_BINARY} = '$hashedBinary'"
        return readableDB.rawQuery(sql,null)
    }

    /*
    * 写真をDBで検索
    * (リンクしている写真を一覧表示する際に使う)
    *
    * @param 写真ID
    * @return 検索結果カーソル
    * */
    private fun searchPhoto(photoId: Int):Cursor{
        val sql = "SELECT ${BaseColumns._ID}," +
                DbContracts.Photos.COLUMN_BINARY +
                " FROM ${DbContracts.Photos.TABLE_NAME}" +
                " WHERE ${BaseColumns._ID} = $photoId"
        return readableDB.rawQuery(sql,null)
    }

    /*
    * 人物にリンクしている写真をすべて検索
    *
    * @return 検索結果カーソル
    * */
    private fun searchAllLinkedPhotos():Cursor{
        val sql = "SELECT ${BaseColumns._ID}," +
                DbContracts.PhotosLinks.COLUMN_PHOTO_ID +
                " FROM ${DbContracts.PhotosLinks.TABLE_NAME}" +
                " WHERE ${DbContracts.PhotosLinks.COLUMN_PERSON_ID} = $personId"
        return readableDB.rawQuery(sql,null)
    }

    /*
    * 写真をDBに追加
    *
    * @param 写真
    * @return 写真ID
    * */
    private fun addPhoto(photo:Photo):Int{
        val values = getContentValues(photo)

        return writableDB.insert(DbContracts.Photos.TABLE_NAME,null,values).toInt()
    }

    /*
    * 写真リンクをDBに追加
    *
    * @param 写真リンク
    * */
    private fun addPhotoLink(photoLink: PhotoLink){
        val values = getContentValues(photoLink)

        writableDB.insert(DbContracts.PhotosLinks.TABLE_NAME,null,values)
    }

    private val displayedPhotoImageViews = mutableListOf<Int>() //値は写真イメージビューのビューID

    /*
    * 全画面表示モードで使う次の写真イメージビューのIDを取得
    *
    * @return 次の写真イメージビューのID
    * */
    private fun getNextPhotoImageViewId():Int{
        val nextIndex = displayedPhotoImageViews.indexOf(displayedPhotoImageViewIdOnFullScreen) + 1
        return if(nextIndex < displayedPhotoImageViews.count()){
            displayedPhotoImageViews[nextIndex]
        }else{
            -1
        }
    }

    /*
    * 全画面表示モードで使う前の写真イメージビューのIDを取得
    *
    * @return 前の写真イメージビューのID
    * */
    private fun getPrevPhotoImageViewId():Int{
        val prevIndex = displayedPhotoImageViews.indexOf(displayedPhotoImageViewIdOnFullScreen) - 1
        return if(0 <= prevIndex){
            displayedPhotoImageViews[prevIndex]
        }else{
            -1
        }
    }

    private val itemCountPerLine = 3 //1行当たりのアイテム数
    /*
    * 写真をリストビューに表示
    *
    * @param ビットマップ
    * */
    private fun displayPhoto(bitmap: Bitmap){
        if(displayedPhotoImageViews.count() % itemCountPerLine == 0){
            this.layoutInflater.inflate(R.layout.photo_listview_item,photoListLinearLayout)
        }

        val photoListViewItem = photoListLinearLayout.getChildAt(photoListLinearLayout.childCount - 1)

        val photoImageView:ImageView? = when(displayedPhotoImageViews.count() % itemCountPerLine){
            0 -> {
                val leftConstraintLayout = photoListViewItem.findViewById<ConstraintLayout>(R.id.leftConstraintLayout)
                leftConstraintLayout.findViewById(R.id.photoImageView)
            }
            1 -> {
                val middleConstraintLayout = photoListViewItem.findViewById<ConstraintLayout>(R.id.middleConstraintLayout)
                middleConstraintLayout.findViewById(R.id.photoImageView)
            }
            2 -> {
                val rightConstraintLayout = photoListViewItem.findViewById<ConstraintLayout>(R.id.rightConstraintLayout)
                rightConstraintLayout.findViewById(R.id.photoImageView)
            }
            else -> {
                null
            }
        }

        photoImageView?:return

        photoImageView.setImageBitmap(bitmap)
        photoImageView.setOnClickListener(ItemOnClickListener())
        photoImageView.setOnLongClickListener(ItemOnLongClickListener())

        photoImageView.id = View.generateViewId() //一意のIDを付与
        displayedPhotoImageViews.add(photoImageView.id)
        displayPhotoCount()
    }

    /*
    * 写真数を表示
    * */
    private fun displayPhotoCount(){
        photoCountTextView.text = displayedPhotoImageViews.count().toString().plus("件")
    }

    /*
    * ページ番号を表示
    *
    * @param 現在ページ
    * */
    private fun displayPageNumber(currentPage:Int){
        pageNumberTextView.text = currentPage.toString().plus(" / ").plus(displayedPhotoImageViews.count())
    }

    /*
    * 写真を全画面表示
    *
    * @param 表示する写真
    * */
    private fun displayPhotoByFullScreen(drawable: Drawable){
        fullScreenViewPhotoImageView.setImageDrawable(drawable)
    }

    /*
    * 写真リストビューのアイテム、クリックリスナ
    * */
    private inner class ItemOnClickListener:View.OnClickListener{
        override fun onClick(v: View?) {
            v?:return
            val imageView = v as ImageView
            val currentPage = displayedPhotoImageViews.indexOf(imageView.id) + 1
            displayPageNumber(currentPage)
            displayPhotoByFullScreen(imageView.drawable)
            displayedPhotoImageViewIdOnFullScreen = imageView.id
            showFullScreenView()
        }
    }

    /*
    * 写真リストビューのアイテム、ロングクリックリスナ
    * */
    private inner class ItemOnLongClickListener:View.OnLongClickListener{
        override fun onLongClick(v: View?): Boolean {
            return true
        }
    }

    /*
    * 人物情報を読み込む
    *
    * @param 人物ID
    * */
    private fun loadPersonalInfo(personId: Int): Person?{
        val sql = "SELECT ${DbContracts.Persons.COLUMN_NAME}," +
                "${DbContracts.Persons.COLUMN_PHONETIC_NAME}," +
                "${DbContracts.Persons.COLUMN_SEX}," +
                "${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}," +
                DbContracts.Persons.COLUMN_NOTE +
                " FROM ${DbContracts.Persons.TABLE_NAME}" +
                " WHERE ${BaseColumns._ID} = $personId"

        //人物を取得
        val cursor = readableDB.rawQuery(sql,null)

        if(cursor.count == 0){
            cursor.close()
            return null
        }

        cursor.moveToNext()

        val person = Person(
            id = personId,
            name = cursor.getString(0),
            phoneticName = cursor.getString(1),
            sex = cursor.getInt(2),
            organizationName = cursor.getString(3),
            note = cursor.getString(4)
        )

        cursor.close()

        return person
    }

    /*
    * 名前が空か否かを取得
    *
    * @param 名前
    * @return 名前が空か否か
    * */
    private fun nameIsEmpty(name:String):Boolean{
        return name == nameSplit.toString()
    }

    private val nameSplit = ','
    /*
    * 人物情報をテキストビューにセット
    *
    * @param 人物
    * */
    private fun setPersonalInfoToTextView(person:Person){
        var name = person.getName()
        //名前 ( 漢字 )があれば通常通り表示し、なければemptyを表示
        if(!nameIsEmpty(name)){
            //名前 ( 漢字 )において、姓のみ、名のみの可能性を考慮している
            name = name.replace(nameSplit,' ')
            nameTextView.text = if(name[0] == ' '){
                name.replace(" ","")
            }else{
                name
            }

            phoneticNameTextView.text = person.getPhoneticName().replace(nameSplit,' ')
            phoneticNameTextView.setTextColor(getColor(this,R.color.textColor))
        }else{
            nameTextView.text = person.getPhoneticName().replace(nameSplit,' ')
            phoneticNameTextView.text = getString(R.string.empty)
            phoneticNameTextView.setTextColor(getColor(this,R.color.emptyTextColor))
        }

        fullScreenViewNameTextView.text = nameTextView.text
    }

    /*
    * 写真追加ボタンを表示
    * */
    private fun showAddPhotoButtons(){
        addPhotoButton.clearAnimation()
        addPhotoButtonsLinearLayout.clearAnimation()

        backgroundOnOpenedSelection.visibility = View.VISIBLE
        addPhotoButtonsLinearLayout.visibility = View.VISIBLE

        val rotateCloseButtonAnimation = AnimationUtils.loadAnimation(this,R.anim.rotate_close_button)
        rotateCloseButtonAnimation.setAnimationListener(object:Animation.AnimationListener{
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                addPhotoButton.setBackgroundResource(R.drawable.add_photo_cancel_button_background)
            }
        })
        addPhotoButton.startAnimation(rotateCloseButtonAnimation)
        addPhotoButtonsLinearLayout.startAnimation(AnimationUtils.loadAnimation(this,R.anim.show_add_photo_buttons))
    }

    /*
    * 写真追加ボタンを非表示
    * */
    private fun hideAddPhotoButtons(){
        addPhotoButton.clearAnimation()
        addPhotoButtonsLinearLayout.clearAnimation()

        val rotateCloseButtonAnimation = AnimationUtils.loadAnimation(this,R.anim.rotate_close_button)
        rotateCloseButtonAnimation.setAnimationListener(object:Animation.AnimationListener{
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                addPhotoButton.setBackgroundResource(R.drawable.add_button_background)
            }
        })
        addPhotoButton.startAnimation(rotateCloseButtonAnimation)
        addPhotoButtonsLinearLayout.startAnimation(AnimationUtils.loadAnimation(this,R.anim.hide_add_photo_buttons))

        backgroundOnOpenedSelection.visibility = View.INVISIBLE
        addPhotoButtonsLinearLayout.visibility = View.INVISIBLE
    }

    /*
    * 全画面表示ビューを表示
    * */
    private fun showFullScreenView(){
        fullScreenView.clearAnimation()

        fullScreenView.visibility = View.VISIBLE

        val showFullScreenViewAnimation = AnimationUtils.loadAnimation(this,R.anim.show_full_screen_view)
        fullScreenView.startAnimation(showFullScreenViewAnimation)
    }

    /*
    * 全画面表示ビューを非表示
    * */
    private fun hideFullScreenView(){
        fullScreenView.clearAnimation()

        val hideFullScreenViewAnimation = AnimationUtils.loadAnimation(this,R.anim.hide_full_screen_view)
        hideFullScreenViewAnimation.setAnimationListener(object:Animation.AnimationListener{
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                fullScreenView.visibility = View.INVISIBLE
            }
        })
        fullScreenView.startAnimation(hideFullScreenViewAnimation)
    }

    /*
    * 全画面表示ビューのオプションバーを表示
    * */
    private fun showFullScreenViewOptionBar(){
        fullScreenViewHeaderOptionBar.visibility = View.VISIBLE
        fullScreenViewFooterOptionBar.visibility = View.VISIBLE
    }

    /*
    * 全画面表示ビューのオプションバーを非表示
    * */
    private fun hideFullScreenViewOptionBar(){
        fullScreenViewHeaderOptionBar.visibility = View.INVISIBLE
        fullScreenViewFooterOptionBar.visibility = View.INVISIBLE
    }
}

package com.example.meishizukan.activity

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
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
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.meishizukan.R
import com.example.meishizukan.dto.Person
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_photos_view.*
import androidx.core.content.ContextCompat.getColor
import androidx.core.graphics.drawable.toBitmap
import com.example.meishizukan.dto.Photo
import com.example.meishizukan.dto.PhotoLink
import com.example.meishizukan.util.*
import com.example.meishizukan.util.BitmapUtils.convertBinaryToBitmap
import com.example.meishizukan.util.BitmapUtils.convertBitmapToBinaryJPEG
import com.example.meishizukan.util.BitmapUtils.convertBitmapToBinaryPNG
import com.example.meishizukan.util.BitmapUtils.getBitmapFromUri
import com.example.meishizukan.util.BitmapUtils.rotateBitmap
import com.example.meishizukan.util.BitmapUtils.saveBitmapToGallery
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.android.synthetic.main.activity_photos_view.adView
import kotlinx.android.synthetic.main.activity_photos_view.backButton
import kotlinx.android.synthetic.main.activity_photos_view.photoListLinearLayout
import java.lang.StringBuilder
import java.security.MessageDigest

private const val OPEN_CAMERA_REQUEST_CODE  = 0
private const val OPEN_GALLERY_REQUEST_CODE = 1
private const val GET_PHOTOS_IN_APP_REQUEST_CODE = 2

private const val WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 3 //ストレージ書き込み権限の要求コード

class PhotosViewActivity : AppCompatActivity() {

    private var personId = 0
    private val newPhotoId = 0
    private val newPhotoLinkId = 0

    private var displayedPhotoImageViewIdOnFullScreen = -1 //全画面表示されている写真イメージビューID
    private val rotateLeftAngle = -90F //全画面表示で画像を回転させるときの回転角度(左回転)
    private val rotateRightAngle = 90F //全画面表示で画像を回転させるときの回転角度(右回転)

    private var isSelecting = false //選択中かいなか

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
            override fun onAdLoaded() {
                photoListLinearLayout.setPadding(0,0,0,180)
            }
            override fun onAdFailedToLoad(errorCode : Int) {}
            override fun onAdOpened() {}
            override fun onAdClicked() {}
            override fun onAdLeftApplication() {}
            override fun onAdClosed() {}
        }

        //前の画面(人物検索画面)に戻る
        backButton.setOnClickListener{
            super.onBackPressed()
        }

        //写真追加ボタンを非表示
        backgroundOnOpenedSelection.setOnClickListener{
            hideAddPhotoButtons()
        }

        //写真追加ボタンを表示
        addPhotoButton.setOnClickListener{
            if(addPhotoButtonsLinearLayout.visibility == View.INVISIBLE) {
                showAddPhotoButtons()
            }else{
                hideAddPhotoButtons()
            }
        }

        //カメラを起動
        cameraButton.setOnClickListener{
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if(intent.resolveActivity(packageManager) != null){
                startActivityForResult(intent,OPEN_CAMERA_REQUEST_CODE)
            }
        }

        //ギャラリー選択画面を表示
        galleryButton.setOnClickListener{
            val intent = Intent()
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(intent, OPEN_GALLERY_REQUEST_CODE)
        }

        //アプリ内写真選択画面を表示
        addedPhotosButton.setOnClickListener{
            val intent = Intent(this,AllPhotosViewActivity::class.java)
            intent.putExtra("REQUEST_CODE", GET_PHOTOS_IN_APP_REQUEST_CODE)
            startActivityForResult(intent, GET_PHOTOS_IN_APP_REQUEST_CODE)
        }

        //全画面表示をやめる処理
        closeButton.setOnClickListener{
            hideFullScreenView()
        }

        //全画面表示処理
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

        //全画面表示時の写真回転処理(左90度)
        rotateLeftButton.setOnClickListener{
            val rotatedBitmap = rotateBitmap(fullScreenViewPhotoImageView.drawable.toBitmap(),rotateLeftAngle)
            fullScreenViewPhotoImageView.setImageBitmap(rotatedBitmap)
        }

        //全画面表示時の写真回転処理(右90度)
        rotateRightButton.setOnClickListener{
            val rotatedBitmap = rotateBitmap(fullScreenViewPhotoImageView.drawable.toBitmap(),rotateRightAngle)
            fullScreenViewPhotoImageView.setImageBitmap(rotatedBitmap)
        }

        //全画面表示時のダウンロード処理
        fullScreenViewDownloadButton.setOnClickListener{
            //権限がなければ要求
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE)
                return@setOnClickListener
            }

            val displayedPhotoImageView =
                findViewById<ImageView>(displayedPhotoImageViewIdOnFullScreen)
            downloadPhoto(displayedPhotoImageView.drawable.toBitmap())
        }

        //写真リストビューを一番上までスクロールする
        headerMenu.setOnClickListener{
            scrollToTop(true)
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

        personId = intent.getIntExtra("PERSON_ID",0)

        //人物情報画面に遷移する
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
                val binary = linkedPhotoCursor.getBlob(0)
                val photo = Photo(
                    id = photoId,
                    hashedBinary = "".toByteArray(),
                    binary = binary,
                    createdOn = ""
                )
                displayPhoto(photo)
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
        dbHelper.close()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                val displayedPhotoImageView =
                    findViewById<ImageView>(displayedPhotoImageViewIdOnFullScreen)
                downloadPhoto(displayedPhotoImageView.drawable.toBitmap())
            } else {
                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_failed_saved_photos),
                    textColor = getColor(this, R.color.toastTextColorOnFailed),
                    backgroundColor = getColor(this, R.color.toastBackgroundColorOnFailed),
                    displayTime = Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private val messageDigest = MessageDigest.getInstance("SHA-256")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //写真追加ボタンを非表示
        if(requestCode == OPEN_CAMERA_REQUEST_CODE || requestCode == OPEN_GALLERY_REQUEST_CODE
            || requestCode == GET_PHOTOS_IN_APP_REQUEST_CODE){
            hideAddPhotoButtons()
        }

        if(requestCode == OPEN_CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK
            && data != null){
            data.extras?:return
            val data = data.extras.get("data")
            data?:return
            val bitmap = data as Bitmap
            val binary = convertBitmapToBinaryJPEG(bitmap)
            val photoId = addPhoto(binary)

            if(-1 == photoId){
                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_not_added_photos),
                    textColor = getColor(this, R.color.toastTextColorOnFailed),
                    backgroundColor = getColor(this, R.color.toastBackgroundColorOnFailed),
                    displayTime = Toast.LENGTH_LONG
                ).show()
                return
            }

            val photo = Photo(
                id = photoId,
                hashedBinary = "".toByteArray(),
                binary = binary,
                createdOn = ""
            )
            displayPhoto(photo)

            Toaster.createToast(
                context = this,
                text = getString(R.string.message_on_added_photos),
                textColor = getColor(this, R.color.toastTextColorOnSuccess),
                backgroundColor = getColor(this, R.color.toastBackgroundColorOnSuccess),
                displayTime = Toast.LENGTH_LONG
            ).show()
        }

        if(requestCode == OPEN_GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK
            && data != null){
            if(data.clipData != null) { //複数選択の場合
                var addedPhotosCount = 0
                for (i in 0 until data.clipData.itemCount) {
                    val bitmap = getBitmapFromUri(this,data.clipData.getItemAt(i).uri)
                    bitmap?:return
                    val binary = convertBitmapToBinaryPNG(bitmap)
                    val photoId = addPhoto(binary)
                    if(-1 != photoId){
                        val photo = Photo(
                            id = photoId,
                            hashedBinary = "".toByteArray(),
                            binary = binary,
                            createdOn = ""
                        )
                        displayPhoto(photo)
                        addedPhotosCount++
                    }
                }

                if(0 == addedPhotosCount){
                    Toaster.createToast(
                        context = this,
                        text = getString(R.string.message_on_not_added_photos),
                        textColor = getColor(this, R.color.toastTextColorOnFailed),
                        backgroundColor = getColor(this, R.color.toastBackgroundColorOnFailed),
                        displayTime = Toast.LENGTH_LONG
                    ).show()
                    return
                }

                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_added_photos),
                    textColor = getColor(this, R.color.toastTextColorOnSuccess),
                    backgroundColor = getColor(this, R.color.toastBackgroundColorOnSuccess),
                    displayTime = Toast.LENGTH_LONG
                ).show()
            }else if(data.data != null){ //1枚のみ選択の場合
                val bitmap = getBitmapFromUri(this, Uri.parse(data.data.toString()))
                bitmap?:return

                val binary = convertBitmapToBinaryPNG(bitmap)
                val photoId = addPhoto(binary)

                if(-1 == photoId) {
                    Toaster.createToast(
                        context = this,
                        text = getString(R.string.message_on_not_added_photos),
                        textColor = getColor(this, R.color.toastTextColorOnFailed),
                        backgroundColor = getColor(this, R.color.toastBackgroundColorOnFailed),
                        displayTime = Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val photo = Photo(
                    id = photoId,
                    hashedBinary = "".toByteArray(),
                    binary = binary,
                    createdOn = ""
                )
                displayPhoto(photo)

                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_added_photos),
                    textColor = getColor(this, R.color.toastTextColorOnSuccess),
                    backgroundColor = getColor(this, R.color.toastBackgroundColorOnSuccess),
                    displayTime = Toast.LENGTH_LONG
                ).show()
            }
        }

        if(requestCode == GET_PHOTOS_IN_APP_REQUEST_CODE && resultCode == Activity.RESULT_OK
            && data != null){
            val selectedPhotos = data.getIntArrayExtra("SELECTED_PHOTOS_ID").toMutableList()

            //選択された写真の中で未リンクのものを取得
            val getLinkedPhotosSql = StringBuilder()
            getLinkedPhotosSql.append("SELECT DISTINCT ${DbContracts.PhotosLinks.COLUMN_PHOTO_ID} FROM ${DbContracts.PhotosLinks.TABLE_NAME}" +
                    " WHERE ${DbContracts.PhotosLinks.COLUMN_PERSON_ID} = $personId" +
                    " AND ${DbContracts.PhotosLinks.COLUMN_PHOTO_ID} IN (")
            selectedPhotos.forEach {
                photoId ->
                getLinkedPhotosSql.append("$photoId,")
            }
            getLinkedPhotosSql.replace(getLinkedPhotosSql.length - 1,getLinkedPhotosSql.length,"") //末尾のカンマを削除
            getLinkedPhotosSql.append(")")

            val getLinkedPhotosCursor = readableDB.rawQuery(getLinkedPhotosSql.toString(),null)

            if(getLinkedPhotosCursor.count != 0) {
                //既にリンクしている写真をリストから除外する
                while (getLinkedPhotosCursor.moveToNext()) {
                    val photoId = getLinkedPhotosCursor.getInt(0)
                    selectedPhotos.remove(photoId)
                }
            }
            getLinkedPhotosCursor.close()

            if(selectedPhotos.count() == 0){
                //全て既に追加されている
                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_not_added_photos),
                    textColor = getColor(this, R.color.toastTextColorOnFailed),
                    backgroundColor = getColor(this, R.color.toastBackgroundColorOnFailed),
                    displayTime = Toast.LENGTH_LONG
                ).show()
                getLinkedPhotosCursor.close()
                return
            }

            //画面に表示するためにビットマップを取得する
            val getBinarySql = StringBuilder()
            getBinarySql.append("SELECT DISTINCT ${BaseColumns._ID}," +
                    DbContracts.Photos.COLUMN_BINARY +
                    " FROM ${DbContracts.Photos.TABLE_NAME}" +
                    " WHERE ${BaseColumns._ID} IN (")

            //選択された写真と人物を紐づける
            selectedPhotos.forEach{
                photoId ->
                val photoLink = PhotoLink(
                    id = newPhotoLinkId,
                    photoId = photoId,
                    personId = personId
                )
                val values = getContentValues(photoLink)
                writableDB.insert(DbContracts.PhotosLinks.TABLE_NAME,null,values)

                getBinarySql.append("$photoId,")
            }

            getBinarySql.replace(getBinarySql.length - 1,getBinarySql.length,"") //末尾のカンマを削除する
            getBinarySql.append(") ORDER BY ${BaseColumns._ID}")

            val getBinaryCursor = readableDB.rawQuery(getBinarySql.toString(),null)
            if(getBinaryCursor.count != 0){
                while(getBinaryCursor.moveToNext()){
                    val photoId = getBinaryCursor.getInt(0)
                    val binary = getBinaryCursor.getBlob(1)
                    val photo = Photo(
                        id = photoId,
                        hashedBinary = "".toByteArray(),
                        binary = binary,
                        createdOn = ""
                    )
                    displayPhoto(photo)
                }
            }
            getBinaryCursor.close()

            Toaster.createToast(
                context = this,
                text = getString(R.string.message_on_added_photos),
                textColor = getColor(this, R.color.toastTextColorOnSuccess),
                backgroundColor = getColor(this, R.color.toastBackgroundColorOnSuccess),
                displayTime = Toast.LENGTH_LONG
            ).show()
        }
    }

    /*
    * 写真を追加
    *
    * 写真がDBに既存している場合はリンクするだけで
    * 写真が存在していない場合はphotosテーブルに新規レコード追加し、
    * 新規レコードとリンクさせる。
    * 既にリンクされている場合はリンクせずに返す。
    *
    * @param バイナリ
    * @return 追加件数(追加:写真ID,追加しなかった:-1)
    * */
    private fun addPhoto(binary:ByteArray):Int{
        val hashedBinary = messageDigest.digest(binary)

        val cursor = searchPhoto(hashedBinary)

        val photoId = if(cursor.count == 0){
            val photo = Photo(
                id = newPhotoId,
                hashedBinary = hashedBinary,
                binary = binary,
                createdOn = "" //DBの方でCURRENT_DATEが設定される
            )

            insertPhoto(photo)
        }else{
            cursor.moveToNext()

            //既存写真のIDを取得
            cursor.getInt(0)
        }
        cursor.close()

        if(isPhotoLinked(photoId,personId)){
            return -1
        }

        val photoLink = PhotoLink(
            id = newPhotoLinkId,
            photoId = photoId,
            personId = personId
        )

        addPhotoLink(photoLink)

        return photoId
    }

    /*
    * 写真をダウンロード
    *
    * @param ビットマップ
    * */
    private fun downloadPhoto(bitmap: Bitmap){
        val savedBitmapUri = saveBitmapToGallery(this, bitmap)
        Log.d("SAVED_BITMAP_URI",savedBitmapUri.toString())

        Toaster.createToast(
            context = this,
            text = getString(R.string.message_on_saved_photos),
            textColor = getColor(this, R.color.toastTextColorOnSuccess),
            backgroundColor = getColor(this, R.color.toastBackgroundColorOnSuccess),
            displayTime = Toast.LENGTH_LONG
        ).show()
    }

    /*
    * 写真の値セットを取得
    *
    * @param 写真
    * @return 写真の値セット
    * */
    private fun getContentValues(photo: Photo):ContentValues{
        return ContentValues().apply {
            put(DbContracts.Photos.COLUMN_HASHED_BINARY,photo.getHashedBinary().contentToString())
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
                " WHERE ${DbContracts.Photos.COLUMN_HASHED_BINARY} = '${hashedBinary.contentToString()}'"
        return readableDB.rawQuery(sql,null)
    }

    /*
    * 指定写真が既にリンクされているか否か
    *
    * @param 写真ID
    * @param 人物ID
    * @return 指定写真リンクがDBに存在するか否か
    * */
    private fun isPhotoLinked(photoId:Int, personId: Int):Boolean{
        val sql = "SELECT ${BaseColumns._ID} FROM ${DbContracts.PhotosLinks.TABLE_NAME}" +
                " WHERE ${DbContracts.PhotosLinks.COLUMN_PHOTO_ID} = $photoId" +
                " AND ${DbContracts.PhotosLinks.COLUMN_PERSON_ID} = $personId"
        val cursor = readableDB.rawQuery(sql,null)
        val recordCount:Int = cursor.count
        cursor.close()
        return 0 != recordCount
    }

    /*
    * 写真をDBで検索
    * (リンクしている写真を一覧表示する際に使う)
    *
    * @param 写真ID
    * @return 検索結果カーソル
    * */
    private fun searchPhoto(photoId: Int):Cursor{
        val sql = "SELECT ${DbContracts.Photos.COLUMN_BINARY}" +
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
        val sql = "SELECT ${DbContracts.PhotosLinks.COLUMN_PHOTO_ID}" +
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
    private fun insertPhoto(photo:Photo):Int{
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
    * @param 写真
    * */
    private fun displayPhoto(photo: Photo){
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

        val bitmap = convertBinaryToBitmap(photo.getBinary())
        photoImageView.setImageBitmap(bitmap)
        photoImageView.setOnClickListener(ItemOnClickListener())
        photoImageView.tag = photo.getId().toString()
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

            //選択中はアイテムの選択・選択解除を処理
            //選択中でなければ全画面表示処理
            if(isSelecting) {
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

                displaySelectedPhotoCount()
            }else {
                val imageView = v as ImageView
                val currentPage = displayedPhotoImageViews.indexOf(imageView.id) + 1
                displayPageNumber(currentPage)
                displayPhotoByFullScreen(imageView.drawable)
                displayedPhotoImageViewIdOnFullScreen = imageView.id
                showFullScreenView()
            }
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

    private val selectedPhotos = hashMapOf<Int,Int>() //選択された写真のリスト Key:写真イメージビューID, Value:写真ID
    /*
    * 選択された写真の枚数を表示
    * */
    private fun displaySelectedPhotoCount(){
        selectedItemCountTextView.text = selectedPhotos.count().toString().plus(getString(R.string.selected_photo_count_text))
    }
}

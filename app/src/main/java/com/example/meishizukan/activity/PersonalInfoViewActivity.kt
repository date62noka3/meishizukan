package com.example.meishizukan.activity

import NoFilterArrayAdapter
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.database.sqlite.SQLiteDatabase
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getColor
import com.example.meishizukan.R
import com.example.meishizukan.dto.Person
import com.example.meishizukan.util.*
import com.example.meishizukan.util.PhoneticName.hiraganaToKatakana
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import junit.framework.TestCase.assertEquals
import kotlinx.android.synthetic.main.activity_personal_info_view.*
import org.junit.Test

class PersonalInfoViewActivity : AppCompatActivity() {

    private val newPersonId = 0

    private var personId = -1
    private var valueChangedElements = mutableListOf<Int>() //値が変更された人物情報要素

    private val dbHelper = DbHelper(this)
    private lateinit var readableDb:SQLiteDatabase
    private lateinit var writableDb:SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info_view)

        readableDb = dbHelper.readableDatabase
        writableDb = dbHelper.writableDatabase

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

        //前の画面(人物検索画面)に戻る
        backButton.setOnClickListener{
            onBackPressed()
        }

        personId = intent.getIntExtra("PERSON_ID",newPersonId)

        //写真画面に遷移
        photosViewButton.setOnClickListener {
            if (valueChangedElements.count() == 0) {
                val intent = Intent(this, PhotosViewActivity::class.java)
                intent.putExtra("PERSON_ID", personId)
                startActivity(intent)
                finish()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_dialog_title))
                .setMessage(getString(R.string.message_on_confirm_transit))
                .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                    val intent = Intent(this, PhotosViewActivity::class.java)
                    intent.putExtra("PERSON_ID", personId)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton(getString(R.string.negative_button_text)) { _, _ -> }
                .setCancelable(false)
                .show()
        }

        if(isNewPerson(personId)){ //新規
            disablePhotosViewButton()
            disableDeleteButton()
        }else{ //編集
            val person = loadPersonalInfo(personId)
            if(person != null) {
                setPersonalInfoToInputFields(person)
            }
        }

        setInputValueToTag()

        //テキストの変更を判定するウォッチャーを設定
        firstPhoneticNameEditText.addTextChangedListener(PersonalInfoEditTextWatcher(firstPhoneticNameEditText))
        lastPhoneticNameEditText.addTextChangedListener(PersonalInfoEditTextWatcher(lastPhoneticNameEditText))
        firstNameEditText.addTextChangedListener(PersonalInfoEditTextWatcher(firstNameEditText))
        lastNameEditText.addTextChangedListener(PersonalInfoEditTextWatcher(lastNameEditText))
        organizationNameEditText.addTextChangedListener(PersonalInfoEditTextWatcher(organizationNameEditText))
        noteEditText.addTextChangedListener(PersonalInfoEditTextWatcher(noteEditText))

        //性別の変更を判定
        sexSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                //重複を無くすため削除
                valueChangedElements.remove(sexSpinner.id)

                if(sexSpinner.tag.toString() != position.toString()){
                    valueChangedElements.add(sexSpinner.id)
                    sexSpinner.setBackgroundResource(R.drawable.value_changed_input_field_background)
                }else{
                    sexSpinner.setBackgroundResource(R.drawable.input_field_background)
                }

                onInputValueChanged()
            }
        }

        //組織名入力欄に入力補完用Adapterを設定
        val organizations = mutableListOf<String>()
        val organizationNameArrayAdapter = NoFilterArrayAdapter(this,android.R.layout.simple_list_item_1,organizations)
        organizationNameEditText.setAdapter(organizationNameArrayAdapter)

        //組織名入力欄の入力値が変わった時、入力補完用Adapterを更新する
        organizationNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val organizationName = organizationNameEditText.text.toString()

                if(organizationName.isBlank()){
                    return
                }

                organizationNameArrayAdapter.clear()

                //組織を検索
                organizationNameArrayAdapter.addAll(searchOrganization(organizationName))
            }
        })

        var isKeyboardShown = false
        fun onKeyboardVisibilityChanged() {
            if(!isKeyboardShown) {
                val focusView = personalInfoConstraintLayout.findFocus()
                focusView.clearFocus() //selectAllOnFocusを走らせるため
            }
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

        val inputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        //人物情報を保存する
        saveButton.setOnClickListener{
            //未入力チェック
            if(isRequiredFieldsBlank()){
                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_blank_error),
                    textColor = getColor(this,R.color.toastTextColorOnFailed),
                    backgroundColor = getColor(this,R.color.toastBackgroundColorOnFailed),
                    displayTime = Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            //フリガナ正規表現チェック
            if(!isPhoneticName()){
                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_illegal_error),
                    textColor = getColor(this,R.color.toastTextColorOnFailed),
                    backgroundColor = getColor(this,R.color.toastBackgroundColorOnFailed),
                    displayTime = Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            //人物インスタンス生成
            val person = createPersonFromInputValues()

            if(isNewPerson(personId)){ //新規追加
                personId = insertPerson(person) //人物を追加
                Log.d("ADDED_PERSON_ID",personId.toString())

                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_saved_new_person),
                    textColor = getColor(this,R.color.toastTextColorOnSuccess),
                    backgroundColor = getColor(this,R.color.toastBackgroundColorOnSuccess),
                    displayTime = Toast.LENGTH_SHORT
                ).show()

                inputMethodManager.hideSoftInputFromWindow(saveButton.windowToken,0) //キーボードを非表示
                enablePhotosViewButton()
                enableDeleteButton()
                scrollToBottom()
            }else{ //編集
                updatePerson(person) //人物情報を更新

                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_updated_personal_info),
                    textColor = getColor(this,R.color.toastTextColorOnSuccess),
                    backgroundColor = getColor(this,R.color.toastBackgroundColorOnSuccess),
                    displayTime = Toast.LENGTH_SHORT
                ).show()
            }

            inputMethodManager.hideSoftInputFromWindow(saveButton.windowToken,0) //キーボードを非表示
            valueChangedElements.clear()
            saveButton.isClickable = false
            setSaveButtonBackground()
            resetEditTextBackground()

            setInputValueToTag()
        }

        //人物を削除する
        deleteButton.setOnClickListener{
            val positiveButtonText = getString(R.string.positive_button_text)
            val negativeButtonText = getString(R.string.negative_button_text)
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_dialog_title))
                .setMessage(getString(R.string.message_on_confirm_delete_person))
                .setPositiveButton(positiveButtonText) { _, _ ->
                    deleteLinkedPhotos(personId)
                    deletePerson(personId)
                    Log.d("DELETED_PERSON_ID",personId.toString())

                    //トップ画面に遷移
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.information_dialog_title))
                        .setMessage(getString(R.string.message_on_deleted_persons))
                        .setPositiveButton(positiveButtonText) { _, _ ->
                            super.onBackPressed()
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
                .setNegativeButton(negativeButtonText) { _, _ -> }
                .setCancelable(false)
                .show()
        }
    }

    override fun onResume(){
        super.onResume()
        adView.resume()
    }

    override fun onDestroy(){
        super.onDestroy()
        adView.destroy()
        readableDb.close()
        writableDb.close()
        dbHelper.close()
    }

    override fun onBackPressed() {
        if(valueChangedElements.count() == 0){
            super.onBackPressed()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_dialog_title))
            .setMessage(getString(R.string.message_on_confirm_transit))
            .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton(getString(R.string.negative_button_text)) { _, _ -> }
            .setCancelable(false)
            .show()
    }

    /*
    * 入力値変更時イベント
    * */
    private fun onInputValueChanged(){
        //入力値の変更があればボタンを有効、なければ無効
        saveButton.isClickable = valueChangedElements.isNotEmpty()

        setSaveButtonBackground()
    }

    /*
    * 保存ボタンの背景を設定
    * */
    private fun setSaveButtonBackground(){
        if(saveButton.isClickable){
            saveButton.setBackgroundResource(R.drawable.save_button_background)
        }else{
            saveButton.setBackgroundResource(R.drawable.disabled_save_button_background)
        }
    }

    /*
    * 一番下にスクロール
    * */
    private fun scrollToBottom(){
        personalInfoScrollView.smoothScrollTo(0,personalInfoConstraintLayout.bottom)
    }

    private val limit = 5 //負荷が大きいため候補を制限する
    /*
    * 組織検索SQLを作成
    *
    * @param 組織名
    * @return SQL
    * */
    private fun createSearchOrganizationSql(organizationName: String):String{
        //OrderBy句の説明
        //検索組織名の文字数 / ヒットした組織名の文字数　で一致率を算出し降順に並び替えている
        return "SELECT DISTINCT ${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}" +
                " FROM ${DbContracts.Persons.TABLE_NAME}" +
                " WHERE ${DbContracts.Persons.COLUMN_ORGANIZATION_NAME} LIKE '%$organizationName%'" +
                " ORDER BY CAST(LENGTH('$organizationName') as REAL) / CAST(LENGTH(${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}) as REAL) DESC" +
                " LIMIT $limit"
    }

    /*
    * 組織を検索
    *
    * @param 組織名
    * @return 組織リスト
    * */
    private fun searchOrganization(organizationName:String):List<String>{
        val sql = createSearchOrganizationSql(organizationName)

        val cursor = readableDb.rawQuery(sql,null)

        val organizations = mutableListOf<String>()

        if(cursor.count == 0){
            cursor.close()
            return organizations
        }

        while(cursor.moveToNext()){
            organizations.add(cursor.getString(0))
            Log.d("HIT_ORGANIZATION_NAME",cursor.getString(0))
        }
        cursor.close()

        return organizations.toList()
    }

    /*
    * 写真画面ボタンを有効化
    * */
    private fun enablePhotosViewButton(){
        val color = getColor(this,R.color.enabledButtonTextColor)
        photosLabel.setTextColor(color)
        photosViewButton.isClickable = true
    }

    /*
    * 写真画面ボタンを無効化
    * */
    private fun disablePhotosViewButton(){
        val color = getColor(this,R.color.disabledButtonTextColor)
        photosLabel.setTextColor(color)
        photosViewButton.isClickable = false
    }

    /*
    * 削除ボタンを有効化
    * */
    private fun enableDeleteButton(){
        deleteButton.setImageResource(R.drawable.delete_button)
        deleteButton.isClickable = true
    }

    /*
    * 削除ボタンを無効化
    * */
    private fun disableDeleteButton(){
        deleteButton.setImageResource(R.drawable.disabled_delete_button)
        deleteButton.isClickable = false
    }

    /*
    * 新規追加か否かを取得
    *
    * @param 人物ID
    * @return 新規追加か否か
    * */
    private fun isNewPerson(personId:Int):Boolean{
        return newPersonId == personId
    }

    /*
    * 必須項目が未入力か否かを取得
    *
    * @return 必須項目が未入力か否か
    * */
    private fun isRequiredFieldsBlank():Boolean{
        return (firstPhoneticNameEditText.text.isBlank() || lastPhoneticNameEditText.text.isBlank())
    }

    /*
    * フリガナ項目の入力値がフリガナか否かを取得
    *
    * @return フリガナ項目の入力値がフリガナか否か
    * */
    private fun isPhoneticName():Boolean{
        return (firstPhoneticNameEditText.text.matches(PhoneticName.phoneticNameRegex)
                && lastPhoneticNameEditText.text.matches(PhoneticName.phoneticNameRegex))
    }

    private val nameSplit = ','
    /*
    * 入力値から人物インスタンスを生成
    *
    * @return 人物インスタンス
    * */
    private fun createPersonFromInputValues():Person{
        //入力された人物情報を取得
        val name = firstNameEditText.text.toString()
            .plus(nameSplit)
            .plus(lastNameEditText.text.toString())
        var phoneticName = firstPhoneticNameEditText.text.toString()
            .plus(nameSplit)
            .plus(lastPhoneticNameEditText.text.toString())
        phoneticName = hiraganaToKatakana(phoneticName)
        val sex = sexSpinner.selectedItemPosition
        val organizationName = organizationNameEditText.text.toString()
        val note = noteEditText.text.toString()

        //人物インスタンスを生成
        return Person(
            id = personId,
            name = name,
            phoneticName = phoneticName,
            sex = sex,
            organizationName = organizationName,
            note = note
        )
    }

    /*
    * 入力欄の背景をリセット
    * */
    private fun resetEditTextBackground(){
        firstPhoneticNameEditText.setBackgroundResource(R.drawable.input_field_background)
        lastPhoneticNameEditText.setBackgroundResource(R.drawable.input_field_background)
        firstNameEditText.setBackgroundResource(R.drawable.input_field_background)
        lastNameEditText.setBackgroundResource(R.drawable.input_field_background)
        sexSpinner.setBackgroundResource(R.drawable.input_field_background)
        organizationNameEditText.setBackgroundResource(R.drawable.input_field_background)
        noteEditText.setBackgroundResource(R.drawable.input_field_background)
    }

    /*
    * 人物情報を入力欄にセット
    *
    * @param 人物
    * */
    private fun setPersonalInfoToInputFields(person:Person){
        //名前、フリガナは半角スペースで区切って姓、名を取得する
        val phoneticName = person.phoneticName.split(nameSplit)
        firstPhoneticNameEditText.setText(phoneticName[0])
        lastPhoneticNameEditText.setText(phoneticName[1])

        //名前 ( 漢字 )はない可能性がある
        val name = person.name.split(nameSplit)
        if(0 < name.count()) {
            firstNameEditText.setText(name[0])
        }
        if(1 < name.count()) {
            lastNameEditText.setText(name[1])
        }

        sexSpinner.setSelection(person.sex)
        organizationNameEditText.setText(person.organizationName)
        noteEditText.setText(person.note)
    }

    /*
    * 入力値をタグに設定
    * */
    private fun setInputValueToTag(){
        firstNameEditText.tag = firstNameEditText.text.toString()
        lastNameEditText.tag = lastNameEditText.text.toString()
        firstPhoneticNameEditText.tag = firstPhoneticNameEditText.text.toString()
        lastPhoneticNameEditText.tag = lastPhoneticNameEditText.text.toString()
        sexSpinner.tag = sexSpinner.selectedItemPosition.toString()
        organizationNameEditText.tag = organizationNameEditText.text.toString()
        noteEditText.tag = noteEditText.text.toString()
    }

    /*
    * 人物情報を読み込む
    *
    * @param 人物ID
    * */
    private fun loadPersonalInfo(personId: Int):Person?{
        val sql = "SELECT ${DbContracts.Persons.COLUMN_NAME}," +
                "${DbContracts.Persons.COLUMN_PHONETIC_NAME}," +
                "${DbContracts.Persons.COLUMN_SEX}," +
                "${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}," +
                DbContracts.Persons.COLUMN_NOTE +
                " FROM ${DbContracts.Persons.TABLE_NAME}" +
                " WHERE ${BaseColumns._ID} = $personId"

        //人物を取得
        val cursor = readableDb.rawQuery(sql,null)

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
    * 人物情報の値セットを取得
    *
    * @param 人物
    * @return 人物情報の値セット
    * */
    private fun getContentValues(person:Person):ContentValues{
        return ContentValues().apply {
            put(DbContracts.Persons.COLUMN_NAME, person.name)
            put(DbContracts.Persons.COLUMN_PHONETIC_NAME, person.phoneticName)
            put(DbContracts.Persons.COLUMN_SEX,person.sex)
            put(DbContracts.Persons.COLUMN_ORGANIZATION_NAME,person.organizationName)
            put(DbContracts.Persons.COLUMN_NOTE,person.note)
        }
    }

    /*
    * 人物を追加
    *
    * @param 人物
    * @return 追加した人物のID
    * */
    private fun insertPerson(person: Person):Int{
        val values = getContentValues(person)

        return writableDb.insert(DbContracts.Persons.TABLE_NAME,null,values).toInt()
    }

    /*
    * 人物情報を更新
    *
    * @param 人物
    * */
    private fun updatePerson(person:Person){
        val values = getContentValues(person)
        writableDb.update(DbContracts.Persons.TABLE_NAME,
            values,
            "${BaseColumns._ID} = ${person.id}",
            null)
    }

    /*
    * 人物を削除
    *
    * @param 人物ID
    * */
    private fun deletePerson(personId: Int){
        writableDb.delete(DbContracts.Persons.TABLE_NAME,"${BaseColumns._ID} = $personId",null)
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
    * 人物情報入力欄ウォッチャー
    *
    * @param 人物情報入力欄
    * */
    private inner class PersonalInfoEditTextWatcher(private val view: View):TextWatcher{
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            //重複を無くすため削除
            valueChangedElements.remove(view.id)

            //元のテキストと異なる場合カウントアップ
            //同値の場合カウントダウン
            if(view is EditText){
                //フリガナにおいてはひらがなとカタカナを区別しないためカタカナに揃えたうえで
                //同値かどうかの判定をする
                val viewText = if(view.id == firstPhoneticNameEditText.id || view.id == lastPhoneticNameEditText.id){
                    hiraganaToKatakana(view.text.toString())
                }else{
                    view.text.toString()
                }

                if(view.tag.toString() != viewText){
                    valueChangedElements.add(view.id)
                    view.setBackgroundResource(R.drawable.value_changed_input_field_background)
                }else{
                    view.setBackgroundResource(R.drawable.input_field_background)
                }
            }
            else if(view is AutoCompleteTextView){
                if(view.tag.toString() != view.text.toString()){
                    valueChangedElements.add(view.id)
                    view.setBackgroundResource(R.drawable.value_changed_input_field_background)
                }else{
                    view.setBackgroundResource(R.drawable.input_field_background)
                }
            }

            onInputValueChanged()
        }
    }

    /*
    * 人物が新規かをテスト
    * */
    @Test
    fun testIsNewPerson(){
        assertEquals(true,isNewPerson(newPersonId))
        assertEquals(false,isNewPerson(12))
    }
}

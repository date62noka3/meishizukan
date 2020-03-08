package com.example.meishizukan.activity

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getColor
import com.example.meishizukan.R
import com.example.meishizukan.dto.Person
import com.example.meishizukan.util.DbContracts
import com.example.meishizukan.util.DbHelper
import com.example.meishizukan.util.Modules
import com.example.meishizukan.util.Toaster
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_personal_info_view.*

class PersonalInfoViewActivity : AppCompatActivity() {

    private val newPersonId = 0

    private var personId = -1
    private var valueChangedElements = mutableListOf<Int>() //値が変更された人物情報要素

    private val dbHelper = DbHelper(this)
    private lateinit var readableDB:SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info_view)

        readableDB = dbHelper.readableDatabase

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

        backButton.setOnClickListener{
            onBackPressed()
        }

        //写真画面に遷移
        photosViewButton.setOnClickListener{
            if(valueChangedElements.count() == 0){
                val intent = Intent(this,PhotosViewActivity::class.java)
                intent.putExtra("PERSON_ID",personId)
                startActivity(intent)
                finish()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_dialog_title))
                .setMessage(getString(R.string.confirm_message_on_transit))
                .setPositiveButton(getString(R.string.positive_button_text)) { dialog, which ->
                    val intent = Intent(this,PhotosViewActivity::class.java)
                    intent.putExtra("PERSON_ID",personId)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton(getString(R.string.negative_button_text)) { dialog, which -> }
                .setCancelable(false)
                .show()
        }

        personId = intent.getIntExtra("PERSON_ID",newPersonId)

        if(isNewPerson(personId)){ //新規
            disablePhotosViewButton()
            disableDeleteButton()
        }else{ //編集
            loadPersonalInfo(personId)

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
                        sexSpinner.setBackgroundResource(R.drawable.value_changed_personal_info_edittext_background)
                    }else{
                        sexSpinner.setBackgroundResource(R.drawable.personal_info_edittext_background)
                    }
                }
            }
        }

        //組織名入力欄に入力補完用Adapterを設定
        val organizationNames = mutableListOf<String>()
        val organizationNameArrayAdapter = ArrayAdapter(this,android.R.layout.simple_list_item_1,organizationNames)
        organizationNameEditText.setAdapter(organizationNameArrayAdapter)

        //組織名入力欄の入力値が変わった時、入力補完用Adapterを更新する
        organizationNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                organizationNameArrayAdapter.clear()

                val organizationName = organizationNameEditText.text.toString()
                val sql = "SELECT ${DbContracts.Persons.COLUMN_ORGANIZATION_NAME}" +
                        " FROM ${DbContracts.Persons.TABLE_NAME}" +
                        " WHERE ${DbContracts.Persons.COLUMN_ORGANIZATION_NAME} LIKE '%$organizationName%'"
                val cursor = readableDB.rawQuery(sql,null)

                if(cursor.count == 0){
                    cursor.close()
                    return
                }

                while(cursor.moveToNext()){
                    organizationNameArrayAdapter.add(cursor.getString(0))
                    Log.d("HIT_ORGANIZATION_NAME",cursor.getString(0))
                }
                cursor.close()
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

        saveButton.setOnClickListener{
            //未入力チェック
            if(isRequiredFieldsBlank()){
                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_blank_error),
                    textColor = getColor(this,R.color.textColorOnInputError),
                    backgroundColor = getColor(this,R.color.backgroundColorOnInputError),
                    displayTime = Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            //フリガナ正規表現チェック
            if(!isPhoneticName()){
                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_illegal_error),
                    textColor = getColor(this,R.color.textColorOnInputError),
                    backgroundColor = getColor(this,R.color.backgroundColorOnInputError),
                    displayTime = Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            //人物インスタンス生成
            val person = createPersonFromInputData()

            if(isNewPerson(personId)){ //新規追加
                personId = insertPerson(person) //人物を追加
                Log.d("ADDED_PERSON_ID",personId.toString())

                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_saved_new_person),
                    textColor = getColor(this,R.color.textColorOnSaved),
                    backgroundColor = getColor(this,R.color.backgroundColorOnSaved),
                    displayTime = Toast.LENGTH_SHORT
                ).show()

                enablePhotosViewButton()
                enableDeleteButton()
            }else{ //編集
                updatePerson(person) //人物情報を更新

                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_updated_personal_info),
                    textColor = getColor(this,R.color.textColorOnSaved),
                    backgroundColor = getColor(this,R.color.backgroundColorOnSaved),
                    displayTime = Toast.LENGTH_SHORT
                ).show()
            }

            valueChangedElements.clear()
            resetEditTextBackground()
        }

        deleteButton.setOnClickListener{
            val positiveButtonText = getString(R.string.positive_button_text)
            val negativeButtonText = getString(R.string.negative_button_text)
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_dialog_title))
                .setMessage(getString(R.string.confirm_message_on_delete))
                .setPositiveButton(positiveButtonText) { dialog, which ->
                    deletePerson(personId)
                    Log.d("DELETED_PERSON_ID",personId.toString())

                    //トップ画面に遷移
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.information_dialog_title))
                        .setMessage(getString(R.string.information_message_on_deleted))
                        .setPositiveButton(positiveButtonText) { dialog, which ->
                            super.onBackPressed()
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
                .setNegativeButton(negativeButtonText) { dialog, which ->
                    Toaster.createToast(
                        context = this,
                        text = getString(R.string.message_on_cancel),
                        displayTime = Toast.LENGTH_SHORT
                    ).show()
                }
                .setCancelable(false)
                .show()
        }
    }

    override fun onResume(){
        super.onResume()
        adView.resume()
    }

    override fun onDestroy(){
        adView.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if((isNewPerson(personId) && isAllFieldsBlank()) || valueChangedElements.count() == 0){
            super.onBackPressed()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_dialog_title))
            .setMessage(getString(R.string.confirm_message_on_transit))
            .setPositiveButton(getString(R.string.positive_button_text)) { dialog, which ->
                super.onBackPressed()
            }
            .setNegativeButton(getString(R.string.negative_button_text),{ dialog, which -> })
            .setCancelable(false)
            .show()
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
        deleteButton.setImageResource(R.drawable.delete_button_enabled)
        deleteButton.isClickable = true
    }

    /*
    * 削除ボタンを無効化
    * */
    private fun disableDeleteButton(){
        deleteButton.setImageResource(R.drawable.delete_button_disabled)
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
        return (firstPhoneticNameEditText.text.isBlank() || lastPhoneticNameEditText.text.isBlank()
            || firstNameEditText.text.isBlank() || lastNameEditText.text.isBlank())
    }

    /*
    * 全ての入力欄が未入力か否かを取得
    *
    * @return 全ての入力欄が未入力か否か
    * */
    private fun isAllFieldsBlank():Boolean{
        return (firstPhoneticNameEditText.text.isBlank() && lastPhoneticNameEditText.text.isBlank()
                && firstNameEditText.text.isBlank() && firstPhoneticNameEditText.text.isBlank()
                && organizationNameEditText.text.isBlank() && noteEditText.text.isBlank())
    }

    /*
    * フリガナ項目の入力値がフリガナか否かを取得
    *
    * @return フリガナ項目の入力値がフリガナか否か
    * */
    private fun isPhoneticName():Boolean{
        return (firstPhoneticNameEditText.text.matches(Modules.phoneticNameRegex)
                && lastPhoneticNameEditText.text.matches(Modules.phoneticNameRegex))
    }

    /*
    * 性別文字列を性別値に変換
    *
    * @param 性別文字列
    * @return 性別値
    * */
    private fun convertSexStringToSexNum(sex:String):Int{
        val sexTypes = resources.getStringArray(R.array.sex_types)
        return sexTypes.indexOf(sex)
    }

    /*
    * 入力値から人物インスタンスを生成
    *
    * @return 人物インスタンス
    * */
    private fun createPersonFromInputData():Person{
        //入力された人物情報を取得
        val name = firstNameEditText.text.toString()
            .plus(" ")
            .plus(lastNameEditText.text.toString())
        val phoneticName = firstPhoneticNameEditText.text.toString()
            .plus(" ")
            .plus(lastPhoneticNameEditText.text.toString())
        val sex = convertSexStringToSexNum(sexSpinner.selectedItem.toString())
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
        firstPhoneticNameEditText.setBackgroundResource(R.drawable.personal_info_edittext_background)
        lastPhoneticNameEditText.setBackgroundResource(R.drawable.personal_info_edittext_background)
        firstNameEditText.setBackgroundResource(R.drawable.personal_info_edittext_background)
        lastNameEditText.setBackgroundResource(R.drawable.personal_info_edittext_background)
        sexSpinner.setBackgroundResource(R.drawable.personal_info_edittext_background)
        organizationNameEditText.setBackgroundResource(R.drawable.personal_info_edittext_background)
        noteEditText.setBackgroundResource(R.drawable.personal_info_edittext_background)
    }

    /*
    * 人物情報を読み込む
    *
    * @param 人物ID
    * */
    private fun loadPersonalInfo(personId: Int){
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
            return
        }

        cursor.moveToNext()

        //人物情報を入力欄にセット
        val name = cursor.getString(0).split(' ')
        firstNameEditText.setText(name[0])
        lastNameEditText.setText(name[1])
        val phoneticName = cursor.getString(1).split(' ')
        firstPhoneticNameEditText.setText(phoneticName[0])
        lastPhoneticNameEditText.setText(phoneticName[1])
        val sex = cursor.getInt(2)
        sexSpinner.setSelection(sex)
        val organizationName = cursor.getString(3)
        organizationNameEditText.setText(organizationName)
        val note = cursor.getString(4)
        noteEditText.setText(note)

        //タグに保持
        firstNameEditText.tag = name[0]
        lastNameEditText.tag = name[1]
        firstPhoneticNameEditText.tag = phoneticName[0]
        lastPhoneticNameEditText.tag = phoneticName[1]
        sexSpinner.tag = sex
        organizationNameEditText.tag = organizationName
        noteEditText.tag = note

        cursor.close()
    }

    /*
    * 人物情報の値セットを取得
    *
    * @param 人物
    * @return 人物情報の値セット
    * */
    private fun getContentValues(person:Person):ContentValues{
        return ContentValues().apply {
            put(DbContracts.Persons.COLUMN_NAME, person.getName())
            put(DbContracts.Persons.COLUMN_PHONETIC_NAME, person.getPhoneticName())
            put(DbContracts.Persons.COLUMN_SEX,person.getSex())
            put(DbContracts.Persons.COLUMN_ORGANIZATION_NAME,person.getOrganizationName())
            put(DbContracts.Persons.COLUMN_NOTE,person.getNote())
        }
    }

    /*
    * 人物を追加
    *
    * @param 人物
    * @return 追加した人物のID
    * */
    private fun insertPerson(person: Person):Int{
        val writableDB = dbHelper.writableDatabase
        val values = getContentValues(person)
        val id = writableDB.insert(DbContracts.Persons.TABLE_NAME,null,values).toInt()
        writableDB.close()

        return id
    }

    /*
    * 人物情報を更新
    *
    * @param 人物
    * */
    private fun updatePerson(person:Person){
        val writableDB = dbHelper.writableDatabase
        val values = getContentValues(person)
        writableDB.update(DbContracts.Persons.TABLE_NAME,
            values,
            "${BaseColumns._ID} = ${person.getId()}",
            null)
        writableDB.close()
    }

    /*
    * 人物を削除
    *
    * @param 削除する人物のID
    * */
    private fun deletePerson(personId: Int){
        val writableDB = dbHelper.writableDatabase
        writableDB.delete(DbContracts.Persons.TABLE_NAME,"${BaseColumns._ID} = $personId",null)
        writableDB.close()
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
                if(view.tag.toString() != view.text.toString()){
                    valueChangedElements.add(view.id)
                    view.setBackgroundResource(R.drawable.value_changed_personal_info_edittext_background)
                }else{
                    view.setBackgroundResource(R.drawable.personal_info_edittext_background)
                }
            }
            else if(view is AutoCompleteTextView){
                if(view.tag.toString() != view.text.toString()){
                    valueChangedElements.add(view.id)
                    view.setBackgroundResource(R.drawable.value_changed_personal_info_edittext_background)
                }else{
                    view.setBackgroundResource(R.drawable.personal_info_edittext_background)
                }
            }
        }
    }
}

package com.example.meishizukan.activity

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getColor
import com.example.meishizukan.R
import com.example.meishizukan.dto.Person
import com.example.meishizukan.util.DbContracts
import com.example.meishizukan.util.DbHelper
import com.example.meishizukan.util.Toaster
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_personal_info_view.*

class PersonalInfoViewActivity : AppCompatActivity() {

    private val newPersonId = 0

    private var personId = -1
    private var isSaved = false

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

        personId = intent.getIntExtra("PERSON_ID",newPersonId)

        if(isNewPerson(personId)){ //新規
            disablePhotosViewButton()
            disableDeleteButton()
        }else{ //編集
            //TODO Personを引っ張ってきてデータを反映
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
        //入力欄のカーソル表示状態を変更する
        fun onKeyboardVisibilityChanged() {
            val focusView = personalInfoConstraintLayout.findFocus()
            if(focusView is EditText){
                val editText = focusView as EditText
                editText.isCursorVisible = isKeyboardShown
            }
            else if(focusView is AutoCompleteTextView){
                val autoCompleteTextView = focusView as AutoCompleteTextView
                autoCompleteTextView.isCursorVisible = isKeyboardShown
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
                isSaved = true

                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_saved_new_person),
                    textColor = getColor(this,R.color.textColorOnSavedNewPerson),
                    backgroundColor = getColor(this,R.color.backgroundColorOnSavedNewPerson),
                    displayTime = Toast.LENGTH_SHORT
                ).show()

                enablePhotosViewButton()
                enableDeleteButton()
            }else{ //編集

            }
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
        if(isSaved){
            super.onBackPressed()
        }

        val title = getString(R.string.confirm_dialog_title)
        val message = getString(R.string.confirm_message_on_transit)
        val positiveButtonText = getString(R.string.positive_button_text)
        val negativeButtonText = getString(R.string.negative_button_text)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, which ->
                super.onBackPressed()
            }
            .setNegativeButton(negativeButtonText,{ dialog, which -> })
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

    private val phoneticNameRegex = Regex("^([\\u30A0-\\u30FF])+\$")
    /*
    * フリガナ項目の入力値がフリガナか否かを取得
    *
    * @return フリガナ項目の入力値がフリガナか否か
    * */
    private fun isPhoneticName():Boolean{
        return (firstPhoneticNameEditText.text.matches(phoneticNameRegex)
                && lastPhoneticNameEditText.text.matches(phoneticNameRegex))
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
    * 人物を追加
    *
    * @param 人物
    * @return 追加した人物のID
    * */
    private fun insertPerson(person: Person):Int{
        val writableDB = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DbContracts.Persons.COLUMN_NAME, person.getName())
            put(DbContracts.Persons.COLUMN_PHONETIC_NAME, person.getPhoneticName())
            put(DbContracts.Persons.COLUMN_SEX,person.getSex())
            put(DbContracts.Persons.COLUMN_ORGANIZATION_NAME,person.getOrganizationName())
            put(DbContracts.Persons.COLUMN_NOTE,person.getNote())
        }
        val id = writableDB.insert(DbContracts.Persons.TABLE_NAME,null,values).toInt()
        writableDB.close()

        return id
    }

    private fun updatePerson(person:Person){

    }

    /*
    * 人物を削除
    *
    * @param 削除する人物のID
    * */
    private fun deletePerson(personId: Int){
        val writableDB = dbHelper.writableDatabase
        writableDB.delete(DbContracts.Persons.TABLE_NAME,"${BaseColumns._ID} = $personId",null)
    }
}

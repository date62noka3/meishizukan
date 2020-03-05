package com.example.meishizukan.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getColor
import com.example.meishizukan.R
import com.example.meishizukan.dto.Person
import com.example.meishizukan.util.Toaster
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_personal_info_view.*

class PersonalInfoViewActivity : AppCompatActivity() {

    private val newPersonId = 0
    private var isSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info_view)

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

        val personId = intent.getIntExtra("PERSON_ID",newPersonId)

        if(isNewPerson(personId)){ //新規
            //フッターの写真画面ボタンを無効化
            val color = getColor(this,R.color.disabledButtonTextColor)
            photosLabel.setTextColor(color)
            photosViewButton.isClickable = false
        }else{ //編集
            //TODO Personを引っ張ってきてデータを反映
        }

        saveButton.setOnClickListener{
            if(isRequiredFieldsBlank()){
                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_input_error),
                    textColor = getColor(this,R.color.textColorOnInputError),
                    backgroundColor = getColor(this,R.color.backgroundColorOnInputError),
                    displayTime = Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if(isNewPerson(personId)){ //新規追加
                Toaster.createToast(
                    context = this,
                    text = getString(R.string.message_on_saved_new_person),
                    textColor = getColor(this,R.color.textColorOnSavedNewPerson),
                    backgroundColor = getColor(this,R.color.backgroundColorOnSavedNewPerson),
                    displayTime = Toast.LENGTH_LONG
                ).show()
            }else{ //編集

            }
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
        val message = getString(R.string.confirm_dialog_message)
        val positiveButtonText = getString(R.string.positive_button_text)
        val negativeButtonText = getString(R.string.negative_button_text)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, which ->
                super.onBackPressed()
            }
            .setNegativeButton(negativeButtonText,{ dialog, which -> })
        alertDialog.show()
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

    private fun insertPerson(person: Person):Int{
        val personId = 1
        return personId
    }

    private fun updatePerson(person:Person){

    }
}

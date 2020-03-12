package com.example.meishizukan.activity

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import com.example.meishizukan.R
import com.example.meishizukan.dto.Person
import com.example.meishizukan.util.DbContracts
import com.example.meishizukan.util.DbHelper
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_photos_view.*
import androidx.core.content.ContextCompat.getColor

class PhotosViewActivity : AppCompatActivity() {

    private var personId = 0

    private val dbHelper = DbHelper(this)
    private lateinit var readableDB: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photos_view)

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
            super.onBackPressed()
        }

        addPhotoButton.setOnClickListener{
            
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
        }
    }

    override fun onResume(){
        super.onResume()
        adView.resume()
    }

    override fun onDestroy(){
        adView.destroy()
        readableDB.close()
        super.onDestroy()
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
            readableDB.close()
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
    }
}

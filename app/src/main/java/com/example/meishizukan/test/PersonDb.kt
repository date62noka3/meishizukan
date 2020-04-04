package com.example.meishizukan.test

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.example.meishizukan.dto.Person
import com.example.meishizukan.util.DbContracts
import com.example.meishizukan.util.DbHelper

private const val SEPARATOR = "|"

class PersonDb(private val context: Context) {
    private val dbHelper = DbHelper(context)
    private var writableDb:SQLiteDatabase

    private val persons = mutableListOf<Person>() //テストデータ

    init {
        //テストデータを読み込む
        val inputStreamReader = context.assets.open("person_list.txt").reader(Charsets.UTF_8)
        inputStreamReader.forEachLine {
            val data = it.split(SEPARATOR)

            persons.add(
                Person(
                    id = data[0].toInt(),
                    name = data[1],
                    phoneticName = data[2],
                    sex = data[3].toInt(),
                    organizationName = data[4],
                    note = data[5]
                )
            )
        }
        inputStreamReader.close()

        writableDb = dbHelper.writableDatabase
    }

    private fun getContentValues(person:Person):ContentValues{
        return ContentValues().apply {
            put(DbContracts.Persons.COLUMN_NAME,person.name)
            put(DbContracts.Persons.COLUMN_PHONETIC_NAME,person.phoneticName)
            put(DbContracts.Persons.COLUMN_SEX,person.sex)
            put(DbContracts.Persons.COLUMN_ORGANIZATION_NAME,person.organizationName)
            put(DbContracts.Persons.COLUMN_NOTE,person.note)
        }
    }

    /*
    * テストデータをDBに追加
    * */
    fun insert(){
        persons.forEach{
            person ->
            val values = getContentValues(person)
            writableDb.insert(DbContracts.Persons.TABLE_NAME,null,values)
        }
    }
}
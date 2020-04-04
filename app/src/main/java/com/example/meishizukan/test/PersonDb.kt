package com.example.meishizukan.test

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.meishizukan.dto.Person
import com.example.meishizukan.util.DbHelper

private const val SEPARATOR = "|"

class PersonDb(private val context: Context) {
    private val dbHelper = DbHelper(context)
    private lateinit var writableDb:SQLiteDatabase

    private val persons = mutableListOf<Person>()

    init{
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
    }
}
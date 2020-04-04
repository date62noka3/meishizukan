package com.example.meishizukan.test

import android.content.Context
import com.example.meishizukan.dto.Person

class PersonDb(private val context: Context) {
    private val persons = mutableListOf<Person>()

    fun getPersons():MutableList<Person>{
        return persons
    }

    init{
        val inputStreamReader = context.assets.open("person_list.csv").reader(Charsets.UTF_8)
        inputStreamReader.forEachLine {
            val data = it.split(',')

            if(data.size != 8)return@forEachLine

            persons.add(
                Person(
                id = data[0].toInt(),
                name = data[1] + data[2],
                phoneticName = data[3] + data[4],
                sex = data[5].toInt(),
                organizationName = data[6],
                note = data[7]
                )
            )
        }
        inputStreamReader.close()
    }
}
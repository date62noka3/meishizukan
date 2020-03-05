package com.example.meishizukan.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

/*
* データベースのコントラクトクラス
* */
object DbContracts{
    object Organizations:BaseColumns{
        const val TABLE_NAME = "organizations"
        const val COLUMN_NAME = "name"
    }

    object Persons:BaseColumns{
        const val TABLE_NAME = "persons"
        const val COLUMN_NAME = "name"
        const val COLUMN_PHONETIC_NAME = "phonetic_name"
        const val COLUMN_SEX = "sex"
        const val COLUMN_ORGANIZATION_ID = "organization_id"
        const val COLUMN_NOTE  = "note"
    }

    object Photos:BaseColumns{
        const val TABLE_NAME = "photos"
        const val COLUMN_BITMAP_INDEX = "bitmap_index"
        const val COLUMN_BITMAP_BINARY = "bitmap_binary"
        const val COLUMN_CREATED_ON = "created_on"
    }

    object PhotosLinks:BaseColumns{
        const val TABLE_NAME = "photos_links"
        const val COLUMN_PHOTO_ID = "photo_id"
        const val COLUMN_PERSON_ID = "person_id"
    }
}

private const val SQL_CREATE_ORGANIZATIONS = "CREATE TABLE ${DbContracts.Organizations.TABLE_NAME}" +
        "(${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${DbContracts.Organizations.COLUMN_NAME} TEXT NOT NULL)"
private const val SQL_DELETE_ORGANIZATIONS = "DROP TABLE IF EXISTS ${DbContracts.Organizations.TABLE_NAME}"

private const val SQL_CREATE_PERSONS = "CREATE TABLE ${DbContracts.Persons.TABLE_NAME}" +
        "(${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${DbContracts.Persons.COLUMN_NAME} TEXT NOT NULL," +
        "${DbContracts.Persons.COLUMN_PHONETIC_NAME} TEXT NOT NULL," +
        "${DbContracts.Persons.COLUMN_SEX} INT NOT NULL DEFAULT 0," +
        "${DbContracts.Persons.COLUMN_ORGANIZATION_ID} INT NOT NULL DEFAULT 0," +
        "${DbContracts.Persons.COLUMN_NOTE} TEXT NOT NULL DEFAULT ''," +
        "FOREIGN KEY(${DbContracts.Persons.COLUMN_ORGANIZATION_ID}) " +
        "REFERENCES ${DbContracts.Organizations.TABLE_NAME}(${BaseColumns._ID}))"
private const val SQL_DELETE_PERSONS = "DROP TABLE IF EXISTS ${DbContracts.Persons.TABLE_NAME}"

private const val SQL_CREATE_PHOTOS = "CREATE TABLE ${DbContracts.Photos.TABLE_NAME}" +
        "(${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${DbContracts.Photos.COLUMN_BITMAP_INDEX} INT NOT NULL," +
        "${DbContracts.Photos.COLUMN_BITMAP_BINARY} BLOB NOT NULL," +
        "${DbContracts.Photos.COLUMN_CREATED_ON} TEXT NOT NULL DEFAULT CURRENT_DATE)"
private const val SQL_DELETE_PHOTOS = "DROP TABLE IF EXISTS ${DbContracts.Photos.TABLE_NAME}"

private const val SQL_CREATE_PHOTOS_LINKS = "CREATE TABLE ${DbContracts.PhotosLinks.TABLE_NAME}" +
        "(${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${DbContracts.PhotosLinks.COLUMN_PHOTO_ID} INT NOT NULL," +
        "${DbContracts.PhotosLinks.COLUMN_PERSON_ID} INT NOT NULL," +
        "FOREIGN KEY(${DbContracts.PhotosLinks.COLUMN_PHOTO_ID}) " +
        "REFERENCES ${DbContracts.Photos.TABLE_NAME}(${BaseColumns._ID})," +
        "FOREIGN KEY(${DbContracts.PhotosLinks.COLUMN_PERSON_ID}) " +
        "REFERENCES ${DbContracts.Persons.TABLE_NAME}(${BaseColumns._ID}))"
private const val SQL_DELETE_PHOTOS_LINKS = "DROP TABLE IF EXISTS ${DbContracts.PhotosLinks.TABLE_NAME}"

class DbHelper(context: Context):SQLiteOpenHelper(context, DATABASE_NAME,null, DATABASE_VERSION) {
    companion object{
        const val DATABASE_NAME = "meishizukan.db"
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ORGANIZATIONS)
        db.execSQL(SQL_CREATE_PERSONS)
        db.execSQL(SQL_CREATE_PHOTOS)
        db.execSQL(SQL_CREATE_PHOTOS_LINKS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_PHOTOS_LINKS)
        db.execSQL(SQL_DELETE_PHOTOS)
        db.execSQL(SQL_DELETE_PERSONS)
        db.execSQL(SQL_DELETE_ORGANIZATIONS)

        onCreate(db)
    }
}
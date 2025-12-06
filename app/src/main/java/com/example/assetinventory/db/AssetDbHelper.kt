package com.example.assetinventory.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AssetDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                file_name TEXT,
                created_at INTEGER
            )"""
        )

        // 修改：增加了 category 字段
        db.execSQL(
            """CREATE TABLE assets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id INTEGER NOT NULL,
                code TEXT NOT NULL,
                name TEXT NOT NULL,
                category TEXT, 
                user TEXT,
                department TEXT,
                location TEXT,
                start_date TEXT,
                status TEXT NOT NULL,
                UNIQUE(task_id, code)
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS assets")
        db.execSQL("DROP TABLE IF EXISTS tasks")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "assets.db"
        // 建议增加版本号，但在开发阶段直接重装APP更简单
        private const val DATABASE_VERSION = 1 
    }
}

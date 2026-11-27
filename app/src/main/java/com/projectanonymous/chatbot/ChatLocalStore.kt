package com.projectanonymous.chatbot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri

/** Small private SQLite store for the current chat transcript and Library metadata. */
class ChatLocalStore(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE messages (id TEXT PRIMARY KEY NOT NULL, text TEXT NOT NULL, from_user INTEGER NOT NULL, created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))) ")
        db.execSQL("CREATE TABLE library_items (id TEXT PRIMARY KEY NOT NULL, uri TEXT NOT NULL, name TEXT NOT NULL, is_image INTEGER NOT NULL, created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))) ")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No destructive migration: future schema changes must preserve user data.
    }

    fun loadMessages(): List<ChatMessage> = readableDatabase.query(
        "messages", arrayOf("id", "text", "from_user"), null, null, null, null, "created_at ASC, rowid ASC"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(
                ChatMessage(
                    id = cursor.getString(0),
                    text = cursor.getString(1),
                    fromUser = cursor.getInt(2) == 1
                )
            )
        }
    }

    fun saveMessage(message: ChatMessage) {
        val values = ContentValues().apply {
            put("id", message.id)
            put("text", message.text)
            put("from_user", if (message.fromUser) 1 else 0)
        }
        writableDatabase.insertWithOnConflict("messages", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun clearMessages() { writableDatabase.delete("messages", null, null) }

    fun loadLibrary(): List<LibraryItem> = readableDatabase.query(
        "library_items", arrayOf("id", "uri", "name", "is_image"), null, null, null, null, "created_at ASC, rowid ASC"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(
                LibraryItem(
                    id = cursor.getString(0),
                    uri = Uri.parse(cursor.getString(1)),
                    name = cursor.getString(2),
                    isImage = cursor.getInt(3) == 1
                )
            )
        }
    }

    fun saveLibraryItem(item: LibraryItem) {
        val values = ContentValues().apply {
            put("id", item.id)
            put("uri", item.uri.toString())
            put("name", item.name)
            put("is_image", if (item.isImage) 1 else 0)
        }
        writableDatabase.insertWithOnConflict("library_items", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteLibraryItem(item: LibraryItem) {
        writableDatabase.delete("library_items", "id = ?", arrayOf(item.id))
    }

    companion object {
        private const val DATABASE_NAME = "chatbot_local.db"
        private const val DATABASE_VERSION = 1
    }
}

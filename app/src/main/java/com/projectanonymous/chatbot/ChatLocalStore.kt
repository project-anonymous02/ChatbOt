package com.projectanonymous.chatbot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import java.util.UUID

data class ChatConversation(val id: String, val title: String, val updatedAt: Long)

/** Private local store for chat conversations, messages and Library metadata. */
class ChatLocalStore(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE conversations (id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE messages (id TEXT PRIMARY KEY NOT NULL, conversation_id TEXT NOT NULL, text TEXT NOT NULL, from_user INTEGER NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX index_messages_conversation ON messages(conversation_id, created_at)")
        db.execSQL("CREATE TABLE library_items (id TEXT PRIMARY KEY NOT NULL, uri TEXT NOT NULL, name TEXT NOT NULL, is_image INTEGER NOT NULL, created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))) ")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val now = System.currentTimeMillis()
            val legacyId = "legacy-" + UUID.randomUUID().toString()
            db.execSQL("CREATE TABLE conversations (id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
            db.execSQL("INSERT INTO conversations(id, title, created_at, updated_at) VALUES(?, ?, ?, ?)", arrayOf(legacyId, "Percakapan sebelumnya", now, now))
            db.execSQL("ALTER TABLE messages ADD COLUMN conversation_id TEXT NOT NULL DEFAULT 'legacy'")
            db.execSQL("UPDATE messages SET conversation_id = ? WHERE conversation_id = 'legacy'", arrayOf(legacyId))
            db.execSQL("CREATE INDEX index_messages_conversation ON messages(conversation_id, created_at)")
        }
    }

    fun loadConversations(): List<ChatConversation> = readableDatabase.query("conversations", arrayOf("id", "title", "updated_at"), null, null, null, null, "updated_at DESC").use { cursor ->
        buildList { while (cursor.moveToNext()) add(ChatConversation(cursor.getString(0), cursor.getString(1), cursor.getLong(2))) }
    }

    fun createConversation(title: String = "Chat baru"): ChatConversation {
        val now = System.currentTimeMillis()
        val item = ChatConversation(UUID.randomUUID().toString(), title, now)
        val values = ContentValues().apply { put("id", item.id); put("title", item.title); put("created_at", now); put("updated_at", now) }
        writableDatabase.insertOrThrow("conversations", null, values)
        return item
    }

    fun loadMessages(conversationId: String): List<ChatMessage> = readableDatabase.query("messages", arrayOf("id", "text", "from_user"), "conversation_id = ?", arrayOf(conversationId), null, null, "created_at ASC, rowid ASC").use { cursor ->
        buildList { while (cursor.moveToNext()) add(ChatMessage(cursor.getString(0), cursor.getString(1), cursor.getInt(2) == 1)) }
    }

    fun saveMessage(message: ChatMessage, conversationId: String): ChatConversation? {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val values = ContentValues().apply { put("id", message.id); put("conversation_id", conversationId); put("text", message.text); put("from_user", if (message.fromUser) 1 else 0); put("created_at", now) }
        db.insertWithOnConflict("messages", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        if (message.fromUser) db.execSQL("UPDATE conversations SET title = CASE WHEN title = 'Chat baru' THEN substr(?, 1, 64) ELSE title END, updated_at = ? WHERE id = ?", arrayOf(message.text, now, conversationId))
        else db.execSQL("UPDATE conversations SET updated_at = ? WHERE id = ?", arrayOf(now, conversationId))
        return getConversation(conversationId)
    }

    fun getConversation(id: String): ChatConversation? = readableDatabase.query("conversations", arrayOf("id", "title", "updated_at"), "id = ?", arrayOf(id), null, null, null).use { cursor ->
        if (cursor.moveToFirst()) ChatConversation(cursor.getString(0), cursor.getString(1), cursor.getLong(2)) else null
    }

    fun deleteConversation(id: String) {
        writableDatabase.beginTransaction()
        try { writableDatabase.delete("messages", "conversation_id = ?", arrayOf(id)); writableDatabase.delete("conversations", "id = ?", arrayOf(id)); writableDatabase.setTransactionSuccessful() }
        finally { writableDatabase.endTransaction() }
    }

    fun loadLibrary(): List<LibraryItem> = readableDatabase.query("library_items", arrayOf("id", "uri", "name", "is_image"), null, null, null, null, "created_at ASC, rowid ASC").use { cursor ->
        buildList { while (cursor.moveToNext()) add(LibraryItem(cursor.getString(0), Uri.parse(cursor.getString(1)), cursor.getString(2), cursor.getInt(3) == 1)) }
    }

    fun saveLibraryItem(item: LibraryItem) {
        val values = ContentValues().apply { put("id", item.id); put("uri", item.uri.toString()); put("name", item.name); put("is_image", if (item.isImage) 1 else 0) }
        writableDatabase.insertWithOnConflict("library_items", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteLibraryItem(item: LibraryItem) { writableDatabase.delete("library_items", "id = ?", arrayOf(item.id)) }

    companion object { private const val DATABASE_NAME = "chatbot_local.db"; private const val DATABASE_VERSION = 2 }
}

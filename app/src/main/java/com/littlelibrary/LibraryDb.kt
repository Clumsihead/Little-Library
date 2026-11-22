package com.littlelibrary

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tiny SQLite store: items (the saved links), tags (every tag you've ever made,
 * kept even if no item uses it any more) and item_tags (which item sits on which shelf).
 */
class LibraryDb private constructor(context: Context) :
    SQLiteOpenHelper(context, "library.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                url TEXT NOT NULL UNIQUE,
                title TEXT NOT NULL DEFAULT '',
                source TEXT NOT NULL DEFAULT '',
                saved_at INTEGER NOT NULL
            )""",
        )
        db.execSQL(
            """CREATE TABLE tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE
            )""",
        )
        db.execSQL(
            """CREATE TABLE item_tags (
                item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
                tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                PRIMARY KEY (item_id, tag_id)
            )""",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    /** Every tag ever created, most used first. */
    suspend fun allTags(): List<TagCount> = withContext(Dispatchers.IO) {
        val out = ArrayList<TagCount>()
        readableDatabase.rawQuery(
            """SELECT t.name, COUNT(it.item_id) AS c
               FROM tags t LEFT JOIN item_tags it ON it.tag_id = t.id
               GROUP BY t.id ORDER BY c DESC, t.name ASC""",
            null,
        ).use { c ->
            while (c.moveToNext()) out.add(TagCount(c.getString(0), c.getInt(1)))
        }
        out
    }

    suspend fun itemCount(): Int = withContext(Dispatchers.IO) {
        DatabaseUtils.queryNumEntries(readableDatabase, "items").toInt()
    }

    /** Saved items, newest first. Pass a tag to see just that shelf. */
    suspend fun items(tag: String?): List<SavedItem> = withContext(Dispatchers.IO) {
        val args = ArrayList<String>()
        val where = if (tag != null) {
            args.add(tag)
            """WHERE i.id IN (
                 SELECT it2.item_id FROM item_tags it2
                 JOIN tags t2 ON t2.id = it2.tag_id WHERE t2.name = ?)"""
        } else {
            ""
        }
        val sql = """SELECT i.id, i.url, i.title, i.source, i.saved_at, COALESCE(GROUP_CONCAT(t.name), '')
                     FROM items i
                     LEFT JOIN item_tags it ON it.item_id = i.id
                     LEFT JOIN tags t ON t.id = it.tag_id
                     $where
                     GROUP BY i.id ORDER BY i.saved_at DESC"""
        val out = ArrayList<SavedItem>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            while (c.moveToNext()) {
                out.add(
                    SavedItem(
                        id = c.getLong(0),
                        url = c.getString(1),
                        title = c.getString(2),
                        source = c.getString(3),
                        savedAt = c.getLong(4),
                        tags = c.getString(5).split(',').filter { it.isNotEmpty() }.sorted(),
                    ),
                )
            }
        }
        out
    }

    /**
     * Saves a link and its tags. If the link is already in the library, the new tags
     * are simply added to the existing entry (no duplicates).
     */
    suspend fun save(url: String, title: String, source: String, tags: List<String>): SaveResult =
        withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                var itemId = -1L
                var existingTitle = ""
                db.rawQuery("SELECT id, title FROM items WHERE url = ?", arrayOf(url)).use { c ->
                    if (c.moveToFirst()) {
                        itemId = c.getLong(0)
                        existingTitle = c.getString(1)
                    }
                }

                val result: SaveResult
                if (itemId == -1L) {
                    val cv = ContentValues().apply {
                        put("url", url)
                        put("title", title)
                        put("source", source)
                        put("saved_at", System.currentTimeMillis())
                    }
                    itemId = db.insertOrThrow("items", null, cv)
                    result = SaveResult.ADDED
                } else {
                    if (existingTitle.isBlank() && title.isNotBlank()) {
                        val cv = ContentValues().apply { put("title", title) }
                        db.update("items", cv, "id = ?", arrayOf(itemId.toString()))
                    }
                    result = SaveResult.UPDATED
                }

                for (name in tags) {
                    db.insertWithOnConflict(
                        "tags", null,
                        ContentValues().apply { put("name", name) },
                        SQLiteDatabase.CONFLICT_IGNORE,
                    )
                    var tagId = -1L
                    db.rawQuery("SELECT id FROM tags WHERE name = ?", arrayOf(name)).use { c ->
                        if (c.moveToFirst()) tagId = c.getLong(0)
                    }
                    if (tagId != -1L) {
                        db.insertWithOnConflict(
                            "item_tags", null,
                            ContentValues().apply {
                                put("item_id", itemId)
                                put("tag_id", tagId)
                            },
                            SQLiteDatabase.CONFLICT_IGNORE,
                        )
                    }
                }
                db.setTransactionSuccessful()
                result
            } finally {
                db.endTransaction()
            }
        }

    suspend fun delete(itemId: Long) = withContext(Dispatchers.IO) {
        writableDatabase.delete("items", "id = ?", arrayOf(itemId.toString()))
        Unit
    }

    companion object {
        @Volatile
        private var instance: LibraryDb? = null

        fun get(context: Context): LibraryDb =
            instance ?: synchronized(this) {
                instance ?: LibraryDb(context.applicationContext).also { instance = it }
            }
    }
}

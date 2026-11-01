package com.jarvis.kisisel;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import java.util.ArrayList;
import java.util.List;

public class MemoryDb extends SQLiteOpenHelper {
    public static class Reminder {
        public final int id;
        public final String title;
        public final long when;
        Reminder(int id, String title, long when) {
            this.id = id; this.title = title; this.when = when;
        }
    }
    public MemoryDb(Context ctx) { super(ctx, "jarvis_memory.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT NOT NULL, created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, content TEXT NOT NULL, created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE reminders (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, trigger_at INTEGER NOT NULL, delivered INTEGER NOT NULL DEFAULT 0)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
    public void note(String content) {
        ContentValues v = new ContentValues(); v.put("content", content); v.put("created", System.currentTimeMillis());
        getWritableDatabase().insert("notes", null, v);
    }
    public List<String> notes() {
        List<String> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT content FROM notes ORDER BY id DESC LIMIT 100", null)) {
            while(c.moveToNext()) out.add(c.getString(0));
        }
        return out;
    }
    public void clearNotes() { getWritableDatabase().delete("notes", null, null); }
    public void message(String role, String content) {
        ContentValues v = new ContentValues(); v.put("role", role); v.put("content", content); v.put("created", System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase(); db.insert("messages", null, v);
        db.execSQL("DELETE FROM messages WHERE id NOT IN (SELECT id FROM messages ORDER BY id DESC LIMIT 150)");
    }
    public List<String[]> messages() {
        List<String[]> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT role,content FROM messages ORDER BY id DESC LIMIT 30", null)) {
            while(c.moveToNext()) out.add(0, new String[]{ c.getString(0), c.getString(1) });
        }
        return out;
    }
    public void clearMessages() { getWritableDatabase().delete("messages", null, null); }
    public int addReminder(String title, long when) {
        ContentValues v = new ContentValues(); v.put("title", title); v.put("trigger_at", when); v.put("delivered", 0);
        return (int)getWritableDatabase().insert("reminders", null, v);
    }
    public List<Reminder> pendingReminders() {
        List<Reminder> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,title,trigger_at FROM reminders WHERE delivered=0 AND trigger_at>=? ORDER BY trigger_at", new String[]{ String.valueOf(System.currentTimeMillis()) })) {
            while(c.moveToNext()) out.add(new Reminder(c.getInt(0), c.getString(1), c.getLong(2)));
        }
        return out;
    }
    public void delivered(int id) {
        ContentValues v = new ContentValues(); v.put("delivered", 1);
        getWritableDatabase().update("reminders", v, "id=?", new String[]{String.valueOf(id)});
    }
    public void removeReminder(int id) { getWritableDatabase().delete("reminders", "id=?", new String[]{String.valueOf(id)}); }
}

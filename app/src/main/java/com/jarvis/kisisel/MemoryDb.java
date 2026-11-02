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
    public MemoryDb(Context ctx) { super(ctx, "jarvis_memory.db", null, 2); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT NOT NULL, created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, content TEXT NOT NULL, created INTEGER NOT NULL, session_id INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE reminders (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, trigger_at INTEGER NOT NULL, delivered INTEGER NOT NULL DEFAULT 0)");
        createSessions(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion<2){db.execSQL("ALTER TABLE messages ADD COLUMN session_id INTEGER NOT NULL DEFAULT 1");createSessions(db);}
    }
    private void createSessions(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, created INTEGER NOT NULL)");
        db.execSQL("INSERT OR IGNORE INTO sessions (id,created) VALUES (1,0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS messages_session ON messages(session_id,id)");
    }
    public long currentSession(){try(Cursor c=getReadableDatabase().rawQuery("SELECT MAX(id) FROM sessions",null)){return c.moveToFirst()?c.getLong(0):1;}}
    public void newConversation(){ContentValues v=new ContentValues();v.put("created",System.currentTimeMillis());getWritableDatabase().insertOrThrow("sessions",null,v);}
    public List<String[]> searchMessages(String search,long before){
        List<String[]> rows=new ArrayList<>();String q="%"+search.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,role,content,created,session_id FROM messages WHERE id<? AND content LIKE ? ESCAPE '\\' ORDER BY id DESC LIMIT 50",new String[]{String.valueOf(before),q})){
            while(c.moveToNext())rows.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)});
        }return rows;
    }
    public void exportMessages(java.io.Writer writer)throws java.io.IOException{
        try(Cursor c=getReadableDatabase().rawQuery("SELECT role,content,created,session_id FROM messages ORDER BY id",null)){
            while(c.moveToNext()){writer.write("Sohbet "+c.getLong(3)+" · "+new java.util.Date(c.getLong(2))+" · "+("user".equals(c.getString(0))?"Sen":"Pandora")+"\n"+c.getString(1)+"\n\n");}
        }
    }
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
        ContentValues v = new ContentValues(); v.put("role", role); v.put("content", content); v.put("session_id",currentSession()); v.put("created", System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase(); db.insert("messages", null, v);

    }
    public List<String[]> messages() {
        List<String[]> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT role,content FROM messages WHERE session_id=? ORDER BY id DESC LIMIT 150", new String[]{String.valueOf(currentSession())})) {
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

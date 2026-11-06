package com.jarvis.kisisel;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Lookup strictly on the device after optional READ_CONTACTS approval.
 * No contact list is sent to any model or cloud provider.
 */
public final class PandoraCallResolver {
    public static final class Match {
        public final String label,number;
        public Match(String label,String number){this.label=label;this.number=number;}
    }
    private PandoraCallResolver(){}
    public static String phoneNumber(String text){
        if(text==null)return null;
        String raw=text.trim();
        if(!raw.matches("[+0-9()\\-\\s]{7,24}"))return null;
        String n=raw.replaceAll("[^0-9+]","");
        if(n.startsWith("+")) {
            if(n.substring(1).contains("+"))return null;
        }else if(n.contains("+"))return null;
        int digits=n.replace("+","").length();
        if(digits<10 || digits>15)return null;
        // Do not automatically dial emergency numbers, USSD or service codes.
        if(n.startsWith("*")||n.startsWith("#"))return null;
        return n;
    }
    public static List<Match> resolve(Context context,String query){
        ArrayList<Match> list=new ArrayList<>();
        String direct=phoneNumber(query);
        if(direct!=null){list.add(new Match(direct,direct));return list;}
        if(query==null||query.trim().length()<2||query.length()>90)return list;
        if(context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
            !=PackageManager.PERMISSION_GRANTED)return list;
        String[] projection={
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String selection=ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ?";
        String name=query.trim().replaceFirst("(?iu)[’']y?[ıiuü]$","")
            .replaceFirst("(?iu)yi$","")
            .replaceFirst("(?iu)([mn])[ıiuü]$","$1");
        String[] args={"%"+name+"%"};
        HashSet<String> unique=new HashSet<>();
        try(Cursor cursor=context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,selection,args,null)){
            if(cursor==null)return list;
            while(cursor.moveToNext()&&list.size()<8){
                String name=cursor.getString(0);
                String value=phoneNumber(cursor.getString(1));
                if(value==null)continue;
                if(unique.add(value))list.add(new Match(
                    name==null?"Kayıtlı kişi":name,value));
            }
        }catch(RuntimeException ignored){
            // Contacts provider or permission may disappear mid-query.
        }
        return list;
    }
}
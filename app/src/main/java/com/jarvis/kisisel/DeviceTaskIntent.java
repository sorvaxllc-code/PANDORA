package com.jarvis.kisisel;
import java.util.Locale;
/** Conservative intent entry; recipient and content are resolved in the task sheet. */
public final class DeviceTaskIntent {
    public enum Kind { NONE, MESSAGE, VIDEO }
    private DeviceTaskIntent(){}
    public static Kind classify(String input){
        if(input==null)return Kind.NONE;
        String q=input.toLowerCase(new Locale("tr","TR")).trim();
        if(q.contains("nasıl")||q.contains("nedir")||q.contains("gönderme")||q.contains("atma")||q.contains("geçme"))return Kind.NONE;
        if((q.contains("görüntülü")||q.contains("video")) && (q.contains("geç")||q.contains("aktar")))return Kind.VIDEO;
        if((q.contains("mesaj")||q.contains("sms"))&&(q.contains("gönder")||q.contains("mesaj at")||q.contains("ilet")))return Kind.MESSAGE;
        return Kind.NONE;
    }
}

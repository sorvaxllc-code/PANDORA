package com.jarvis.kisisel.ai;
import android.content.Context;
import java.net.*;
import java.io.*;
import org.json.JSONObject;
/** Authenticated read-only discovery; never asks the agent to execute a task. */
public final class HermesHealth {
    private HermesHealth(){}
    public static String check(Context context){
        String key=HermesVault.get(context);if(key.isEmpty())return "Hermes anahtarı kayıtlı değil veya çözülemedi. Anahtarı yeniden gir.";
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL("http://127.0.0.1:8642/v1/models").openConnection();
            c.setInstanceFollowRedirects(false);c.setConnectTimeout(4000);c.setReadTimeout(6000);c.setRequestProperty("Authorization","Bearer "+key);
            int status=c.getResponseCode();
            if(status==401||status==403)return "Gateway erişilebilir, fakat anahtar kabul edilmedi. Termux API_SERVER_KEY ile eşleştiğini kontrol et.";
            if(status!=200)return "Gateway HTTP "+status+" döndürdü.";
            StringBuilder text=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(c.getInputStream(),java.nio.charset.StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){text.append(line);if(text.length()>65536)throw new IOException();}}
            String model=new JSONObject(text.toString()).getJSONArray("data").getJSONObject(0).getString("id");
            return "Hermes Gateway erişilebilir; anahtar kabul edildi. Model: "+model+"\nBu kontrol sohbet üretimini ve görsel model desteğini test etmez.";
        }catch(Exception e){return "Hermes Gateway'e ulaşılamadı veya geçerli model yanıtı alınamadı. Termux'ta hermes gateway çalışıyor olmalı.";}
        finally{if(c!=null)c.disconnect();}
    }
}

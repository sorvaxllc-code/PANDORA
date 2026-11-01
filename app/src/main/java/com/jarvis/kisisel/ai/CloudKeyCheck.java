package com.jarvis.kisisel.ai;
import android.content.Context;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** GET key verification does not generate text or use completion quota. */
public final class CloudKeyCheck {
    private CloudKeyCheck() {}
    public interface Callback { void onResult(boolean success, String message); }
    public static void checkAsync(Context context, Callback callback) {
        Context app=context.getApplicationContext();
        new Thread(() -> {
            String key=SecretVault.get(app);
            if(key.isEmpty()) {callback.onResult(false,"OpenRouter API anahtarı kayıtlı değil. Ayarlardan anahtar ekle.");return;}
            HttpURLConnection conn=null;
            try {
                conn=(HttpURLConnection)new URL(FreeCloudPolicy.KEY_CHECK_URL).openConnection();
                conn.setRequestMethod("GET");conn.setConnectTimeout(7000);conn.setReadTimeout(10000);
                conn.setRequestProperty("Authorization","Bearer "+key);
                int status=conn.getResponseCode();
                if(status!=200) {callback.onResult(false,FreeCloudPolicy.httpMessage(status,true));return;}
                StringBuilder response=new StringBuilder();
                try(InputStream in=conn.getInputStream();
                    BufferedReader reader=new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))){
                    String line;while((line=reader.readLine())!=null && response.length()<30000)response.append(line);
                }
                JSONObject data=new JSONObject(response.toString()).optJSONObject("data");
                if(data==null) {callback.onResult(false,"Sunucu cevap verdi ama beklenen anahtar bilgisi bulunamadı.");return;}
                StringBuilder msg=new StringBuilder("OpenRouter API anahtarı geçerli. Bu kontrol model üretimini test etmez ve sohbet isteği göndermez.\n\nYapay zekâ > Bulut seçerek ücretsiz sohbet deneyebilirsin.");
                JSONObject daily=data.optJSONObject("free_model_daily_requests");
                if(daily!=null){
                    int limit=daily.optInt("limit",-1),remaining=daily.optInt("remaining",-1);
                    if(limit>=0 && remaining>=0)msg.append("\nGörünen ücretsiz kota: ").append(remaining).append(" / ").append(limit).append(" istek.");
                }
                callback.onResult(true,msg.toString());
            } catch(Exception ex) {
                callback.onResult(false,"Bulut anahtarı kontrol edilemedi: bağlantı sorunu veya sunucu yanıt vermiyor. İnternet bağlantını kontrol et.");
            } finally {if(conn!=null)conn.disconnect();}
        },"pandora-cloud-key-check").start();
    }
}

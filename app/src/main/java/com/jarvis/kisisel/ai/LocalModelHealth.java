package com.jarvis.kisisel.ai;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Fast, read-only health probe; never sends notes or other personal content. */
public final class LocalModelHealth {
    public interface Callback { void onCheck(boolean ready,String details); }
    private LocalModelHealth(){}
    public static void checkAsync(Callback callback){
        new Thread(()->{
            String info;boolean ok=false;
            HttpURLConnection con=null;
            try{
                long start=System.nanoTime();
                con=(HttpURLConnection)new URL("http://127.0.0.1:8080/health").openConnection();
                con.setConnectTimeout(2000);con.setReadTimeout(2500);
                int status=con.getResponseCode();
                if(status==200){
                    try(InputStream in=con.getInputStream()){
                        ByteArrayOutputStream buf=new ByteArrayOutputStream();byte[] bytes=new byte[1024];int n;
                        while((n=in.read(bytes))!=-1 && buf.size()<8192) buf.write(bytes,0,n);
                        JSONObject json=new JSONObject(buf.toString(StandardCharsets.UTF_8.name()));
                        ok="ok".equals(json.optString("status"));
                    }
                }
                long ms=(System.nanoTime()-start)/1_000_000L;
                info=ok?"Yerel model sunucusu hazır ("+ms+" ms). Bu değer cevap üretme hızını ölçmez.":
                         "Sunucu yanıt verdi ancak model henüz hazır değil (HTTP "+status+").";
            }catch(Exception ex){info="Yerel sunucuya bağlanılamadı. Termux'ta llama-server açık mı?";}
            finally{if(con!=null)con.disconnect();}
            callback.onCheck(ok,info);
        },"pandora-local-health").start();
    }
}

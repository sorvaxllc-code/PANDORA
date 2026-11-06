package com.jarvis.kisisel.ai;
import android.content.Context;
import android.content.SharedPreferences;
import com.jarvis.kisisel.MemoryDb;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
/** Modes OFF/LOCAL/CLOUD/AUTO. AUTO uses ONLY free OpenRouter, then local. */
public final class ModelRouter {
 private final Context app; private final MemoryDb memory;
 private final SharedPreferences prefs;
 public interface Callback {void onResult(String text,String source);}
 public ModelRouter(Context context,MemoryDb memory){
   this.app=context.getApplicationContext();this.memory=memory;
   prefs=app.getSharedPreferences("jarvis_settings",0);
 }
 public String mode(){return prefs.getString("pandora_mode","OFF");}
 public void setMode(String mode){if(mode.equals("OFF")||mode.equals("LOCAL")||mode.equals("CLOUD")||mode.equals("AUTO")||mode.equals("HERMES"))prefs.edit().putString("pandora_mode",mode).apply();}
 public String modeLabel(){switch(mode()){
   case "HERMES":return "HERMES";case "LOCAL":return "YEREL";case "CLOUD":return "BULUT";case "AUTO":return "OTOMATİK";default:return "KOMUT";}}
 public boolean allowCloudMemory(){return prefs.getBoolean("cloud_history_optin",false);}
 public void setAllowCloudMemory(boolean value){prefs.edit().putBoolean("cloud_history_optin",value).apply();}
 private JSONArray messages(String prompt,boolean cloud,boolean allowActions,boolean includeMemory) throws Exception {
   JSONArray a=new JSONArray();
   StringBuilder system=new StringBuilder(ModelIntentPolicy.systemInstructions(allowActions));
   if(includeMemory && (!cloud || allowCloudMemory())){
     List<String> notes=memory.notes();int n=Math.min(notes.size(),cloud?10:15);
     system.append("Kullanıcının notları (doğrulanmamış olabilir):\n");
     for(int i=0;i<n;i++){String note=notes.get(i);system.append("- ").append(note.length()>250?note.substring(0,250):note).append("\n");}
   }
   a.put(new JSONObject().put("role","system").put("content",system.toString()));
   if(includeMemory && (!cloud || allowCloudMemory())){
     List<String[]> hist=memory.messages();int start=Math.max(0,hist.size()-5);
     for(int i=start;i<hist.size();i++){
       String[] m=hist.get(i);
       if(i==hist.size()-1 && "user".equals(m[0]) && m[1].equals(prompt))continue;String content=m[1];if(content.length()>500)content=content.substring(0,500);
       a.put(new JSONObject().put("role","user".equals(m[0])?"user":"assistant").put("content",content));
     }
   }
   a.put(new JSONObject().put("role","user").put("content",prompt));
   return a;
 }
 public HttpAiProvider.CancelToken askAsync(String prompt,Callback callback){
   return askInternal(prompt,callback,true,true);
 }
 /** Read-only reasoning path; returned model content never becomes a device action. */
 public HttpAiProvider.CancelToken askInformationalAsync(String prompt,Callback callback) {
   return askInternal(prompt,callback,false,true);
 }
 /** Never include unrelated notes/chat in a screen review. */
 public HttpAiProvider.CancelToken askScreenAsync(String prompt,Callback callback) {
   return askInternal(prompt,callback,false,false);
 }
 private HttpAiProvider.CancelToken askInternal(String prompt,Callback callback,
                                                boolean allowActions,boolean includeMemory) {
   final HttpAiProvider.CancelToken cancellation=new HttpAiProvider.CancelToken();
   new Thread(()->{
     String text=null,source="";String errors="";
     String mode=mode();String key=SecretVault.get(app);
     if(!cancellation.isCancelled() && mode.equals("HERMES")){
       String hermesKey=HermesVault.get(app);
       if(hermesKey.isEmpty()) errors="Hermes API anahtarını Ayarlar'dan ekle.";
       else try{
         HttpAiProvider gateway=new HttpAiProvider("Hermes Agent (yerel Gateway)",
           "http://127.0.0.1:8642/v1/chat/completions","hermes-agent",hermesKey,120000);
         text=gateway.answer(messages(prompt,true,false,includeMemory),cancellation);
         source=gateway.id();
       }catch(Exception e){errors="Hermes: "+e.getMessage();}
     }
     if(!cancellation.isCancelled() && (mode.equals("CLOUD")||mode.equals("AUTO")) && !key.isEmpty()){
       try {
         AiProvider cloud=new HttpAiProvider("OpenRouter ücretsiz",FreeCloudPolicy.COMPLETIONS_URL,FreeCloudPolicy.MODEL,key,FreeCloudPolicy.CLOUD_READ_TIMEOUT_MS);
         text=((HttpAiProvider)cloud).answer(messages(prompt,true,allowActions,includeMemory),cancellation);source=cloud.id();
       } catch(Exception e){errors="Bulut: "+e.getMessage();}
     }else if(mode.equals("CLOUD")){errors="Bulut için Ayarlar'dan OpenRouter anahtarını ekle.";}
     if(!cancellation.isCancelled() && text==null && (mode.equals("LOCAL")||mode.equals("AUTO"))){
       try {
         HttpAiProvider local=new HttpAiProvider("Yerel Qwen","http://127.0.0.1:8080/v1/chat/completions","jarvis-local","",mode.equals("AUTO")?FreeCloudPolicy.FALLBACK_LOCAL_READ_TIMEOUT_MS:FreeCloudPolicy.LOCAL_READ_TIMEOUT_MS);
         text=local.answer(messages(prompt,false,allowActions,includeMemory),cancellation);source=local.id();
       } catch(Exception e){errors+=(errors.isEmpty()?"":"; ")+"Yerel: "+e.getMessage();}
     }
     if(cancellation.isCancelled())return;
     if(text==null){
       if(mode.equals("HERMES")){text="Hermes yanıt veremedi. "+errors+" Gateway Termux içinde açık mı ve API anahtarı kayıtlı mı kontrol et.";}
       else if(mode.equals("LOCAL")){
         text="Yerel model belirlenen sürede yanıt üretemedi. 8B model bu telefonda çok yavaş çalışıyor. "
             +"Durdur düğmesini kullanabilir veya Yapay zekâ > Bulut/Otomatik moduna geçebilirsin. "
             +"Not alma ve saat gibi temel görevler bulut gerektirmez.";
       } else if(mode.equals("AUTO")) {
         text="Otomatik modda yanıt alınamadı. Ücretsiz bulut ya da yerel model erişilemez durumda. "+errors;
       } else {
         text="Yanıt alınamadı. "+(errors.isEmpty()?"Model modunu veya bulut anahtarını kontrol et.":errors);
       }
     }
     if(!cancellation.isCancelled())callback.onResult(text,source);
   },"pandora-model-router").start();
   return cancellation;
 }
}

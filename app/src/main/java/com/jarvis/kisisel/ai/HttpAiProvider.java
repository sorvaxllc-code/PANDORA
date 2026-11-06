package com.jarvis.kisisel.ai;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
/** Hardcoded loopback, OpenRouter or Groq HTTPS only. No arbitrary remote URLs. */
public final class HttpAiProvider implements AiProvider {
 private final String id,endpoint,model,token;
 private final int timeoutMs;
 public HttpAiProvider(String id,String endpoint,String model,String token,int timeoutMs){
   this.id=id;this.endpoint=endpoint;this.model=model;this.token=token;this.timeoutMs=timeoutMs;
 }
 public String id(){return id;}
 /** Disconnecting the HTTP socket allows user cancellation without waiting for read timeout. */
 public static final class CancelToken {
   private boolean cancelled=false;
   private HttpURLConnection active;
   public synchronized boolean isCancelled(){return cancelled;}
   public synchronized void attach(HttpURLConnection c){
     if(cancelled){c.disconnect();return;}
     active=c;
   }
   public synchronized void detach(HttpURLConnection c){if(active==c)active=null;}
   public synchronized void cancel(){cancelled=true;if(active!=null){active.disconnect();active=null;}}
   public void ensureActive() throws java.io.IOException {if(isCancelled())throw new java.io.IOException("İstek iptal edildi");}
 }
 public String answer(JSONArray messages) throws Exception {return answer(messages,new CancelToken());}
 public String answer(JSONArray messages,CancelToken cancellation) throws Exception {
   cancellation.ensureActive();
   if(!endpoint.equals("http://127.0.0.1:8080/v1/chat/completions") &&
      !endpoint.equals(FreeCloudPolicy.COMPLETIONS_URL) &&
      !endpoint.equals("https://api.groq.com/openai/v1/chat/completions") &&
      !endpoint.equals("http://127.0.0.1:8642/v1/chat/completions"))throw new SecurityException("Unsupported endpoint");
   HttpURLConnection con=(HttpURLConnection)new URL(endpoint).openConnection();
   cancellation.attach(con);
   try {
     cancellation.ensureActive();
     con.setRequestMethod("POST");con.setConnectTimeout(5000);con.setReadTimeout(timeoutMs);
     con.setRequestProperty("Content-Type","application/json; charset=utf-8");
     if(token!=null && !token.isEmpty())con.setRequestProperty("Authorization","Bearer "+token);
     if(endpoint.equals(FreeCloudPolicy.COMPLETIONS_URL) && !FreeCloudPolicy.isFreeOnly(model))
       throw new SecurityException("Ücretli bulut modeli yasaklı");
     boolean groq=endpoint.equals("https://api.groq.com/openai/v1/chat/completions");
     if(groq && !"openai/gpt-oss-20b".equals(model))
       throw new SecurityException("Groq model allowlist mismatch");
     con.setDoOutput(true);
     String resolvedModel=model;
     // llama.cpp may be started without --alias, in which case discover its actual loaded model id.
     if(endpoint.startsWith("http://127.0.0.1:")){
       try {
         HttpURLConnection models=(HttpURLConnection)new URL(endpoint.startsWith("http://127.0.0.1:8642/")?"http://127.0.0.1:8642/v1/models":"http://127.0.0.1:8080/v1/models").openConnection();
         models.setConnectTimeout(4000);models.setReadTimeout(4000);
         if(endpoint.startsWith("http://127.0.0.1:8642/"))models.setRequestProperty("Authorization","Bearer "+token);
         try(InputStream is=models.getInputStream();BufferedReader rd=new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))){
           StringBuilder b=new StringBuilder();String line;
           while((line=rd.readLine())!=null)b.append(line);
           resolvedModel=new JSONObject(b.toString()).getJSONArray("data").getJSONObject(0).getString("id");
         }finally{models.disconnect();}
       }catch(Exception ignored){/* try configured alias */}
     }
     cancellation.ensureActive();
     JSONObject payload=new JSONObject().put("model",resolvedModel).put("messages",messages)
       .put("stream",false)
       .put("max_tokens",endpoint.equals("http://127.0.0.1:8080/v1/chat/completions")?128:(groq?800:1024));
     if(groq)payload.put("reasoning_effort","low");
     byte[] bytes=payload.toString().getBytes(StandardCharsets.UTF_8);
     try(OutputStream out=con.getOutputStream()){out.write(bytes);}
     int status=con.getResponseCode();
     if(status!=200) {
       if(groq) {
         if(status==401||status==403)throw new Exception("Groq anahtarı geçersiz veya yetkisiz (HTTP "+status+").");
         if(status==429)throw new Exception("Groq ücretsiz hesap kotası doldu (HTTP 429). Daha sonra tekrar dene ya da Hermes'i seç.");
         if(status>=500)throw new Exception("Groq servisi şu anda yanıt veremiyor (HTTP "+status+").");
         throw new Exception("Groq HTTP "+status+". Model / hesap ayarlarını kontrol et.");
       }
       throw new Exception(FreeCloudPolicy.httpMessage(status,endpoint.equals(FreeCloudPolicy.COMPLETIONS_URL)));
     }
     StringBuilder response=new StringBuilder();
     try(InputStream in=con.getInputStream();BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
       String line;while((line=br.readLine())!=null)response.append(line);
     }
     cancellation.ensureActive();
     // HTTP 200 can still contain an API error or a message with JSON null content.
     // Never display JSONObject.NULL as the literal word "null".
     JSONObject root=new JSONObject(response.toString());
     if(root.has("error") && !root.isNull("error")) {
       throw new Exception("Model sağlayıcısı bir hata döndürdü (HTTP 200). Lütfen başka zaman tekrar dene.");
     }
     JSONArray choices=root.optJSONArray("choices");
     if(choices==null || choices.length()==0)throw new Exception("Model cevap seçeneği döndürmedi.");
     JSONObject choice=choices.getJSONObject(0);
     JSONObject message=choice.optJSONObject("message");
     if(message==null)throw new Exception("Modelin mesaj alanı bulunamadı.");
     if(message.isNull("content")){
       // Reasoning-only responses can exhaust the output token budget.
       throw new Exception("Ücretsiz model cevap metni üretmedi (content=null). Tekrar deneyebilir veya farklı model seçebiliriz.");
     }
     Object content=message.opt("content");
     String answer="";
     if(content instanceof String){answer=(String)content;}
     else if(content instanceof JSONArray){
       JSONArray parts=(JSONArray)content;StringBuilder combined=new StringBuilder();
       for(int i=0;i<parts.length();i++){
         JSONObject part=parts.optJSONObject(i);
         if(part!=null && "text".equals(part.optString("type"))) combined.append(part.optString("text",""));
       }
       answer=combined.toString();
     }
     answer=answer.replaceAll("(?s)<think>.*?</think>","").trim();
     if(answer.isEmpty() || "null".equalsIgnoreCase(answer)){
       throw new Exception("Model bu istekte okunabilir metin üretmedi. Ücretsiz bir başka modele geçmek gerekebilir.");
     }
     return answer;
   }finally{cancellation.detach(con);con.disconnect();}
 }
}

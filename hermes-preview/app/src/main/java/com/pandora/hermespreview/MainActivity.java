package com.pandora.hermespreview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class MainActivity extends Activity {
    private static final String BASE = "http://127.0.0.1:8642";
    private static final String ALIAS = "pandora19_local_hermes_aes";
    private static final int SPEECH_REQUEST = 21;
    private static final int BLACK = Color.rgb(7,7,7), GRAY = Color.rgb(37,37,37);
    private SharedPreferences preferences;
    private LinearLayout conversation;
    private ScrollView scroller;
    private EditText prompt;
    private TextView connection;
    private Button cancelButton, sendButton, voiceButton;
    private volatile HttpURLConnection active;
    private volatile boolean busy;
    private volatile int requestVersion = 0;
    private TextToSpeech tts;
    private boolean ttsReady = false, readAloud = false;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        preferences = getSharedPreferences("pandora19", MODE_PRIVATE);
        readAloud = preferences.getBoolean("readAloud", false);
        setupUi();
        tts = new TextToSpeech(this, result -> {
            if(result == TextToSpeech.SUCCESS) {
                int language = tts.setLanguage(new Locale("tr","TR"));
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
        addMessage(false, "PANDORA 19 • Hermes yerel denemesi\n\nHermes Gateway Termux içinde açık olmalı. İlk bağlantı için Ayarlar'dan API anahtarını yalnızca bu uygulamaya gir. Anahtarını hiç kimseyle paylaşma.");
        if (!hasKey()) showSettings();
    }

    private void setupUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(17), dp(13), dp(17), dp(9));
        root.setBackgroundColor(BLACK);
        setContentView(root);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("P A N D O R A");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1));
        Button settings = button("AYAR", GRAY);
        settings.setOnClickListener(v -> showSettings());
        header.addView(settings);
        root.addView(header);

        connection = new TextView(this);
        connection.setText("HERMES · LOCALHOST:8642");
        connection.setTextColor(0xFFB7B7B7);
        connection.setTextSize(12);
        connection.setPadding(0,0,0,dp(10));
        root.addView(connection);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        Button check = button("BAĞLANTI TESTİ", GRAY);
        check.setOnClickListener(v -> testConnection());
        actions.addView(check, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button fresh = button("YENİ SOHBET", GRAY);
        LinearLayout.LayoutParams freshLp = new LinearLayout.LayoutParams(0, dp(46), 1);
        freshLp.leftMargin = dp(8);
        actions.addView(fresh,freshLp);
        fresh.setOnClickListener(v -> {
            preferences.edit().remove("session").apply();
            conversation.removeAllViews();
            addMessage(false, "Yeni Hermes oturumu başlatılacak.");
        });
        root.addView(actions);

        scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        conversation = new LinearLayout(this);
        conversation.setOrientation(LinearLayout.VERTICAL);
        conversation.setPadding(0, dp(20),0,dp(12));
        scroller.addView(conversation);
        root.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.BOTTOM);
        prompt = new EditText(this);
        prompt.setHint("PANDORA'ya yaz...");
        prompt.setTextColor(Color.WHITE);
        prompt.setHintTextColor(0xFF858585);
        prompt.setTextSize(15);
        prompt.setMinLines(1);
        prompt.setMaxLines(4);
        prompt.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        prompt.setBackground(shape(GRAY,12));
        prompt.setPadding(dp(12),dp(10),dp(12),dp(10));
        composer.addView(prompt,new LinearLayout.LayoutParams(0,-2,1));
        Button mic = button("🎙",GRAY);
        LinearLayout.LayoutParams micLp = new LinearLayout.LayoutParams(dp(49),dp(49));
        micLp.leftMargin = dp(6);
        composer.addView(mic,micLp);
        mic.setOnClickListener(v->requestSpeech());
        sendButton = button("➤",Color.WHITE);
        sendButton.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams sendLp=new LinearLayout.LayoutParams(dp(49),dp(49));
        sendLp.leftMargin=dp(6);
        composer.addView(sendButton,sendLp);
        sendButton.setOnClickListener(v->send());
        cancelButton=button("■",GRAY);
        LinearLayout.LayoutParams cancelLp=new LinearLayout.LayoutParams(dp(49),dp(49));
        cancelLp.leftMargin=dp(6);
        composer.addView(cancelButton,cancelLp);
        cancelButton.setVisibility(View.GONE);
        cancelButton.setOnClickListener(v->stopRequest());
        root.addView(composer);
        voiceButton=button(readAloud?"SES AÇIK":"SES KAPALI",BLACK);
        voiceButton.setOnClickListener(v->{
            readAloud=!readAloud;preferences.edit().putBoolean("readAloud",readAloud).apply();
            voiceButton.setText(readAloud?"SES AÇIK":"SES KAPALI");
            if(!readAloud && tts!=null)tts.stop();
        });
        root.addView(voiceButton,new LinearLayout.LayoutParams(-1,dp(42)));
    }

    private GradientDrawable shape(int color,int radius) {
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);g.setCornerRadius(dp(radius));
        return g;
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private Button button(String label,int color) {
        Button b=new Button(this);
        b.setText(label);b.setTextSize(11);b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(Color.WHITE);b.setBackground(shape(color,10));
        b.setPadding(dp(8),0,dp(8),0);
        return b;
    }
    private void addMessage(boolean mine,String body) {
        TextView bubble=new TextView(this);
        bubble.setText(body);
        bubble.setTextColor(mine?Color.BLACK:Color.WHITE);
        bubble.setTextSize(15);
        bubble.setPadding(dp(14),dp(13),dp(14),dp(13));
        bubble.setBackground(shape(mine?Color.WHITE:GRAY,15));
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-2,-2);
        layout.gravity=mine?Gravity.RIGHT:Gravity.LEFT;
        layout.setMargins(mine?dp(35):0,0,mine?0:dp(35),dp(10));
        conversation.addView(bubble,layout);
        scroller.post(()->scroller.fullScroll(View.FOCUS_DOWN));
        if(!mine && readAloud && ttsReady)tts.speak(body,TextToSpeech.QUEUE_FLUSH,null,"pandora-response");
    }
    private void status(String value){connection.setText(value);}
    private SecretKey secret() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        java.security.Key existing=ks.getKey(ALIAS,null);
        if(existing instanceof SecretKey)return (SecretKey) existing;
        KeyGenerator generator=KeyGenerator.getInstance("AES","AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }
    private void saveKey(String key) throws Exception {
        if(key.length()<32 || key.length()>256 || key.contains("\n") || key.contains("\r"))
            throw new IllegalArgumentException("En az 32 karakterlik geçerli Hermes API anahtarı gir.");
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, secret());
        byte[] encrypted=c.doFinal(key.getBytes(StandardCharsets.UTF_8));
        preferences.edit().putString("key_iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP))
            .putString("key_data",Base64.encodeToString(encrypted,Base64.NO_WRAP)).apply();
    }
    private boolean hasKey(){return !preferences.getString("key_data","").isEmpty();}
    private String readKey() throws Exception {
        byte[] iv=Base64.decode(preferences.getString("key_iv",""),Base64.NO_WRAP);
        byte[] data=Base64.decode(preferences.getString("key_data",""),Base64.NO_WRAP);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE,secret(),new GCMParameterSpec(128,iv));
        return new String(c.doFinal(data),StandardCharsets.UTF_8);
    }
    private void showSettings() {
        EditText field=new EditText(this);
        field.setHint(hasKey()?"Kayıtlı anahtarı korumak için boş bırak":"Hermes API_SERVER_KEY");
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setPadding(dp(12),dp(18),dp(12),dp(18));
        new AlertDialog.Builder(this).setTitle("PANDORA 19 · Yerel Hermes")
            .setMessage("Adres sabit: http://127.0.0.1:8642\n\nTermux içindeki ~/.hermes/.env dosyasındaki API_SERVER_KEY değerini yapıştır. Anahtar yalnızca Android Keystore ile şifreli kaydedilir. Bu uygulamayı ve erişim anahtarını başkalarıyla paylaşma.")
            .setView(field)
            .setNegativeButton("Kapat",null)
            .setNeutralButton("Anahtarı sil",(dialog,which)->{
                preferences.edit().remove("key_iv").remove("key_data").remove("session").apply();
                status("HERMES · ANAHTAR SİLİNDİ");
            })
            .setPositiveButton("Kaydet",(dialog,which)->{
                String key=field.getText().toString().trim();
                if(key.isEmpty()&&hasKey())return;
                try {saveKey(key); status("HERMES · ANAHTAR KAYDEDİLDİ");}
                catch(Exception error){new AlertDialog.Builder(this).setMessage(error.getMessage()).setPositiveButton("Tamam",null).show();}
            }).show();
    }
    private String session() {
        String old=preferences.getString("session","");
        if(!old.isEmpty())return old;
        String created="pandora19-"+UUID.randomUUID();
        preferences.edit().putString("session",created).apply();
        return created;
    }
    private String request(String path,String method,String payload) throws Exception {
        HttpURLConnection conn=(HttpURLConnection)new URL(BASE+path).openConnection();
        active=conn;
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(120000);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("Authorization","Bearer "+readKey());
        conn.setRequestProperty("Accept","application/json");
        if(payload!=null){
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
            conn.setRequestProperty("X-Hermes-Session-Id",session());
            conn.setRequestProperty("X-Hermes-Session-Key","agent:main:pandora19:dm:owner");
        }
        try {
            if(payload!=null){try(OutputStream out=conn.getOutputStream()){out.write(payload.getBytes(StandardCharsets.UTF_8));}}
            int code=conn.getResponseCode();
            if(code!=200)throw new IllegalStateException("Hermes HTTP "+code+(code==429?" · kullanım sınırı":" · bağlantıyı ve Gateway'i kontrol et"));
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            try(InputStream in=conn.getInputStream()){
                byte[] b=new byte[8192]; int count;
                while((count=in.read(b))!=-1){
                    if(buffer.size()+count>1048576)throw new IllegalStateException("Yanıt çok büyük.");
                    buffer.write(b,0,count);
                }
            }
            return buffer.toString("UTF-8");
        }finally {active=null;conn.disconnect();}
    }
    private void testConnection() {
        if(busy)return;
        if(!hasKey()){showSettings();return;}
        busy=true;status("HERMES · BAĞLANIYOR");
        new Thread(()->{
            String output;
            try {
                JSONObject result=new JSONObject(request("/v1/models","GET",null));
                JSONArray models=result.getJSONArray("data");
                if(models.length()<1)throw new IllegalStateException("Model listesi boş.");
                output="Hermes bağlantısı başarılı · "+models.getJSONObject(0).optString("id","hermes-agent");
            }catch(Exception ex){output="Bağlantı hatası: "+ex.getMessage();}
            final String text=output;
            runOnUiThread(()->{busy=false;status("HERMES · LOCALHOST:8642");addMessage(false,text);});
        },"pandora19-test").start();
    }
    private void send() {
        if(busy)return;
        String question=prompt.getText().toString().trim();
        if(question.isEmpty())return;
        if(!hasKey()){showSettings();return;}
        prompt.setText("");
        addMessage(true,question);
        busy=true;sendButton.setEnabled(false);
        cancelButton.setVisibility(View.VISIBLE);status("HERMES · YANIT BEKLENİYOR");
        int version=++requestVersion;
        new Thread(()->{
            String response;
            try{
                JSONArray messages=new JSONArray();
                messages.put(new JSONObject().put("role","system").put("content",
                  "Sen PANDORA adlı Türkçe Android asistanının Hermes motorusun. Türkçe yanıt ver. Telefonu veya uygulamaları kontrol ettiğini iddia etme. Dışa veri gönderme, ödeme, silme veya terminal komutlarını açık onay olmadan çalıştırma. Kullanıcı normal sohbet ediyorsa araç kullanma."));
                messages.put(new JSONObject().put("role","user").put("content",question));
                JSONObject payload=new JSONObject().put("model","hermes-agent").put("stream",false).put("messages",messages);
                JSONObject obj=new JSONObject(request("/v1/chat/completions","POST",payload.toString()));
                JSONObject choice=obj.getJSONArray("choices").getJSONObject(0);
                String finish=choice.optString("finish_reason","");
                if("cancelled".equals(finish)||"error".equals(finish))throw new IllegalStateException("Yanıt tamamlanamadı: "+finish);
                response=choice.getJSONObject("message").optString("content","");
                if(response.trim().isEmpty())throw new IllegalStateException("Yanıt boş.");
            }catch(Exception ex){response="Hermes yanıt hatası: "+ex.getMessage();}
            final String answer=response;
            runOnUiThread(()->{
                if(version!=requestVersion||isFinishing())return;
                busy=false;sendButton.setEnabled(true);cancelButton.setVisibility(View.GONE);
                status("HERMES · LOCALHOST:8642");addMessage(false,answer);
            });
        },"pandora19-chat").start();
    }
    private void stopRequest() {
        ++requestVersion;
        HttpURLConnection conn=active;
        if(conn!=null)conn.disconnect();
        busy=false;sendButton.setEnabled(true);
        cancelButton.setVisibility(View.GONE);
        status("HERMES · İPTAL EDİLDİ");
    }
    private void requestSpeech() {
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR");
        try {startActivityForResult(intent,SPEECH_REQUEST);}
        catch(ActivityNotFoundException ex){Toast.makeText(this,"Ses tanıma uygulaması bulunamadı.",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==SPEECH_REQUEST&&resultCode==RESULT_OK&&data!=null){
            ArrayList<String> transcript=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(transcript!=null&&!transcript.isEmpty())prompt.setText(transcript.get(0));
        }
    }
    @Override protected void onDestroy() {
        stopRequest();
        if(tts!=null){tts.stop();tts.shutdown();}
        super.onDestroy();
    }
}

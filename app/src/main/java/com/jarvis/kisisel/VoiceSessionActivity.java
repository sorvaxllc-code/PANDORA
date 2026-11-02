package com.jarvis.kisisel;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.view.Gravity;
import android.widget.*;
/** A silent voice surface backed by the same microphone service as background wake detection. */
public final class VoiceSessionActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView state,heard,answer;private OrbView wave;private boolean visible;
    private final Runnable refresh=new Runnable(){public void run(){
        if(!visible)return;
        state.setText(VoiceSessionState.phase);heard.setText(VoiceSessionState.transcript);answer.setText(VoiceSessionState.answer);
        wave.setActive("Dinliyorum".equals(VoiceSessionState.phase)||"Yanıtı okuyorum".equals(VoiceSessionState.phase));
        handler.postDelayed(this,300);
    }};
    @Override public void onCreate(Bundle saved){super.onCreate(saved);
        getWindow().setStatusBarColor(0xFF070707);getWindow().setNavigationBarColor(0xFF070707);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(32,32,32,32);root.setBackgroundColor(0xFF070707);setContentView(root);
        root.addView(label("P A N D O R A",18));state=label("Hazırlanıyor",15);root.addView(state);
        wave=new OrbView(this);root.addView(wave,new LinearLayout.LayoutParams(-1,(int)(getResources().getDisplayMetrics().density*220)));
        ScrollView scroll=new ScrollView(this);LinearLayout text=new LinearLayout(this);text.setOrientation(1);heard=label("",19);answer=label("",17);heard.setTextIsSelectable(true);answer.setTextIsSelectable(true);text.addView(heard);text.addView(answer);scroll.addView(text);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        button(root,"Tekrar konuş",()->send(WakeMonitorService.ACTION_LISTEN));
        button(root,"Yanıtı / işlemi durdur",()->send(WakeMonitorService.ACTION_CANCEL));
        button(root,"Sohbete dön",this::finish);
        root.addView(label("Bu ekranı kapattığında Hey Pandora dinlemesi sürer. Menüden veya kalıcı bildirimden kapatabilirsin.",12));
        if(saved==null && !PandoraAlerts.headsetConnected(this)){VoiceSessionState.set("Sesli kullanım için kulaklık bağla");}
        else if(saved==null){
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);
            else send(WakeMonitorService.ACTION_LISTEN);
        }
    }
    private TextView label(String text,int size){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setPadding(8,12,8,12);return t;}
    private void button(LinearLayout root,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->action.run());root.addView(b,new LinearLayout.LayoutParams(-1,-2));}
    private void send(String action){
        if(WakeMonitorService.ACTION_CANCEL.equals(action)&&!WakeMonitorService.running)return;
        if(!PandoraAlerts.headsetConnected(this)){VoiceSessionState.set("Sesli kullanım için kulaklık bağla");return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){state.setText("Mikrofon izni gerekiyor");return;}
        try{startForegroundService(new Intent(this,WakeMonitorService.class).setAction(action));}
        catch(RuntimeException e){VoiceSessionState.set("Dinleme başlatılamadı · uygulamayı yeniden aç");}
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);
        if(code==1&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)send(WakeMonitorService.ACTION_LISTEN);
        else {VoiceSessionState.set("Mikrofon izni verilmedi");}
    }
    @Override protected void onResume(){super.onResume();visible=true;handler.post(refresh);}
    @Override protected void onPause(){visible=false;handler.removeCallbacks(refresh);wave.setActive(false);super.onPause();}
}

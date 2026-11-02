package com.jarvis.kisisel;
import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.*;
/** Small user-invoked sheet. Resolves contact through Android picker; never guesses a recipient. */
public final class DeviceTaskActivity extends Activity {
    private String command="",recipient="",number="",body="";
    private static final int PICK=81;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        command=getIntent().getStringExtra("command");if(command==null)command="";
        if(state!=null){body=state.getString("body","");recipient=state.getString("recipient","");number=state.getString("number","");}
        if("VIDEO".equals(getIntent().getStringExtra("kind")))video();else message();
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("body",body);state.putString("recipient",recipient);state.putString("number",number);}
    private void video(){
        new AlertDialog.Builder(this).setTitle("Görüntülü görüşme")
            .setMessage("Mevcut görüşmenin görüntülüye geçiş düğmesini deneyebilirim. Başka bir uygulamada aynı kişiyi otomatik arama ve normal aramayı aktarma henüz desteklenmiyor; mevcut aramanı sonlandırmam.")
            .setPositiveButton("Mevcut görüşmede dene",(d,w)->{
                finish();new android.os.Handler(getMainLooper()).postDelayed(()->{
                    String result=PandoraAccessibilityService.requestVideoSwitch();
                    try(MemoryDb db=new MemoryDb(getApplicationContext())){db.message("pandora",result);}
                    Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG).show();
                },500);
            }).setNeutralButton("WhatsApp görüşmesinde dene",(d,w)->{
                finish();new android.os.Handler(getMainLooper()).postDelayed(()->{
                    String result=PandoraAccessibilityService.requestWhatsAppVideoSwitch();
                    try(MemoryDb db=new MemoryDb(getApplicationContext())){db.message("pandora",result);}
                    Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG).show();
                },500);
            }).setNegativeButton("Vazgeç",(d,w)->finish()).setOnCancelListener(d->finish()).show();
    }
    private void message(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(32,12,32,12);
        TextView request=new TextView(this);request.setText("İsteğin: "+command);box.addView(request);
        Button contact=new Button(this);contact.setText(number.isEmpty()?"Alıcıyı rehberden seç":recipient+" · "+number);box.addView(contact);
        EditText text=new EditText(this);text.setHint("Gönderilecek mesaj");text.setText(body);box.addView(text);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Mesaj hazırla").setView(box)
            .setPositiveButton("Mesaj uygulamasında aç",null).setNegativeButton("Vazgeç",(d,w)->finish())
            .setOnCancelListener(d->finish()).create();
        contact.setOnClickListener(v->{body=text.getText().toString();dialog.dismiss();
            try{startActivityForResult(new Intent(Intent.ACTION_PICK,ContactsContract.CommonDataKinds.Phone.CONTENT_URI),PICK);}
            catch(RuntimeException e){message();Toast.makeText(this,"Rehber seçici açılamadı.",Toast.LENGTH_LONG).show();}});
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
            body=text.getText().toString().trim();
            if(number.isEmpty()||body.isEmpty()){Toast.makeText(this,"Alıcıyı ve mesajı belirtmelisin.",Toast.LENGTH_SHORT).show();return;}
            try{startActivity(new Intent(Intent.ACTION_SENDTO,Uri.fromParts("smsto",number,null)).putExtra("sms_body",body));
                try(MemoryDb db=new MemoryDb(this)){db.message("pandora",recipient+" için mesaj taslağı açıldı. Gönderim mesaj uygulamasında tamamlanmalı.");}finish();
            }catch(RuntimeException e){Toast.makeText(this,"Mesaj uygulaması bulunamadı.",Toast.LENGTH_LONG).show();}
        }));dialog.show();
    }
    @Override protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);
        if(code==PICK){
            if(result==RESULT_OK && data!=null && data.getData()!=null){
                try(Cursor cursor=getContentResolver().query(data.getData(),new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,null)){
                    if(cursor!=null&&cursor.moveToFirst()){recipient=cursor.getString(0);number=cursor.getString(1);}
                }catch(RuntimeException e){number="";}
            }message();
        }
    }
}

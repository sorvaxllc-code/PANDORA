package com.jarvis.kisisel;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import java.util.List;
/** Paginated, read-only archive. New conversations never delete old messages. */
public final class HistoryActivity extends Activity {
    private LinearLayout results;private EditText search;private Button more;private long before=Long.MAX_VALUE;private String query="";
    @Override public void onCreate(Bundle state){super.onCreate(state);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,24,24,24);root.setBackgroundColor(0xFF070707);setContentView(root);
        TextView title=new TextView(this);title.setText("Konuşma geçmişi");title.setTextColor(Color.WHITE);title.setTextSize(24);root.addView(title);
        search=new EditText(this);search.setTextColor(Color.WHITE);search.setHintTextColor(0xFFAAAAAA);search.setHint("Tüm konuşmalarda ara");search.setSingleLine();root.addView(search);
        Button find=new Button(this);find.setText("Ara");root.addView(find);find.setOnClickListener(v->{query=search.getText().toString().trim();before=Long.MAX_VALUE;results.removeAllViews();load();});
        ScrollView scroll=new ScrollView(this);results=new LinearLayout(this);results.setOrientation(1);scroll.addView(results);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        more=new Button(this);more.setText("Daha eski mesajlar");more.setOnClickListener(v->load());root.addView(more);
        Button export=new Button(this);export.setText("Geçmişi dosyaya kaydet");export.setOnClickListener(v->{
            try{startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain").putExtra(Intent.EXTRA_TITLE,"Pandora-gecmis.txt"),9);}
            catch(RuntimeException e){Toast.makeText(this,"Dosya seçici açılamadı.",Toast.LENGTH_LONG).show();}
        });root.addView(export);load();
    }
    private void load(){
        List<String[]> rows;try(MemoryDb db=new MemoryDb(this)){rows=db.searchMessages(query,before);}
        for(String[] row:rows){TextView text=new TextView(this);text.setTextColor(Color.WHITE);text.setTextSize(16);text.setTextIsSelectable(true);text.setPadding(8,20,8,20);
            text.setText("Sohbet "+row[4]+" · "+("user".equals(row[1])?"Sen":"Pandora")+" · "+java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new java.util.Date(Long.parseLong(row[3])))+"\n\n"+row[2]);results.addView(text);before=Long.parseLong(row[0]);}
        more.setEnabled(rows.size()==50);if(rows.isEmpty()&&before==Long.MAX_VALUE){TextView empty=new TextView(this);empty.setTextColor(Color.LTGRAY);empty.setText("Kayıt bulunamadı.");results.addView(empty);}
    }
    @Override protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);
        if(code!=9||result!=RESULT_OK||data==null||data.getData()==null)return;
        android.net.Uri uri=data.getData();
        new Thread(()->{
            String outcome;
            try(MemoryDb db=new MemoryDb(getApplicationContext());java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
                if(out==null)throw new java.io.IOException();
                try(java.io.Writer writer=new java.io.OutputStreamWriter(out,java.nio.charset.StandardCharsets.UTF_8)){db.exportMessages(writer);}outcome="Geçmiş kaydedildi.";
            }catch(Exception e){outcome="Dosya kaydedilemedi.";}
            final String message=outcome;runOnUiThread(()->Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show());
        },"pandora-export").start();
    }
}

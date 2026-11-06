package com.jarvis.kisisel.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.jarvis.kisisel.MemoryDb;
import com.jarvis.kisisel.ai.ModelRouter;
import com.jarvis.kisisel.ai.HttpAiProvider;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A real bounded 3-call workflow, NOT a list of fictional agent names.
 *
 * 1) Route user objective to a specialist.
 * 2) Another specialist independently reviews the user objective.
 * 3) The captain synthesizes both actual responses.
 *
 * All three calls use the user's selected configured model. No unapproved
 * account actions, browsing or device calls. Progress only marks an expert
 * "done" after receiving a provider result.
 */
public final class CaptainTeam implements AutoCloseable {
    public interface Listener {
        void progress(String label,String detail,int step);
        void complete(String goal,String answer);
        void failed(String reason);
    }
    private final Context app;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final SharedPreferences state;
    private MemoryDb db;
    private HttpAiProvider.CancelToken active;
    private boolean cancelled=false,busy=false;
    private int serial=0;

    public CaptainTeam(Context context){
        app=context.getApplicationContext();
        state=app.getSharedPreferences("pandora_captain_tasks_v26",Context.MODE_PRIVATE);
    }
    public synchronized boolean busy(){return busy;}
    public synchronized void cancel(){
        cancelled=true;
        serial++;
        busy=false;
        if(active!=null){active.cancel();active=null;}
        if(db!=null){db.close();db=null;}
    }
    public void start(String objective,Listener listener){
        final CaptainPlanner.Plan plan=CaptainPlanner.forGoal(objective);
        if(plan.goal.isEmpty()){listener.failed("Önce ekip için bir görev yaz.");return;}
        final int ticket;
        synchronized(this){
            if(busy){listener.failed("Kaptan şu anda önceki işi yürütüyor.");return;}
            busy=true;cancelled=false;ticket=++serial;
            db=new MemoryDb(app);
        }
        publish(ticket,listener,"PANDORA · Kaptan",
            "Görevi "+plan.lead+" ve "+plan.reviewer+" uzmanlarına ayırdı.",0);
        call(ticket,listener,plan.lead,
            CaptainPlanner.specialistInstructions(plan.lead,plan.goal),first->{
            publish(ticket,listener,plan.lead,"Uzman görüşü tamamlandı.",1);
            call(ticket,listener,plan.reviewer,
                CaptainPlanner.specialistInstructions(plan.reviewer,plan.goal),second->{
                publish(ticket,listener,plan.reviewer,"İkinci uzman görüşü tamamlandı.",2);
                call(ticket,listener,"PANDORA · Kaptan",
                    CaptainPlanner.captainInstructions(plan,first,second),finalText->{
                    if(!valid(ticket))return;
                    save(plan.goal,finalText,plan.lead,plan.reviewer);
                    synchronized(CaptainTeam.this){
                        if(serial!=ticket)return;
                        busy=false;active=null;
                        if(db!=null){db.close();db=null;}
                    }
                    main.post(()->listener.complete(plan.goal,finalText));
                });
            });
        });
    }
    private interface Result {void got(String text);}
    private void call(int ticket,Listener listener,String role,String prompt,Result result){
        if(!valid(ticket))return;
        publish(ticket,listener,role,"Yapay zekâ modeli yanıtı hazırlanıyor…",role.contains("Kaptan")?3:1);
        ModelRouter router;
        synchronized(this){
            if(!valid(ticket))return;
            router=new ModelRouter(app,db);
        }
        if("OFF".equals(router.mode())){
            failure(ticket,listener,"Yapay zekâ modu kapalı. Önce Groq veya Hermes'i seç.");
            return;
        }
        try{
            HttpAiProvider.CancelToken next=router.askTeamAsync(prompt,(answer,source)->{
                if(!valid(ticket))return;
                if(source==null||source.isEmpty()){
                    failure(ticket,listener,"Uzman yanıt veremedi: "+(answer==null?"bilinmeyen":answer));
                    return;
                }
                if(answer==null||answer.trim().isEmpty()){
                    failure(ticket,listener,"Model boş yanıt döndürdü; uzman görevi tamamlanmadı.");
                    return;
                }
                result.got(answer.trim());
            });
            synchronized(this){
                if(valid(ticket))active=next;
                else next.cancel();
            }
        }catch(RuntimeException e){
            failure(ticket,listener,"Uzman bağlantısı başlatılamadı: "+e.getClass().getSimpleName());
        }
    }
    private synchronized boolean valid(int ticket){
        return !cancelled && busy && serial==ticket;
    }
    private void publish(int ticket,Listener listener,String label,String detail,int step){
        if(valid(ticket))main.post(()->{if(valid(ticket))listener.progress(label,detail,step);});
    }
    private void failure(int ticket,Listener listener,String reason){
        if(!valid(ticket))return;
        synchronized(this){
            if(serial!=ticket)return;
            busy=false;
            if(active!=null){active.cancel();active=null;}
            if(db!=null){db.close();db=null;}
        }
        main.post(()->listener.failed(reason));
    }
    private void save(String goal,String answer,String lead,String reviewer){
        // User-device-only task summary. No keys, audio, screenshots or private files.
        JSONArray list=history();
        JSONObject task=new JSONObject();
        try{
            task.put("goal",goal);
            task.put("captain",answer.length()>9000?answer.substring(0,9000):answer);
            task.put("experts",lead+" + "+reviewer);
            task.put("timestamp",System.currentTimeMillis());
            JSONArray next=new JSONArray().put(task);
            for(int i=0;i<Math.min(14,list.length());i++)next.put(list.optJSONObject(i));
            state.edit().putString("history",next.toString()).apply();
        }catch(Exception ignored){}
    }
    public JSONArray history(){
        try{return new JSONArray(state.getString("history","[]"));}
        catch(Exception ignored){return new JSONArray();}
    }
    @Override public void close(){cancel();}
}
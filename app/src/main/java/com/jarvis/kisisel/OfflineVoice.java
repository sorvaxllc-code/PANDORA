package com.jarvis.kisisel;
import android.content.Context;
import android.media.*;
import android.os.*;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;
import org.json.JSONObject;
/** Local wake detector and Turkish command recognizer share one microphone, without chimes. */
public final class OfflineVoice implements AutoCloseable {
    public interface Listener {void partial(String text);void result(String text);void error(); default void ready(){} default void idle(){} default void transcript(String text){} }
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Listener listener;
    private Model turkish,wakeModel;
    private volatile boolean closed,paused,reset,manual;
    private volatile AudioRecord recorder;
    private Thread worker;
    public OfflineVoice(Context context,Listener listener){
        this.listener=listener;
        StorageService.unpack(context,"model-tr","pandora-model-tr",tr->{
            if(closed){tr.close();return;}turkish=tr;
            StorageService.unpack(context,"model-wake","pandora-model-wake",en->{
                if(closed){en.close();return;}wakeModel=en;
                worker=new Thread(this::run,"pandora-local-voice");worker.start();
            },e->{if(!closed){close();listener.error();}});
        },e->{if(!closed)listener.error();});
    }
    private static String value(String json,String key){try{return new JSONObject(json).optString(key,"");}catch(Exception e){return "";}}
    @android.annotation.SuppressLint("MissingPermission")
    private void run(){
        Recognizer wake=null,command=null;AudioRecord audio=null;
        try{
            wake=new Recognizer(wakeModel,16000f,"[\"hey pandora\",\"[unk]\"]");
            command=new Recognizer(turkish,16000f);
            int minimum=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            audio=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(6400,minimum));
            recorder=audio;
            if(audio.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("Microphone unavailable");
            audio.startRecording();main.post(()->{if(!closed)listener.ready();});short[] chunk=new short[1600];boolean activated=false;long deadline=0;
            while(!closed){
                int count=audio.read(chunk,0,chunk.length);
                if(count<0){if(closed)break;throw new IllegalStateException("Audio capture failed");}
                if(reset){reset=false;wake.reset();command.reset();activated=false;}
                if(manual){manual=false;paused=false;wake.reset();command.reset();activated=true;deadline=SystemClock.elapsedRealtime()+16000;
                    main.post(()->{if(!closed)listener.partial("hey pandora");});}
                if(paused||count==0)continue;
                if(!activated){
                    boolean finalWake=wake.acceptWaveForm(chunk,count);
                    String words=value(finalWake?wake.getResult():wake.getPartialResult(),finalWake?"text":"partial");
                    if(WakeWordMatcher.matches(words)){
                        activated=true;deadline=SystemClock.elapsedRealtime()+16000;command.reset();
                        main.post(()->{if(!closed&&!paused)listener.partial("hey pandora");});
                    }
                }else{
                    boolean ended=command.acceptWaveForm(chunk,count);
                    if(!ended){String partial=value(command.getPartialResult(),"partial");
                        main.post(()->{if(!closed&&!paused)listener.transcript(partial);});}
                    if(ended || SystemClock.elapsedRealtime()>deadline){
                        String text=value(ended?command.getResult():command.getFinalResult(),"text").trim();
                        if(!text.isEmpty()){
                            paused=true;activated=false;wake.reset();
                            main.post(()->{if(!closed)listener.result("hey pandora "+text);});
                        }else if(SystemClock.elapsedRealtime()>deadline){activated=false;wake.reset();main.post(()->{if(!closed)listener.idle();});}
                    }
                }
            }
        }catch(Exception e){if(!closed)main.post(()->{if(!closed)listener.error();});}
        finally{
            if(audio!=null){try{audio.stop();}catch(RuntimeException ignored){}audio.release();}recorder=null;
            if(wake!=null)wake.close();if(command!=null)command.close();
            if(wakeModel!=null){wakeModel.close();wakeModel=null;}if(turkish!=null){turkish.close();turkish=null;}
        }
    }
    public void beginCommand(){if(!closed){reset=true;manual=true;paused=false;}}
    public void pause(){manual=false;paused=true;}
    public void resume(){if(closed)return;reset=true;paused=false;}
    public void close(){
        closed=true;AudioRecord audio=recorder;if(audio!=null)try{audio.stop();}catch(RuntimeException ignored){}
        if(worker==null && turkish!=null){turkish.close();turkish=null;}
    }
}

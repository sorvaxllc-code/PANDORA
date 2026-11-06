package com.jarvis.kisisel;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;

/**
 * Offline headset-only, always-ready hotword detector.
 *
 * Uses AudioRecord + Vosk, NOT Android SpeechRecognizer.startListening:
 * the platform recognizer emits a start/stop chime on some handsets.
 * The recorder is stopped during calls, disconnection, TTS and user pauses.
 * Raw audio is processed in-memory on device and is never sent to a server.
 */
public final class OfflineVoice implements AutoCloseable {
    public interface Listener {
        void partial(String text);
        void result(String text);
        void error(String reason);
    }

    private final Context app;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Listener listener;
    private volatile boolean closed=false,paused=true,reset=false;
    private volatile AudioRecord recorder;
    private volatile Thread worker;
    private Model wakeModel, turkish;

    public OfflineVoice(Context context, Listener callback){
        app=context.getApplicationContext();
        listener=callback;
        StorageService.unpack(app,"model-tr","pandora23-model-tr",tr->{
            if(closed){tr.close();return;}
            turkish=tr;
            StorageService.unpack(app,"model-wake","pandora23-model-wake",en->{
                if(closed){en.close();return;}
                wakeModel=en;
                worker=new Thread(this::process,"pandora-offline-hotword");
                worker.start();
            },error->report("Uyandırma modeli açılamadı"));
        },error->report("Türkçe konuşma modeli açılamadı"));
    }

    private void report(String message){
        if(closed)return;
        main.post(()->{if(!closed)listener.error(message);});
    }
    private static String field(String raw,String key){
        try{return new JSONObject(raw).optString(key,"");}
        catch(Exception ignored){return "";}
    }
    private boolean mayCapture(){
        return !closed && !paused
            && PandoraAlerts.headsetConnected(app)
            && !PandoraAlerts.inCall(app);
    }
    @android.annotation.SuppressLint("MissingPermission")
    private void process(){
        Recognizer wake=null,command=null;
        AudioRecord audio=null;
        boolean capturing=false;
        try{
            // Limited vocabulary prevents arbitrary conversations from being commands.
            wake=new Recognizer(wakeModel,16000f,"[\"hey pandora\",\"[unk]\"]");
            command=new Recognizer(turkish,16000f);
            int min=AudioRecord.getMinBufferSize(16000,
                AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(min<=0)throw new IllegalStateException("Bu cihazda 16 kHz mikrofon bulunamadı");
            audio=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                16000,AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,Math.max(min,6400));
            recorder=audio;
            if(audio.getState()!=AudioRecord.STATE_INITIALIZED)
                throw new IllegalStateException("Mikrofon açılamadı");
            short[] samples=new short[1600]; // 100ms at 16kHz
            boolean activated=false;
            long deadline=0;
            while(!closed){
                if(!mayCapture()){
                    if(capturing)try{audio.stop();}catch(IllegalStateException ignored){}
                    capturing=false;
                    activated=false;
                    Thread.sleep(125);
                    continue;
                }
                if(!capturing){
                    audio.startRecording();
                    capturing=true;
                    reset=true;
                }
                if(reset){
                    reset=false;
                    wake.reset();
                    command.reset();
                    activated=false;
                }
                int count=audio.read(samples,0,samples.length);
                if(count<=0){
                    if(closed||!mayCapture())continue;
                    throw new IllegalStateException("Mikrofon okuma hatası: "+count);
                }
                if(!mayCapture())continue;
                if(!activated){
                    boolean complete=wake.acceptWaveForm(samples,count);
                    String spoken=field(complete?wake.getResult():wake.getPartialResult(),
                        complete?"text":"partial");
                    if(WakeWordMatcher.matches(spoken)){
                        activated=true;
                        deadline=SystemClock.elapsedRealtime()+14000L;
                        command.reset();
                        main.post(()->{if(mayCapture())listener.partial("hey pandora");});
                    }
                }else {
                    boolean complete=command.acceptWaveForm(samples,count);
                    boolean timedOut=SystemClock.elapsedRealtime()>deadline;
                    if(complete||timedOut){
                        String recognized=field(complete?command.getResult():
                            command.getFinalResult(),"text").trim();
                        if(!recognized.isEmpty()){
                            // Pause capture before passing a command to the assistant.
                            paused=true;
                            activated=false;
                            wake.reset();
                            final String phrase="hey pandora "+recognized;
                            main.post(()->{if(!closed)listener.result(phrase);});
                        }else if(timedOut){
                            activated=false;
                            wake.reset();
                        }
                    }
                }
            }
        }catch(Exception ex){
            if(!closed)report("Sessiz ses algılayıcı başlatılamadı: "+ex.getClass().getSimpleName());
        }finally{
            if(audio!=null){
                try{audio.stop();}catch(IllegalStateException ignored){}
                audio.release();
            }
            recorder=null;
            if(wake!=null)wake.close();
            if(command!=null)command.close();
            if(wakeModel!=null){wakeModel.close();wakeModel=null;}
            if(turkish!=null){turkish.close();turkish=null;}
        }
    }
    public void pause(){
        paused=true;
        AudioRecord audio=recorder;
        if(audio!=null && audio.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING){
            try{audio.stop();}catch(IllegalStateException ignored){}
        }
    }
    public void resume(){
        if(closed)return;
        reset=true;
        paused=false;
    }
    @Override public void close(){
        closed=true;
        pause();
        Thread thread=worker;
        if(thread!=null)thread.interrupt();
        // Worker owns resources and releases them in its finally block.
        if(worker==null){
            if(wakeModel!=null){wakeModel.close();wakeModel=null;}
            if(turkish!=null){turkish.close();turkish=null;}
        }
    }
}
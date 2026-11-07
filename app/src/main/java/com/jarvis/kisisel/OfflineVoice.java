package com.jarvis.kisisel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;
import com.jarvis.kisisel.safety.StopPhraseGate;

/**
 * PANDORA hotword pipeline:
 *  - Mic input must actually be routed to a headset (never an accidental phone mic).
 *  - ONE continuous AudioRecord stream, no Android SpeechRecognizer sessions or chimes.
 *  - Model recognition is gated by a final utterance and word confidence.
 *  - During assistant replies, audio frames are immediately discarded instead of
 *    repeatedly closing/reopening HFP mic and triggering headset activation tones.
 *  - On disconnection/calls recording stops and Bluetooth route is relinquished.
 *
 * Audio is not persisted, logged, uploaded or fed to the LLM until an explicit wake.
 */
public final class OfflineVoice implements AutoCloseable {
    public interface Listener {
        void wake();
        void result(String phrase);
        void status(String state);
        void error(String reason);
        void emergencyStop();
    }
    private final Context app;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final HeadsetAudioRoute route;
    private volatile boolean closed=false;
    private volatile boolean muted=true;
    private volatile boolean reset=true;
    private volatile AudioRecord recorder;
    private volatile Thread worker;
    private volatile String state="Çevrimdışı ses modelleri hazırlanıyor";
    private Model wakeModel,turkishModel;

    public OfflineVoice(Context context,Listener listener){
        this.app=context.getApplicationContext();
        this.listener=listener;
        this.route=new HeadsetAudioRoute(app);
        notifyState(state);
        StorageService.unpack(app,"model-tr","pandora24-model-tr",tr->{
            if(closed){tr.close();return;}
            turkishModel=tr;
            StorageService.unpack(app,"model-wake","pandora24-model-wake",en->{
                if(closed){en.close();return;}
                wakeModel=en;
                Thread task=new Thread(this::process,"pandora-headset-wake");
                worker=task;
                task.start();
            },e->fail("Uyandırma modeli yüklenemedi"));
        },e->fail("Türkçe konuşma modeli yüklenemedi"));
    }
    private void notifyState(String next){
        if(next.equals(state))return;
        state=next;
        main.post(()->{if(!closed)listener.status(next);});
    }
    public String status(){return state;}
    public boolean isCapturing(){
        AudioRecord r=recorder;
        return r!=null && r.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING;
    }
    private void fail(String msg){
        if(closed)return;
        notifyState(msg);
        main.post(()->{if(!closed)listener.error(msg);});
    }
    private static JSONObject parsed(String json){
        try{return new JSONObject(json);}catch(Exception e){return new JSONObject();}
    }
    private static String transcription(JSONObject result){
        return result.optString("text","").trim();
    }
    /**
     * Vosk includes per-word conf when Recognizer#setWords(true).
     * Missing scores are represented by -1, not fabricated confidence.
     */
    private static double confidence(JSONObject result){
        JSONArray words=result.optJSONArray("result");
        if(words==null||words.length()==0)return -1;
        double sum=0;int count=0;
        for(int i=0;i<words.length();i++){
            JSONObject w=words.optJSONObject(i);
            if(w==null||!w.has("conf"))continue;
            sum+=w.optDouble("conf",0);
            count++;
        }
        return count==0?-1:sum/count;
    }
    private static long voicedDuration(short[] audio,int count){
        if(count<=0)return 0;
        double sq=0;
        for(int i=0;i<count;i++){
            double v=audio[i];
            sq+=v*v;
        }
        double rms=Math.sqrt(sq/count);
        // This is only a sanity gate; Vosk's speech model provides the main
        // language verification, which filters machine noise better than RMS.
        return rms>420 ? (count*1000L)/16000L : 0L;
    }
    private boolean hasOutput(){
        return PandoraAlerts.headsetConnected(app) && !PandoraAlerts.inCall(app);
    }
    @SuppressLint("MissingPermission")
    private void process(){
        Recognizer wake=null,command=null,stopWord=null;
        AudioRecord mic=null;
        boolean live=false,activated=false;
        long wakeVoicedMs=0,commandVoicedMs=0,stopVoicedMs=0;
        long wakeDeadline=0;
        long nextRouteAttempt=0;
        long lastRouteCheck=0;
        try{
            wake=new Recognizer(wakeModel,16000f,
                "[\"pandora\",\"hey pandora\",\"hi pandora\",\"[unk]\"]");
            wake.setWords(true);
            command=new Recognizer(turkishModel,16000f);
            command.setWords(true);
            // Lightweight separate Turkish grammar: recognizes an exact
            // emergency stop while assistant TTS/model work has normal
            // recognition muted; it reuses the SAME AudioRecord stream.
            stopWord=new Recognizer(turkishModel,16000f,
                "[\"dur\",\"iptal et\",\"pandora dur\",\"[unk]\"]");
            stopWord.setWords(true);
            int min=AudioRecord.getMinBufferSize(16000,
                AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(min<=0)throw new IllegalStateException("16 kHz mikrofon desteklenmiyor");
            short[] frame=new short[1600];

            while(!closed){
                if(!hasOutput()){
                    if(mic!=null){
                        try{mic.stop();}catch(RuntimeException ignored){}
                        mic.release();mic=null;recorder=null;
                    }
                    live=false;activated=false;
                    route.releaseSelection();
                    notifyState(PandoraAlerts.headsetConnected(app)?
                        "Telefon görüşmesi; mikrofon kapalı" :
                        "Kulaklık bekleniyor; mikrofon kapalı");
                    Thread.sleep(350);
                    continue;
                }

                if(mic==null){
                    if(SystemClock.elapsedRealtime()<nextRouteAttempt){
                        Thread.sleep(250);continue;
                    }
                    if(!route.prepare()){
                        notifyState(route.status());
                        nextRouteAttempt=SystemClock.elapsedRealtime()+2500;
                        Thread.sleep(300);
                        continue;
                    }
                    mic=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        16000,AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,Math.max(min,6400));
                    if(mic.getState()!=AudioRecord.STATE_INITIALIZED
                        ||!route.configure(mic)){
                        if(mic!=null)mic.release();
                        mic=null;
                        notifyState(route.status());
                        nextRouteAttempt=SystemClock.elapsedRealtime()+8000;
                        continue;
                    }
                    recorder=mic;
                    mic.startRecording();
                    live=mic.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING;
                    boolean verified=false;
                    // HFP/SCO may take a moment to negotiate. Verify before
                    // processing any audio; never silently fall back to phone mic.
                    for(int i=0;i<9 && live && !closed && hasOutput();i++){
                        if(route.verify(mic)){verified=true;break;}
                        Thread.sleep(140);
                    }
                    if(!verified){
                        notifyState(route.status());
                        mic.stop();mic.release();mic=null;recorder=null;live=false;
                        nextRouteAttempt=SystemClock.elapsedRealtime()+16000;
                        continue;
                    }
                    notifyState("Kulaklık mikrofonu etkin · Pandora için hazır");
                    reset=true;
                    lastRouteCheck=SystemClock.elapsedRealtime();
                }
                if(!live||mic.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING){
                    if(mic!=null){mic.release();mic=null;recorder=null;}
                    live=false;
                    nextRouteAttempt=SystemClock.elapsedRealtime()+8000;
                    continue;
                }

                int count=mic.read(frame,0,frame.length);
                if(count<=0){
                    if(closed||!hasOutput())continue;
                    notifyState("Kulaklık mikrofonu okuma hatası");
                    mic.stop();mic.release();mic=null;recorder=null;
                    live=false;nextRouteAttempt=SystemClock.elapsedRealtime()+16000;
                    continue;
                }
                if(!hasOutput())continue;
                if(SystemClock.elapsedRealtime()-lastRouteCheck>4000){
                    lastRouteCheck=SystemClock.elapsedRealtime();
                    if(!route.verify(mic)){
                        notifyState(route.status());
                        mic.stop();mic.release();mic=null;recorder=null;
                        live=false;nextRouteAttempt=SystemClock.elapsedRealtime()+16000;
                        continue;
                    }
                }

                // Emergency STOP is the ONLY speech allowed while muted,
                // including TTS. It never executes a model or device action.
                stopVoicedMs+=voicedDuration(frame,count);
                boolean stopComplete=stopWord.acceptWaveForm(frame,count);
                if(stopComplete){
                    JSONObject finalStop=parsed(stopWord.getResult());
                    boolean stopVerified=StopPhraseGate.verified(
                        transcription(finalStop),confidence(finalStop),stopVoicedMs);
                    stopVoicedMs=0;
                    if(stopVerified){
                        muted=true;reset=true;
                        main.post(()->{if(!closed)listener.emergencyStop();});
                        continue;
                    }
                }
                if(stopVoicedMs>4500) {
                    stopWord.reset();stopVoicedMs=0;
                }
                if(muted){
                    // Normal wake/commands remain ignored while busy.
                    continue;
                }
                if(reset){
                    reset=false;activated=false;wakeVoicedMs=0;commandVoicedMs=0;
                    wake.reset();command.reset();
                }
                long voiced=voicedDuration(frame,count);
                if(!activated){
                    wakeVoicedMs+=voiced;
                    // Both decoders hear the SAME pre-wake utterance, allowing
                    // "Pandora, bunu yap" as one spoken sentence.
                    boolean cmdDone=command.acceptWaveForm(frame,count);
                    boolean complete=wake.acceptWaveForm(frame,count);
                    if(!complete)continue;  // NEVER act on Vosk partial guesses.
                    JSONObject done=parsed(wake.getResult());
                    String text=transcription(done);
                    boolean verified=WakePhraseGate.verified(
                        text,confidence(done),wakeVoicedMs);
                    JSONObject speech=parsed(cmdDone
                        ? command.getResult():command.getFinalResult());
                    String full=transcription(speech);
                    double fullConfidence=confidence(speech);
                    command.reset();
                    wake.reset();wakeVoicedMs=0;
                    if(verified){
                        String suffix=WakeWordMatcher.matches(full)
                            ? full.replaceFirst("(?iu)^(?:hey\\s+)?pandora\\s*[,!?;:]?\\s*","").trim()
                            : "";
                        main.post(()->{if(!closed && !muted)listener.wake();});
                        if(suffix.length()>1 && (fullConfidence<0 || fullConfidence>=0.40)){
                            // The entire command arrived after the wake word in
                            // the first utterance: no repeated wake word required.
                            muted=true;
                            activated=false;
                            final String commandText="pandora "+suffix;
                            notifyState("Komut alındı · yanıt hazırlanıyor");
                            main.post(()->{if(!closed)listener.result(commandText);});
                        }else{
                            activated=true;
                            commandVoicedMs=0;
                            wakeDeadline=SystemClock.elapsedRealtime()+13000L;
                            notifyState("Pandora algılandı · komutunu dinliyorum");
                        }
                    }
                }else{
                    commandVoicedMs+=voiced;
                    boolean complete=command.acceptWaveForm(frame,count);
                    boolean timedOut=SystemClock.elapsedRealtime()>=wakeDeadline;
                    if(!complete && !timedOut)continue;
                    JSONObject result=parsed(complete?command.getResult():
                        command.getFinalResult());
                    String phrase=transcription(result);
                    double conf=confidence(result);
                    boolean sensible=phrase.length()>1
                        && commandVoicedMs>=270
                        && (conf<0 || conf>=.38);
                    if(sensible){
                        muted=true;
                        activated=false;
                        final String commandText="hey pandora "+phrase;
                        notifyState("Komut alındı · yanıt hazırlanıyor");
                        main.post(()->{if(!closed)listener.result(commandText);});
                    }else if(timedOut){
                        activated=false;
                        command.reset();
                        wake.reset();
                        notifyState("Pandora için hazır");
                    }else{
                        command.reset();
                        commandVoicedMs=0;
                    }
                }
            }
        }catch(InterruptedException ignored){
            Thread.currentThread().interrupt();
        }catch(Exception ex){
            if(!closed)fail("Ses algılama hatası: "+ex.getClass().getSimpleName());
        }finally{
            recorder=null;
            if(mic!=null){
                try{mic.stop();}catch(RuntimeException ignored){}
                mic.release();
            }
            route.close();
            if(wake!=null)wake.close();
            if(command!=null)command.close();
            if(stopWord!=null)stopWord.close();
            if(wakeModel!=null){wakeModel.close();wakeModel=null;}
            if(turkishModel!=null){turkishModel.close();turkishModel=null;}
        }
    }
    /** Suppress recognition without reopening the headset mic (no repeated tones). */
    public void pause(){muted=true;}
    public void resume(){if(!closed){reset=true;muted=false;}}
    @Override public void close(){
        if(closed)return;
        muted=true;
        closed=true;
        AudioRecord current=recorder;
        if(current!=null && current.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING){
            try{current.stop();}catch(RuntimeException ignored){}
        }
        Thread thread=worker;
        if(thread!=null)thread.interrupt();
        else{
            // Resources may still be unpacking; callbacks check closed.
            if(wakeModel!=null){wakeModel.close();wakeModel=null;}
            if(turkishModel!=null){turkishModel.close();turkishModel=null;}
        }
    }
}
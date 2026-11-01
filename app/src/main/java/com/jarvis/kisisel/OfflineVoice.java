package com.jarvis.kisisel;
import android.content.Context;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import org.json.JSONObject;
/** Local audio processing only. No Android SpeechRecognizer restart chimes or audio upload. */
public final class OfflineVoice implements AutoCloseable {
    public interface Listener {void partial(String text);void result(String text);void error();}
    private Model model;
    private Recognizer recognizer;
    private SpeechService speech;
    private boolean closed,paused;
    public OfflineVoice(Context context,Listener listener){
        StorageService.unpack(context,"model-tr","pandora-model-tr",loaded->{
            if(closed){loaded.close();return;}
            model=loaded;
            try{
                recognizer=new Recognizer(model,16000f);
                speech=new SpeechService(recognizer,16000f);
                speech.startListening(new org.vosk.android.RecognitionListener(){
                    public void onPartialResult(String json){if(!closed&&!paused)listener.partial(value(json,"partial"));}
                    public void onResult(String json){if(!closed&&!paused)listener.result(value(json,"text"));}
                    public void onFinalResult(String json){}
                    public void onError(Exception e){if(!closed)listener.error();}
                    public void onTimeout(){if(!closed)listener.error();}
                });
                speech.setPause(paused);
            }catch(Exception e){listener.error();}
        },error->{if(!closed)listener.error();});
    }
    private static String value(String json,String key){try{return new JSONObject(json).optString(key,"");}catch(Exception e){return "";}}
    public void pause(){paused=true;if(speech!=null)speech.setPause(true);}
    public void resume(){if(closed)return;paused=false;if(speech!=null){speech.reset();speech.setPause(false);}}
    public void close(){closed=true;if(speech!=null){speech.cancel();speech.shutdown();speech=null;}if(recognizer!=null){recognizer.close();recognizer=null;}if(model!=null){model.close();model=null;}}
}

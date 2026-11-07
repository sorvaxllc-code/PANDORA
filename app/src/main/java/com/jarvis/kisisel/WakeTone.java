package com.jarvis.kisisel;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

/**
 * Original, quiet one-shot wake confirmation, never the Android STT/notification beep.
 * Played ONLY after the wake detector verifies an utterance and only into a
 * connected headset output. If routing cannot be requested, fail silently.
 */
public final class WakeTone {
    private WakeTone(){}
    public static void play(Context context){
        if(!PandoraAlerts.headsetConnected(context) || PandoraAlerts.inCall(context))return;
        AudioManager manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if(manager==null)return;
        AudioDeviceInfo selected=null;
        int priority=-1;
        for(AudioDeviceInfo dev:manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
            int type=dev.getType(),p=-1;
            if(type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO)p=5;
            else if(type==AudioDeviceInfo.TYPE_BLE_HEADSET)p=5;
            else if(type==AudioDeviceInfo.TYPE_WIRED_HEADSET)p=4;
            else if(type==AudioDeviceInfo.TYPE_USB_HEADSET)p=4;
            else if(type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)p=3;
            else if(type==AudioDeviceInfo.TYPE_WIRED_HEADPHONES)p=2;
            if(p>priority){selected=dev;priority=p;}
        }
        if(selected==null || priority<2)return;
        try{
            final int rate=24000;
            final int frames=rate/5;
            short[] pcm=new short[frames];
            double phase=0;
            for(int i=0;i<frames;i++){
                double t=(double)i/frames;
                double envelope=Math.sin(Math.PI*t);
                envelope*=envelope;
                double hz=620+260*t;
                phase+=2*Math.PI*hz/rate;
                // Soft 200-ms rising glass sound, not a ringing notification.
                double v=.11*envelope*(Math.sin(phase)+.16*Math.sin(2*phase));
                pcm[i]=(short)(v*32767);
            }
            AudioTrack track=new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(frames*2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();
            if(track.getState()!=AudioTrack.STATE_INITIALIZED ||
                !track.setPreferredDevice(selected)){
                track.release();return;
            }
            track.setVolume(.38f);
            if(track.write(pcm,0,pcm.length)!=pcm.length){track.release();return;}
            track.play();
            new Handler(Looper.getMainLooper()).postDelayed(()->{
                try{track.stop();}catch(RuntimeException ignored){}
                track.release();
            },400);
        }catch(RuntimeException ignored){
            // No audio can be safer than unexpectedly playing from the speaker.
        }
    }
}
package com.jarvis.kisisel;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Build;

/**
 * Captures only from a real headset input, never the phone microphone by accident.
 * Bluetooth A2DP is OUTPUT ONLY: to record, the headset must expose HFP/SCO/BLE
 * input. On Android 12+ selectCommunicationDevice opens its microphone profile.
 *
 * Device selection is performed once per live device, not on each speech segment.
 * A platform/earbud beep when HFP first connects cannot be suppressed here.
 */
public final class HeadsetAudioRoute implements AutoCloseable {
    private final Context app;
    private final AudioManager audio;
    private AudioDeviceInfo input;
    private int requestedId=-1;
    private boolean ownsSelection=false;
    private volatile String status="Hazırlanıyor";

    public HeadsetAudioRoute(Context context){
        app=context.getApplicationContext();
        audio=(AudioManager)app.getSystemService(Context.AUDIO_SERVICE);
    }
    public String status(){return status;}
    public static boolean micInputType(int type){
        return type==AudioDeviceInfo.TYPE_WIRED_HEADSET
            ||type==AudioDeviceInfo.TYPE_USB_HEADSET
            ||type==AudioDeviceInfo.TYPE_USB_DEVICE
            ||type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            ||(Build.VERSION.SDK_INT>=31 && type==AudioDeviceInfo.TYPE_BLE_HEADSET);
    }
    private boolean bluetoothOutput(){
        if(audio==null)return false;
        for(AudioDeviceInfo dev:audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
            int t=dev.getType();
            if(t==AudioDeviceInfo.TYPE_BLUETOOTH_SCO || t==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
               ||(Build.VERSION.SDK_INT>=31&&t==AudioDeviceInfo.TYPE_BLE_HEADSET))
                return true;
        }
        return false;
    }
    private AudioDeviceInfo findHeadsetInput(){
        if(audio==null)return null;
        AudioDeviceInfo chosen=null;
        for(AudioDeviceInfo dev:audio.getDevices(AudioManager.GET_DEVICES_INPUTS)){
            if(!dev.isSource() || !micInputType(dev.getType()))continue;
            // Prefer Bluetooth microphone when wireless output is attached.
            if(bluetoothOutput()){
                if(dev.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    ||(Build.VERSION.SDK_INT>=31&&dev.getType()==AudioDeviceInfo.TYPE_BLE_HEADSET))
                    return dev;
            }else if(dev.getType()==AudioDeviceInfo.TYPE_WIRED_HEADSET
                ||dev.getType()==AudioDeviceInfo.TYPE_USB_HEADSET
                ||dev.getType()==AudioDeviceInfo.TYPE_USB_DEVICE){
                chosen=dev;
                break;
            }
        }
        return chosen;
    }
    /** Returns false until a microphone-capable headset profile is available. */
    public boolean prepare(){
        if(audio==null){status="Ses sistemi bulunamadı";return false;}
        if(!PandoraAlerts.headsetConnected(app)){
            releaseSelection();
            input=null;status="Kulaklık bekleniyor";
            return false;
        }
        if(PandoraAlerts.inCall(app)){
            input=null;status="Telefon görüşmesi: dinleme duraklatıldı";
            return false;
        }
        boolean bt=bluetoothOutput();
        if(bt && Build.VERSION.SDK_INT>=31){
            if(app.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                !=PackageManager.PERMISSION_GRANTED){
                status="Bluetooth izni gerekiyor";
                return false;
            }
            // Do not re-request a communication route every 100ms.
            if(!ownsSelection){
                for(AudioDeviceInfo device:audio.getAvailableCommunicationDevices()){
                    if(device.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                        ||device.getType()==AudioDeviceInfo.TYPE_BLE_HEADSET){
                        try{
                            if(audio.setCommunicationDevice(device)){
                                requestedId=device.getId();
                                ownsSelection=true;
                            }
                        }catch(SecurityException ignored){
                            status="Bluetooth ses izni gerekiyor";
                            return false;
                        }
                        break;
                    }
                }
            }
        }
        input=findHeadsetInput();
        if(input==null){
            status=bt?"Bluetooth kulaklık mikrofonu bağlanamadı":
                "Bağlı kulaklıkta mikrofon yok";
            return false;
        }
        status="Kulaklık mikrofonu bulundu";
        return true;
    }
    /** Must be called before AudioRecord.startRecording(). */
    public boolean configure(AudioRecord rec){
        if(input==null||!micInputType(input.getType())){
            status="Kulaklık mikrofonu seçilemedi";
            return false;
        }
        boolean ok=rec.setPreferredDevice(input);
        status=ok?"Kulaklık mikrofonuna yönlendiriliyor":"Mikrofon yönlendirmesi reddedildi";
        return ok;
    }
    public boolean verify(AudioRecord rec){
        AudioDeviceInfo routed=rec.getRoutedDevice();
        boolean ok=routed!=null && routed.isSource() && micInputType(routed.getType());
        status=ok?"Kulaklık mikrofonundan dinliyor":
            "Telefon mikrofonuna yönlendirildi; gizlilik için durduruldu";
        return ok;
    }
    public void releaseSelection(){
        if(ownsSelection && Build.VERSION.SDK_INT>=31 && audio!=null){
            try{audio.clearCommunicationDevice();}catch(RuntimeException ignored){}
        }
        requestedId=-1;
        ownsSelection=false;
    }
    @Override public void close(){releaseSelection();}
}
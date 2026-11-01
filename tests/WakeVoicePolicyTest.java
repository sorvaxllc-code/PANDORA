import com.jarvis.kisisel.WakeVoicePolicy;

public class WakeVoicePolicyTest {
    public static void main(String[] args) {
        if (!WakeVoicePolicy.allowDirectSpeech(true,true,false))
            throw new AssertionError("Headphones should receive direct spoken replies");
        if (WakeVoicePolicy.allowDirectSpeech(true,false,false))
            throw new AssertionError("Without headphones, notification only");
        if (WakeVoicePolicy.allowDirectSpeech(false,true,false))
            throw new AssertionError("Respect user's voice-off setting");
        if (WakeVoicePolicy.allowDirectSpeech(true,true,true))
            throw new AssertionError("Never interrupt calls");
        System.out.println("WakeVoicePolicy passed");
    }
}

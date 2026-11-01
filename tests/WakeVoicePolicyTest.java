import com.jarvis.kisisel.WakeVoicePolicy;

public class WakeVoicePolicyTest {
    public static void main(String[] args) {
        if (!WakeVoicePolicy.allowDirectSpeech(true,false))
            throw new AssertionError("Direct voice commands must speak on phone speaker as well as headphones");
        if (WakeVoicePolicy.allowDirectSpeech(false,false))
            throw new AssertionError("Respect user's voice-off setting");
        if (WakeVoicePolicy.allowDirectSpeech(true,true))
            throw new AssertionError("Never interrupt calls");
        System.out.println("WakeVoicePolicy passed");
    }
}

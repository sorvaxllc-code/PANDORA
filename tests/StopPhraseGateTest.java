import com.jarvis.kisisel.safety.StopPhraseGate;
public final class StopPhraseGateTest {
    static void ok(boolean x){if(!x)throw new AssertionError("StopPhraseGateTest");}
    public static void main(String[] a){
        ok(StopPhraseGate.verified("dur",.92,480));
        ok(StopPhraseGate.verified("Pandora, dur",.90,1100));
        ok(!StopPhraseGate.verified("durdurmayı düşünüyorum",.99,900));
        ok(!StopPhraseGate.verified("dur",.5,900));
        ok(!StopPhraseGate.verified("dur",.95,100));
        ok(!StopPhraseGate.verified("dur",.95,4000));
        ok(!StopPhraseGate.verified("yardım",.99,900));
        System.out.println("StopPhraseGateTest PASS");
    }
}

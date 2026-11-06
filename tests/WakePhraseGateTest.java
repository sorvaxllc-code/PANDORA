import com.jarvis.kisisel.WakePhraseGate;
public final class WakePhraseGateTest {
    static void require(boolean ok){if(!ok)throw new AssertionError("WakePhraseGate failed");}
    public static void main(String[] args){
        require(WakePhraseGate.verified("hey pandora",.88,850));
        require(WakePhraseGate.verified("hi pandora",.73,830));
        require(WakePhraseGate.verified("hey pandora",-1,850));
        require(!WakePhraseGate.verified("hey pandora",.38,900));
        require(!WakePhraseGate.verified("hey pandora",.85,80));
        require(!WakePhraseGate.verified("hey pandora",-1,360));
        require(!WakePhraseGate.verified("hey pandora",.80,14000));
        require(!WakePhraseGate.verified("weird sound",.99,1800));
        require(!WakePhraseGate.verified("",.99,900));
        require(!WakePhraseGate.verified("hello pandora",.99,900));
        System.out.println("WakePhraseGateTest: OK");
    }
}
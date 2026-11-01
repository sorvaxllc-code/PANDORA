import com.jarvis.kisisel.ScreenSourcePolicy;
public class ScreenSourcePolicyTest {
    public static void main(String[] args) {
        if(!ScreenSourcePolicy.canRead("com.twitter.android",false))
            throw new AssertionError("Visible X content should be allowed after command");
        if(ScreenSourcePolicy.canRead("com.twitter.android",true))
            throw new AssertionError("Never OCR password input screens");
        if(ScreenSourcePolicy.canRead("com.android.systemui",false))
            throw new AssertionError("Never read Quick Settings or notification shade");
        if(ScreenSourcePolicy.canRead("com.jarvis.kisisel.stable",false))
            throw new AssertionError("Never read our own app as X");
        if(ScreenSourcePolicy.canRead("",false))
            throw new AssertionError("Unknown foreground app");
        System.out.println("ScreenSourcePolicy passed");
    }
}

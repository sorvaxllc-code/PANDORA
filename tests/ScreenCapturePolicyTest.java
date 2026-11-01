import com.jarvis.kisisel.ScreenCapturePolicy;

public class ScreenCapturePolicyTest {
    private static void expect(boolean actual,boolean expected,String what) {
        if(actual!=expected)throw new AssertionError(what);
    }
    public static void main(String[] args) {
        expect(ScreenCapturePolicy.canUse("com.twitter.android","com.twitter.android",false),true,"normal foreground screen");
        expect(ScreenCapturePolicy.canUse("com.twitter.android","com.whatsapp",false),false,"changed app");
        expect(ScreenCapturePolicy.canUse("com.twitter.android","com.twitter.android",true),false,"password form");
        expect(ScreenCapturePolicy.canUse("com.twitter.android","com.android.systemui",false),false,"notification shade");
        expect(ScreenCapturePolicy.canUse("com.jarvis.kisisel.stable","com.jarvis.kisisel.stable",false),false,"own app");
        expect(ScreenCapturePolicy.canUse(null,"com.twitter.android",false),false,"missing expected");
        expect(ScreenCapturePolicy.canUse("com.twitter.android",null,false),false,"missing actual");
        expect(ScreenCapturePolicy.canUse("com.twitter.android","com.twitter.android.fake",false),false,"prefix spoof");
        System.out.println("ScreenCapturePolicy passed");
    }
}

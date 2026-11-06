import com.jarvis.kisisel.ScreenCloudPolicy;
public class ScreenCloudPolicyTest {
    private static void check(boolean condition) { if(!condition)throw new AssertionError("Screen cloud policy"); }
    public static void main(String[] args) {
        check(ScreenCloudPolicy.allowed("LOCAL",false));
        check(!ScreenCloudPolicy.allowed("OFF",true));
        check(!ScreenCloudPolicy.allowed("CLOUD",false));
        check(!ScreenCloudPolicy.allowed("AUTO",false));
        check(ScreenCloudPolicy.allowed("CLOUD",true));
        check(ScreenCloudPolicy.allowed("AUTO",true));
        check(!ScreenCloudPolicy.allowed("GROQ",false));
        check(!ScreenCloudPolicy.allowed("HYBRID",false));
        check(!ScreenCloudPolicy.allowed("HERMES",false));
        check(ScreenCloudPolicy.allowed("GROQ",true));
        check(ScreenCloudPolicy.allowed("HYBRID",true));
        check(ScreenCloudPolicy.allowed("HERMES",true));
        System.out.println("ScreenCloudPolicy passed");
    }
}

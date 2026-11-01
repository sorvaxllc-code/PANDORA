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
        System.out.println("ScreenCloudPolicy passed");
    }
}

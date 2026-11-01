import com.jarvis.kisisel.PandoraTilePolicy;
public class PandoraTilePolicyTest {
    public static void main(String[] args) {
        if(!PandoraTilePolicy.canAnalyzeFromTile(true))
            throw new AssertionError("Accessibility should permit quick tile without microphone");
        if(PandoraTilePolicy.canAnalyzeFromTile(false))
            throw new AssertionError("Accessibility must be explicitly enabled");
        System.out.println("PandoraTilePolicy passed");
    }
}

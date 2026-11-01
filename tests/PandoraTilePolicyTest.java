import com.jarvis.kisisel.PandoraTilePolicy;
public class PandoraTilePolicyTest {
    public static void main(String[] args) {
        if(!PandoraTilePolicy.canAnalyzeFromTile(true,true))throw new AssertionError("Both prerequisites should permit analysis");
        if(PandoraTilePolicy.canAnalyzeFromTile(true,false))throw new AssertionError("No screen permission");
        if(PandoraTilePolicy.canAnalyzeFromTile(false,true))throw new AssertionError("No active foreground service");
        if(PandoraTilePolicy.canAnalyzeFromTile(false,false))throw new AssertionError("Neither ready");
        System.out.println("PandoraTilePolicy passed");
    }
}

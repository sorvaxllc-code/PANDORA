import com.jarvis.kisisel.safety.RiskPolicy;
import static com.jarvis.kisisel.safety.RiskPolicy.*;
public final class RiskPolicyTest {
 static void ok(boolean x){if(!x)throw new AssertionError("RiskPolicyTest");}
 public static void main(String[]a){
   for(Action action:Action.values()){
      if(action!=Action.UNKNOWN&&action!=Action.CHANGE_SECURITY){
        ok(RiskPolicy.check(action,true,true)==Decision.BLOCK);
      }
   }
   ok(RiskPolicy.check(Action.READ_SCREEN,false,false)==Decision.ALLOW);
   ok(RiskPolicy.check(Action.SEND_MESSAGE,false,false)==Decision.REQUIRE_EXPLICIT_APPROVAL);
   ok(RiskPolicy.check(Action.PURCHASE,false,false)==Decision.REQUIRE_EXPLICIT_APPROVAL);
   ok(RiskPolicy.check(Action.TRANSFER_FUNDS,false,false)==Decision.REQUIRE_EXPLICIT_APPROVAL);
   ok(RiskPolicy.check(Action.DELETE,false,true)==Decision.ALLOW);
   ok(RiskPolicy.check(Action.CHANGE_SECURITY,false,true)==Decision.BLOCK);
   ok(RiskPolicy.check(Action.UNKNOWN,false,true)==Decision.BLOCK);
   System.out.println("RiskPolicyTest PASS");
 }
}

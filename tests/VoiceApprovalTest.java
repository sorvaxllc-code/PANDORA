import com.jarvis.kisisel.safety.VoiceApproval;
public final class VoiceApprovalTest {
 static void ok(boolean x){if(!x)throw new AssertionError("VoiceApprovalTest");}
 public static void main(String[] args){
  VoiceApproval a=new VoiceApproval();
  a.request("call:+905551112233",500);
  ok(!a.consume("call:+905559998877","onaylıyorum",1000));
  ok(!a.consume("call:+905551112233","evet",1000));
  ok(a.consume("call:+905551112233","onaylıyorum",1000));
  ok(!a.consume("call:+905551112233","onaylıyorum",1001));
  a.request("delete:doc1",1000);
  ok(!a.consume("delete:doc1","onaylıyorum",31001));
  a.clear(); ok(!a.pending());
  System.out.println("VoiceApprovalTest PASS");
 }
}

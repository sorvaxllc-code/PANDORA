import com.jarvis.kisisel.ai.HybridRoutePolicy;
public final class HybridRoutePolicyTest {
  static void ok(boolean v){if(!v)throw new AssertionError("HybridRoutePolicy failed");}
  public static void main(String[] args){
    ok(HybridRoutePolicy.provider("HYBRID",true,true).equals("GROQ"));
    ok(HybridRoutePolicy.provider("HYBRID",false,true).equals("HERMES"));
    ok(HybridRoutePolicy.provider("HYBRID",false,false).equals("NEED_GROQ"));
    ok(HybridRoutePolicy.provider("GROQ",false,true).equals("NEED_GROQ"));
    ok(HybridRoutePolicy.provider("HERMES",true,true).equals("HERMES"));
    ok(HybridRoutePolicy.isExplicitHermesRequest("Hermes, şu dosyayı kontrol et"));
    ok(HybridRoutePolicy.isExplicitHermesRequest("hermes: şuna bak"));
    ok(!HybridRoutePolicy.isExplicitHermesRequest("Hermes hakkında bilgi ver"));
    ok(!HybridRoutePolicy.isExplicitHermesRequest("İşlemi Hermes'e devreder miyiz"));
    ok(HybridRoutePolicy.stripHermesPrefix("Hermes, dosyayı kontrol et").equals("dosyayı kontrol et"));
    System.out.println("HybridRoutePolicyTest OK");
  }
}
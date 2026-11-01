import com.jarvis.kisisel.ai.FreeCloudPolicy;
public final class FreeCloudPolicyTest {
  public static void main(String[] args){
    require(FreeCloudPolicy.isFreeOnly("openrouter/free"),"free allowed");
    require(!FreeCloudPolicy.isFreeOnly("openrouter/auto"),"auto not allowed");
    require(!FreeCloudPolicy.isFreeOnly("openai/gpt-4o"),"paid disallowed");
    require(FreeCloudPolicy.COMPLETIONS_URL.startsWith("https://"),"encrypted endpoint");
    require(FreeCloudPolicy.httpMessage(429,true).contains("kota"),"clear quota errors");
    require(FreeCloudPolicy.httpMessage(401,true).contains("anahtarı"),"clear auth errors");
    require(FreeCloudPolicy.FALLBACK_LOCAL_READ_TIMEOUT_MS<FreeCloudPolicy.LOCAL_READ_TIMEOUT_MS,"fallback bounded");
    System.out.println("FreeCloudPolicy: 7 tests passed");
  }
  private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
}

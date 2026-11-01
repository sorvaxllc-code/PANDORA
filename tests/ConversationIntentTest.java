import com.jarvis.kisisel.ConversationIntent;
public final class ConversationIntentTest {
  private static int n=0;
  static void test(String input,boolean expected){
    boolean actual=ConversationIntent.isOnlyGreeting(input);
    if(actual!=expected)throw new AssertionError("Unexpected greeting classification: "+input+" expected "+expected+" actual "+actual);
    n++;
  }
  public static void main(String[] args){
    test("Merhaba",true);
    test("Merhaba!",true);
    test("SELAM",true);
    test("Merhaba Pandora.",true);
    test("Hey Pandora, merhaba!",true);
    test("Günaydın",true);
    test("İyi akşamlar Pandora",true);
    test("Merhaba Pandora. Sen benim kişisel yapay zekâ asistanımsın. Bana Türkçe cevap ver. Bir yapay zekâ ajanı ile normal bir sohbet botu arasındaki farkı üç cümlede açıkla.",false);
    test("Merhaba, bana bir konuyu araştır",false);
    test("Selam nasılsın",false);
    test("Pandora, merhaba, notlarımı göster",false);
    test("Bir yapay zekâ ajanı ile sohbet botu arasındaki farkı açıkla",false);
    test("",false);
    test(null,false);
    System.out.println("ConversationIntent: "+n+" tests passed");
  }
}

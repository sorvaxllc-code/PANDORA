import com.jarvis.kisisel.WakeWordMatcher;
public class WakeWordMatcherTest {
  private static void expect(boolean actual, boolean desired, String message) {
    if (actual != desired) throw new AssertionError(message);
  }
  public static void main(String[] args) {
    expect(WakeWordMatcher.matches("Hey Pandora, beni duyuyor musun?"),true,"hey phrase");
    expect(WakeWordMatcher.matches("Pandora!"),true,"pandora");
    expect(WakeWordMatcher.matches("  HEY   PANDORA   "),true,"whitespace");
    expect(WakeWordMatcher.matches("Pandoranın kitabı"),false,"prefix guard");
    expect(WakeWordMatcher.matches("Merhaba Pandora"),false,"non-leading");
    expect(WakeWordMatcher.matches(""),false,"empty");
    expect(WakeWordMatcher.matches(null),false,"null");
    System.out.println("WakeWordMatcher tests passed");
  }
}
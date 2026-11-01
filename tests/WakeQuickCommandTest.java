import com.jarvis.kisisel.WakeQuickCommand;
public class WakeQuickCommandTest {
    private static void check(String query,WakeQuickCommand.Kind expected) {
        WakeQuickCommand.Result result=WakeQuickCommand.parse(query);
        if(result==null||result.kind!=expected)throw new AssertionError(query+" -> "+expected);
    }
    public static void main(String[] args) {
        check("Hey Pandora, saat kaç?",WakeQuickCommand.Kind.TIME);
        check("PANDORA! şu an saat kaç",WakeQuickCommand.Kind.TIME);
        check("Pandora, bugün ayın kaçı?",WakeQuickCommand.Kind.DATE);
        check("Hey Pandora, bugün hangi gün?",WakeQuickCommand.Kind.DATE);
        check("Hey Pandora, şarjım kaç?",WakeQuickCommand.Kind.BATTERY);
        check("Hey Pandora",WakeQuickCommand.Kind.WAKE_ONLY);
        check("Hey Pandora şu ekrana bak",WakeQuickCommand.Kind.SCREEN_ANALYZE);
        check("Hey Pandora bu haber ne demek",WakeQuickCommand.Kind.SCREEN_ANALYZE);
        check("Hey Pandora görüntülü görüşmeye geç",WakeQuickCommand.Kind.VIDEO_UPGRADE);
        check("Hey Pandora, Binance'de pozisyon aç",WakeQuickCommand.Kind.UNKNOWN);
        check("Pandora, WhatsApp görüşmesini kapat",WakeQuickCommand.Kind.UNKNOWN);
        if(WakeQuickCommand.parse("Merhaba Pandora")!=null)throw new AssertionError("Needs prefix");
        if(WakeQuickCommand.parse(null)!=null)throw new AssertionError("Null input");
        System.out.println("WakeQuickCommand passed");
    }
}
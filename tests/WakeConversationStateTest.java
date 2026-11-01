import com.jarvis.kisisel.WakeConversationState;
import com.jarvis.kisisel.WakeQuickCommand;

public class WakeConversationStateTest {
    private static void check(WakeQuickCommand.Kind actual,WakeQuickCommand.Kind expected) {
        if(actual!=expected)throw new AssertionError("Expected "+expected+" got "+actual);
    }
    public static void main(String[] args) {
        WakeConversationState s=new WakeConversationState();
        check(s.accept("saat kaç",1000),null);
        check(s.accept("Hey Pandora",2000),WakeQuickCommand.Kind.WAKE_ONLY);
        check(s.accept("saat kaç",3000),WakeQuickCommand.Kind.TIME);
        check(s.accept("saat kaç",4000),null);
        check(s.accept("Hey Pandora bu haber ne demek",6000),WakeQuickCommand.Kind.SCREEN_ANALYZE);
        check(s.accept("görüntülü görüşmeye geç",7000),null);
        check(s.accept("Hey Pandora",10000),WakeQuickCommand.Kind.WAKE_ONLY);
        check(s.accept("WhatsApp bana merhaba dedi",12000),WakeQuickCommand.Kind.UNKNOWN);
        check(s.accept("görüntülü görüşmeye geç",13000),null);
        check(s.accept("Hey Pandora",13500),WakeQuickCommand.Kind.WAKE_ONLY);
        check(s.accept("görüntülü görüşmeye geç",14000),WakeQuickCommand.Kind.VIDEO_UPGRADE);
        check(s.accept("Hey Pandora",30000),WakeQuickCommand.Kind.WAKE_ONLY);
        check(s.accept("saat kaç",47000),null);
        System.out.println("WakeConversationState passed");
    }
}

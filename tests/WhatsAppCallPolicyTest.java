import com.jarvis.kisisel.WhatsAppCallPolicy;

public class WhatsAppCallPolicyTest {
    static void yes(boolean b) { if(!b)throw new AssertionError("Expected true"); }
    static void no(boolean b) { if(b)throw new AssertionError("Expected false"); }
    public static void main(String[] args) {
        yes(WhatsAppCallPolicy.supportedPackage("com.whatsapp"));
        yes(WhatsAppCallPolicy.supportedPackage("com.whatsapp.w4b"));
        no(WhatsAppCallPolicy.supportedPackage("com.whatsapp.fake"));
        yes(WhatsAppCallPolicy.isEndCallControl("End call"));
        yes(WhatsAppCallPolicy.isEndCallControl("Aramayı bitir"));
        yes(WhatsAppCallPolicy.isAudioControl("Mute"));
        yes(WhatsAppCallPolicy.isAudioControl("Hoparlör"));
        no(WhatsAppCallPolicy.mayUpgrade("com.whatsapp",false,true,false));
        no(WhatsAppCallPolicy.mayUpgrade("com.whatsapp",true,false,false));
        no(WhatsAppCallPolicy.mayUpgrade("com.whatsapp",true,true,true));
        no(WhatsAppCallPolicy.mayUpgrade("com.telegram.messenger",true,true,false));
        yes(WhatsAppCallPolicy.mayUpgrade("com.whatsapp",true,true,false));
        no(WhatsAppCallPolicy.isUpgradeButton("The video call was yesterday",true));
        no(WhatsAppCallPolicy.isUpgradeButton("Video call",false));
        yes(WhatsAppCallPolicy.isUpgradeButton("Video call",true));
        yes(WhatsAppCallPolicy.isUpgradeButton("Görüntülü görüşmeye geç",true));
        yes(WhatsAppCallPolicy.isUpgradeButton("Switch to video",true));
        yes(WhatsAppCallPolicy.indicatesActiveVideo("Turn off video"));
        yes(WhatsAppCallPolicy.indicatesActiveVideo("Sesli aramaya geç"));
        no(WhatsAppCallPolicy.isUpgradeButton("Turn off video",true));
        System.out.println("WhatsAppCallPolicy passed");
    }
}

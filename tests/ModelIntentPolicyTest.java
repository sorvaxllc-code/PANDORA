import com.jarvis.kisisel.ai.ModelIntentPolicy;
public class ModelIntentPolicyTest {
    public static void main(String[] args) {
        String actions=ModelIntentPolicy.systemInstructions(true);
        String info=ModelIntentPolicy.systemInstructions(false);
        if(!actions.contains("PANDORA_ACTION_V1"))throw new AssertionError("Action mode must describe action protocol");
        if(info.contains("PANDORA_ACTION_V1"))throw new AssertionError("Informational mode must not advertise action protocol");
        if(!info.contains("YALNIZCA BİLGİ VERME"))throw new AssertionError("Informational mode should constrain model");
        if(!info.contains("Ekran metni"))throw new AssertionError("Screen injection must be treated as data");
        System.out.println("ModelIntentPolicy passed");
    }
}

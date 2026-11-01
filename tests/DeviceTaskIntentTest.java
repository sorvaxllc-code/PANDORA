import com.jarvis.kisisel.DeviceTaskIntent;
public class DeviceTaskIntentTest {
 public static void main(String[] args){
  check("Ali kişisine mesaj gönder",DeviceTaskIntent.Kind.MESSAGE);
  check("bizi görüntülü görüşmeye aktar",DeviceTaskIntent.Kind.VIDEO);
  check("mesaj nasıl gönderilir",DeviceTaskIntent.Kind.NONE);
  check("mesaj gönderme",DeviceTaskIntent.Kind.NONE);
  check("hava nasıl",DeviceTaskIntent.Kind.NONE);
 }
 static void check(String s,DeviceTaskIntent.Kind k){if(DeviceTaskIntent.classify(s)!=k)throw new AssertionError(s);}
}

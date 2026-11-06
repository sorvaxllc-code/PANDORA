import com.jarvis.kisisel.core.CaptainPlanner;
public class CaptainPlannerTest {
 static void ok(boolean x){if(!x)throw new AssertionError("CaptainPlanner test failed");}
 public static void main(String[] a){
   ok(CaptainPlanner.forGoal("Shopify ürün satışı").lead.equals("E-Ticaret"));
   ok(CaptainPlanner.forGoal("Android uygulama hata").lead.equals("Yazılım"));
   ok(CaptainPlanner.forGoal("Kaldıraç risk bütçe").lead.equals("Finans & Risk"));
   ok(CaptainPlanner.forGoal("Video tasarım üret").lead.equals("Yaratıcı"));
   ok(CaptainPlanner.forGoal("Günlük proje takvim").lead.equals("Operasyon"));
   ok(CaptainPlanner.forGoal("Bana bir düşünce deneyini yorumla").lead.equals("Strateji"));
   String s=CaptainPlanner.captainInstructions(CaptainPlanner.forGoal("Android uygulama"),"Bir","İki");
   ok(s.contains("Bir")&&s.contains("İki")&&s.contains("onayı"));
   ok(CaptainPlanner.specialistInstructions("Araştırma","Soru").contains("canlı"));
   ok(CaptainPlanner.forGoal("A".repeat(3500)).goal.length()==1800);
   System.out.println("CaptainPlannerTest: OK");
 }
}
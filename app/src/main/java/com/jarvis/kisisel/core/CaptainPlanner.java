package com.jarvis.kisisel.core;

import java.util.Locale;

/**
 * Local, predictable dispatch. Each selected expert gets its OWN AI call.
 * Model outputs are written back as evidence for the captain synthesis stage.
 * Offline dispatch never asserts an external site, app, or financial account was visited.
 */
public final class CaptainPlanner {
    public static final class Plan {
        public final String lead, reviewer;
        public final String goal;
        Plan(String goal,String lead,String reviewer){
            this.goal=goal;this.lead=lead;this.reviewer=reviewer;
        }
    }
    private CaptainPlanner(){}
    public static Plan forGoal(String raw){
        String goal=raw==null?"":raw.trim();
        if(goal.length()>1800)goal=goal.substring(0,1800);
        String s=goal.toLowerCase(new Locale("tr","TR"));
        if(containsAny(s,"grafik","candlestick","rsi","macd","mum formasyonu",
            "destek direnç","btc","usdt","binance","coin","teknik analiz"))
            return new Plan(goal,"Grafik & Kripto","Finans & Risk");
        if(containsAny(s,"haber","gündem","son dakika","x gönder","twitter","telegram",
            "akış","takip et","izle","bildir"))
            return new Plan(goal,"Haber & Kaynak","Doğrulama");
        if(containsAny(s,"fiyat karşılaştır","fiyat analiz","kar marjı","maliyet hesap",
            "rekabetçi fiyat","komisyon hesabı"))
            return new Plan(goal,"Fiyat Analizi","E-Ticaret");
        if(containsAny(s,"shopify","ürün","pazar","seo","müşteri","satış","reklam","ticaret",
            "dropshipping","tedarik","amazon","marka","mağaza"))
            return new Plan(goal,"E-Ticaret","Pazar Araştırması");
        if(containsAny(s,"kod","yazılım","uygulama","android","github","hata","api",
            "sistem","program","otomasyon","bot"))
            return new Plan(goal,"Yazılım","Kalite & Güvenlik");
        if(containsAny(s,"kripto","yatırım","bütçe","finans","fiyat","banka","vadeli",
            "risk","kaldıraç","strateji","borsa","gelir"))
            return new Plan(goal,"Finans & Risk","Eleştirel Analiz");
        if(containsAny(s,"tasarım","görsel","logo","video","metin","yazı","yaratıcı","içerik"))
            return new Plan(goal,"Yaratıcı","Pazarlama");
        if(containsAny(s,"plan","günlük","zaman","iş akışı","proje","ekip","takvim",
            "görev","organizasyon"))
            return new Plan(goal,"Operasyon","Strateji");
        return new Plan(goal,"Strateji","Araştırma");
    }
    private static boolean containsAny(String s,String... terms){
        for(String term:terms)if(s.contains(term))return true;
        return false;
    }
    public static String specialistInstructions(String role,String goal){
        String common="Sen PANDORA'nın "+role+" uzmanısın. Kullanıcı isteği: "+goal
            +"\nBir ekip arkadaşı olarak uygulanabilir somut katkı hazırla."
            +" Bilmediğin gerçek zamanlı bilgiye sahip olduğunu iddia etme."
            +" İnterneti, hesabı, cihazı veya dosyaları gerçekten açıp incelemediysen açtığını söyleme."
            +" İzinsiz işlem, ödeme, emir, mesaj, arama veya hesap değişikliği yapma."
            +" Türkçe yaz; kısa, açık ve teknik olarak doğru ol. Asla sahte kaynak oluşturma.";
        switch(role){
            case "Grafik & Kripto":return common+"\nGerçek mum verisi veya gösterge yoksa formasyon ve al-sat sinyali uydurma. Grafik, hacim, funding, stop ve likidasyon risklerini koşullu senaryolarla açıkla.";
            case "Haber & Kaynak":return common+"\nX/Telegram hesapları veya RSS sağlayıcısı bağlı değilse haberleri anlık gördüğünü iddia etme. Kaynak URL, tarih ve doğrulama ihtiyacını açıkla.";
            case "Doğrulama":return common+"\nİddiaların kanıt kalitesini, manipülasyon ve sahte hesap riskini açıkla; anlık haber kaynağı yoksa belirt.";
            case "Fiyat Analizi":return common+"\nMarj=net gelir-maliyet; kargo/komisyon/KDV/kur varsayımlarını açık göster. Gerçek fiyat verisi yoksa fiyat uydurma.";
            case "E-Ticaret":return common+"\nÜrün, rekabet, maliyet, pazarlama ve somut ilk 3 aksiyona odaklan. Fiyat/komisyon doğrulanmadıysa tahmin de.";
            case "Pazar Araştırması":return common+"\nDoğrulanması gereken varsayımları ve gerçek araştırma adımlarını önceliklendir; canlı web erişimin yoksa açıkça belirt.";
            case "Yazılım":return common+"\nMimari, minimum çalışan çözüm, test ve hata durumları; kodu çalıştırmadıysan test edildi demeden sun.";
            case "Kalite & Güvenlik":return common+"\nYazılım için güvenlik tehditleri, izin sınırları, kritik testler, geri alma planı oluştur.";
            case "Finans & Risk":return common+"\nBütçe, işlem maliyeti, kayıp riski ve senaryo hesaplarını belirt; kesin getiri veya yatırım emri verme.";
            case "Eleştirel Analiz":return common+"\nVarsayımları çürütmeye çalış, risk ve belirsizliği numaralarla ayır.";
            case "Yaratıcı":return common+"\nİşle ilgili 3 özgün yaratıcı seçenek ve üretilebilir taslak öner.";
            case "Pazarlama":return common+"\nHedef kitle, mesaj, organik dağıtım ve ölçülebilir test öner.";
            case "Operasyon":return common+"\nİşleri bağımlılık sırasına göre böl; öncelik, sorumlu, kabul kriteri öner.";
            case "Araştırma":return common+"\nSoruyu analiz et, kanıt gereken noktaları ve belirsizlikleri işaretle; çevrimiçi araştırma yaptığını varsayma.";
            default:return common+"\nAmaç, kısıt, alternatifler ve en verimli ilk adımları açıkla.";
        }
    }
    public static String captainInstructions(Plan p,String first,String second){
        return "Sen PANDORA kaptanısın. Kullanıcının işi için iki farklı uzmanı gerçekten dinledin."
           +" Aynı model çağrısı farklı rollerle yapılmış olabilir; bağımsız insan veya ayrı dış veri kaynağı varmış gibi konuşma."
           +" İlgili katkıları birleştir, varsa çelişki ve riskleri belirt. Türkçe ve somut yanıt ver."
           +" Çıktı: 1) Karar/özet 2) Kimin ne yaptığı 3) Sıralı 3 görev 4) Kullanıcı onayı veya gerçek entegrasyon gereken işler."
           +" 'Görevler yapıldı' deme; yalnızca gerçekten tamamlanan analizleri tamamlandı göster."
           +" Hiçbir araç, web sitesi, telefon veya gerçek hesap üzerinde işlem yaptığını iddia etme."
           +"\nKULLANICI GÖREVİ:\n"+p.goal
           +"\n"+p.lead+" UZMANININ GERÇEK YANITI:\n"+first
           +"\n"+p.reviewer+" UZMANININ GERÇEK YANITI:\n"+second;
    }
}
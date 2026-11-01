package com.jarvis.kisisel.ai;
/** Single immutable configuration for free-only cloud calls. */
public final class FreeCloudPolicy {
    private FreeCloudPolicy() {}
    public static final String COMPLETIONS_URL="https://openrouter.ai/api/v1/chat/completions";
    public static final String KEY_CHECK_URL="https://openrouter.ai/api/v1/key";
    public static final String MODEL="openrouter/free";
    public static final int CLOUD_READ_TIMEOUT_MS=45000;
    public static final int LOCAL_READ_TIMEOUT_MS=45000;
    public static final int FALLBACK_LOCAL_READ_TIMEOUT_MS=12000;
    public static boolean isFreeOnly(String model) { return MODEL.equals(model); }
    public static String httpMessage(int code, boolean cloud) {
        if (cloud) {
            if(code==401 || code==403) return "OpenRouter anahtarı geçersiz veya erişime kapalı (HTTP "+code+"). Ayarlardan yeniden kaydet.";
            if(code==402) return "Servis bu isteği kabul etmedi (HTTP 402). Ücretli modele geçilmedi.";
            if(code==429) return "OpenRouter ücretsiz istek kotası veya hız sınırı doldu (HTTP 429). Daha sonra deneyebilirsin.";
            if(code==400) return "Ücretsiz bulut modeli isteği kabul etmedi (HTTP 400).";
            if(code==404) return "Ücretsiz bulut modeli şu anda bulunamadı (HTTP 404).";
            if(code>=500) return "Bulut sağlayıcısında geçici hata (HTTP "+code+").";
            return "Bulut isteği başarısız (HTTP "+code+").";
        }
        if(code==503) return "Termux yerel modeli henüz hazır değil (HTTP 503).";
        return "Yerel model hata verdi (HTTP "+code+").";
    }
}

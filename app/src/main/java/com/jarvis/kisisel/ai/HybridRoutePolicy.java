package com.jarvis.kisisel.ai;

/** Deterministic model selection; no hidden paid fallback and no arbitrary device actions. */
public final class HybridRoutePolicy {
    private HybridRoutePolicy(){}
    public static String provider(String mode,boolean groqKey,boolean hermesKey){
        if("HERMES".equals(mode))return hermesKey?"HERMES":"NEED_HERMES";
        if("GROQ".equals(mode))return groqKey?"GROQ":"NEED_GROQ";
        if("HYBRID".equals(mode)){
            if(groqKey)return "GROQ";
            if(hermesKey)return "HERMES"; // preserve previous PANDORA until Groq setup
            return "NEED_GROQ";
        }
        return mode;
    }
    public static boolean isExplicitHermesRequest(String text){
        if(text==null)return false;
        String q=text.trim().toLowerCase(java.util.Locale.ROOT);
        return q.startsWith("hermes,")||q.startsWith("hermes:")
            ||q.startsWith("hermes ")||q.equals("hermes");
    }
    public static String stripHermesPrefix(String text){
        if(!isExplicitHermesRequest(text))return text;
        return text.trim().replaceFirst("(?iu)^hermes[,:]?\\\\s*","").trim();
    }
}
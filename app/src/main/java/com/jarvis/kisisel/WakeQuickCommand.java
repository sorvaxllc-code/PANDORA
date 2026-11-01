package com.jarvis.kisisel;
import java.util.Locale;
/** Whitelisted, read-only hands-free commands. No account or device modification. */
public final class WakeQuickCommand {
    public enum Kind { WAKE_ONLY, TIME, DATE, BATTERY, UNKNOWN }
    public static final class Result {
        public final Kind kind;
        public Result(Kind kind) {this.kind=kind;}
    }
    private WakeQuickCommand(){}
    public static Result parse(String phrase){
        if(!WakeWordMatcher.matches(phrase))return null;
        String q=phrase.toLowerCase(new Locale("tr","TR")).replaceAll("[.,!?;:]+"," ")
            .replaceAll("\\s+"," ").trim().replaceFirst("^(?:hey\\s+)?pandora(?:\\s+|$)","").trim();
        if(q.isEmpty())return new Result(Kind.WAKE_ONLY);
        if(q.equals("saat kaç")||q.equals("şu an saat kaç")||q.equals("şimdi saat kaç")||
           q.equals("saat kaç oldu")||q.equals("saati söyle"))return new Result(Kind.TIME);
        if(q.equals("bugün ayın kaçı")||q.equals("bugün hangi gün")||
           q.equals("bugün günlerden ne")||q.equals("bugünün tarihi ne")||
           q.equals("tarih ne")||q.equals("tarihi söyle"))return new Result(Kind.DATE);
        if(q.equals("şarjım kaç")||q.equals("şarj yüzde kaç")||q.equals("pil yüzde kaç")||
           q.equals("pilim yüzde kaç")||q.equals("batarya yüzde kaç"))return new Result(Kind.BATTERY);
        return new Result(Kind.UNKNOWN);
    }
}
package com.jarvis.kisisel.safety;

/** One explicit spoken confirmation for one exact user-requested operation.
 * Expiration uses Android elapsedRealtime supplied by the caller. */
public final class VoiceApproval {
    private String id;
    private long expiry;
    private boolean active;
    public synchronized void request(String actionId,long nowMs){
        if(actionId==null||actionId.isEmpty())throw new IllegalArgumentException("actionId");
        id=actionId;expiry=nowMs+30000;active=true;
    }
    public synchronized boolean consume(String actionId,String utterance,long nowMs){
        if(!active || nowMs>expiry || !id.equals(actionId)
            || utterance==null)return false;
        String phrase=utterance.toLowerCase(new java.util.Locale("tr","TR")).trim()
            .replaceAll("[.,!?]+$","");
        if(!phrase.equals("onaylıyorum") && !phrase.equals("aramayı onayla")
            && !phrase.equals("evet onaylıyorum"))return false;
        clear();
        return true;
    }
    public synchronized void clear(){active=false;id=null;expiry=0;}
    public synchronized boolean pending(){return active;}
}

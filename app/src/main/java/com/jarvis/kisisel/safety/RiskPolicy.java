package com.jarvis.kisisel.safety;

/** Deny-by-default policy for future generic actions. Input is a typed tool
 * action, NEVER a natural language model assertion. */
public final class RiskPolicy {
    public enum Action {
        READ_SCREEN, OPEN_APP, NAVIGATE, PLAY_MEDIA,
        CALL_CONTACT, SEND_MESSAGE, DELETE, PURCHASE, TRANSFER_FUNDS,
        CHANGE_ACCOUNT, CHANGE_SECURITY, UNKNOWN
    }
    public enum Decision { ALLOW, REQUIRE_EXPLICIT_APPROVAL, BLOCK }
    private RiskPolicy(){}
    public static Decision check(Action action,boolean protectedScreen,boolean approval){
        if(protectedScreen || action==null)return Decision.BLOCK;
        switch(action){
            case READ_SCREEN: case OPEN_APP: case NAVIGATE: case PLAY_MEDIA:
                return Decision.ALLOW;
            case CALL_CONTACT: case SEND_MESSAGE: case DELETE:
            case PURCHASE: case TRANSFER_FUNDS: case CHANGE_ACCOUNT:
                return approval?Decision.ALLOW:Decision.REQUIRE_EXPLICIT_APPROVAL;
            case CHANGE_SECURITY: case UNKNOWN: default:
                return Decision.BLOCK;
        }
    }
}

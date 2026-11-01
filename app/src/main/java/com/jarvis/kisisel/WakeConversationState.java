package com.jarvis.kisisel;

/** Makes "Hey Pandora" followed by a second short command work, without ambient execution. */
public final class WakeConversationState {
    private long followUpUntilMs = 0L;
    private long lastActionMs = -100000L;

    public WakeQuickCommand.Kind accept(String phrase,long nowMs) {
        if(phrase==null || phrase.trim().isEmpty()) return null;
        boolean explicit = WakeWordMatcher.matches(phrase);
        if(!explicit && (followUpUntilMs==0L || nowMs>followUpUntilMs)) return null;
        WakeQuickCommand.Result result = WakeQuickCommand.parse(
            explicit ? phrase : ("hey pandora " + phrase));
        if(result==null) return null;
        WakeQuickCommand.Kind kind=result.kind;
        if(kind==WakeQuickCommand.Kind.WAKE_ONLY) {
            followUpUntilMs=nowMs+16000;
            return kind;
        }
        // The follow-up may be an open-ended question; it is routed only to the
        // informational AI model, never to a privileged device action.
        if(nowMs-lastActionMs<2000) return null;
        lastActionMs=nowMs;
        followUpUntilMs=0L;
        return kind;
    }

    public void reset() { followUpUntilMs=0L;lastActionMs=-100000L; }
}

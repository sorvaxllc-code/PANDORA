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
        if(kind==WakeQuickCommand.Kind.UNKNOWN && !explicit)
            return null; // A random nearby conversation is not a supported command.
        if(nowMs-lastActionMs<2000) return null;
        lastActionMs=nowMs;
        followUpUntilMs=0L;
        return kind;
    }

    public void reset() { followUpUntilMs=0L;lastActionMs=-100000L; }
}

package com.jarvis.kisisel.core;
import android.content.Context;
import java.util.function.Consumer;
/** Add a new skill by implementing this interface and registering it in AgentRegistry. */
public interface PandoraSkill {
  String name();
  String description();
  /** Return true only when the command was handled. */
  boolean handle(Context context, String normalized, Consumer<String> reply);
}

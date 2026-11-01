package com.jarvis.kisisel.core;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
public final class AgentRegistry {
  private final List<PandoraSkill> skills=new ArrayList<>();
  public AgentRegistry register(PandoraSkill skill){skills.add(skill);return this;}
  public boolean tryHandle(Context context, String normalized, Consumer<String> reply){
    for(PandoraSkill skill:skills) if(skill.handle(context,normalized,reply)) return true;
    return false;
  }
  public String describe(){
    StringBuilder b=new StringBuilder("Etkin modüller:\n");
    for(PandoraSkill s:skills)b.append("• ").append(s.name()).append(" — ").append(s.description()).append("\n");
    return b.toString();
  }
  public static AgentRegistry standard(){return new AgentRegistry()
      .register(new SystemSkill()).register(new AccountsSkill());}
}

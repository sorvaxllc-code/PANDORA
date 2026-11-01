package com.jarvis.kisisel.ai;
import org.json.JSONArray;
/** Provider abstraction. Add new provider without changing MainActivity. */
public interface AiProvider {
  String id();
  String answer(JSONArray messages) throws Exception;
}

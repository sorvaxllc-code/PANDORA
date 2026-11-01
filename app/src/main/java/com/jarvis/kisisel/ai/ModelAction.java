package com.jarvis.kisisel.ai;

import org.json.JSONObject;

/** A tiny, explicitly allowlisted command language. Model output is never executable code. */
public final class ModelAction {
    public final String type;
    public final String argument;
    private ModelAction(String type,String argument) {this.type=type;this.argument=argument;}

    /** Returns null for ordinary prose, unknown actions, or malformed replies. */
    public static ModelAction parse(String raw) {
        if(raw==null || raw.length()>2000) return null;
        String s=raw.trim();
        if(s.startsWith("```")) {
            int firstNewline=s.indexOf('\n');
            int lastFence=s.lastIndexOf("```");
            if(firstNewline<0 || lastFence<=firstNewline) return null;
            s=s.substring(firstNewline+1,lastFence).trim();
        }
        if(!s.startsWith("{") || !s.endsWith("}")) return null;
        try {
            JSONObject o=new JSONObject(s);
            if(!"PANDORA_ACTION_V1".equals(o.optString("format")))return null;
            String type=o.optString("action","").trim();
            if(!isAllowed(type))return null;
            String arg=o.optString("argument","").trim();
            if(arg.length()>180 || arg.contains("\n") || arg.contains("\r"))return null;
            if((type.equals("SEARCH_WEB") || type.equals("SEARCH_YOUTUBE") || type.equals("SAVE_NOTE")) && arg.isEmpty())return null;
            return new ModelAction(type,arg);
        } catch(Exception ex){return null;}
    }
    private static boolean isAllowed(String a){
        switch(a){
            case "SEARCH_WEB": case "SEARCH_YOUTUBE": case "OPEN_CAMERA":
            case "OPEN_SETTINGS": case "SHOW_NOTES": case "SHOW_REMINDERS":
            case "SAVE_NOTE": return true;
            default: return false;
        }
    }
}

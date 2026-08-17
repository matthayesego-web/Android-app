package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/** Device-local faction-scoped chat blocks. */
public final class BlockedUserStore {
    private static final String PREFS="tornfca_chat_blocks_v1";
    private BlockedUserStore(){}

    public static boolean isBlocked(Context c,int factionId,int playerId){
        if(c==null||factionId<=0||playerId<=0)return false;
        return load(c,factionId).optJSONObject(String.valueOf(playerId))!=null;
    }

    public static void block(Context c,int factionId,int playerId,String name){
        if(c==null||factionId<=0||playerId<=0)return;
        JSONObject root=load(c,factionId),entry=new JSONObject();
        try{entry.put("name",name==null?"Member":name);entry.put("blocked_at",System.currentTimeMillis());root.put(String.valueOf(playerId),entry);}catch(Exception ignored){}
        save(c,factionId,root);
    }

    public static int count(Context c,int factionId){return load(c,factionId).length();}

    public static JSONArray blockedUsers(Context c,int factionId){
        JSONArray out=new JSONArray();JSONObject root=load(c,factionId);java.util.Iterator<String> keys=root.keys();
        while(keys.hasNext()){String id=keys.next();JSONObject entry=root.optJSONObject(id);JSONObject row=new JSONObject();try{row.put("player_id",Integer.parseInt(id));row.put("name",entry==null?"Member":entry.optString("name","Member"));out.put(row);}catch(Exception ignored){}}
        return out;
    }

    public static void clearFaction(Context c,int factionId){if(c!=null)c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(key(factionId)).apply();}

    private static JSONObject load(Context c,int factionId){
        if(c==null||factionId<=0)return new JSONObject();String raw=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(key(factionId),"{}");
        try{return new JSONObject(raw);}catch(Exception e){return new JSONObject();}
    }
    private static void save(Context c,int factionId,JSONObject value){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(key(factionId),value.toString()).apply();}
    private static String key(int factionId){return"faction_"+factionId;}
}

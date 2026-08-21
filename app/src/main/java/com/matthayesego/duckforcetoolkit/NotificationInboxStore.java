package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/** Small device-local inbox so important TornFCA notifications remain visible after the shade clears. */
public final class NotificationInboxStore {
    private static final String PREFS="tornfca_notification_inbox_v1",KEY="items";
    private static final Object LOCK=new Object();
    private NotificationInboxStore(){}
    public static void add(Context c,String type,String title,String body,int factionId){synchronized(LOCK){try{SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);JSONArray old=new JSONArray(p.getString(KEY,"[]"));JSONArray next=new JSONArray();JSONObject row=new JSONObject();row.put("id",java.util.UUID.randomUUID().toString());row.put("type",type==null?"personal":type);row.put("title",title==null?"TornFCA":title);row.put("body",body==null?"":body);row.put("faction_id",factionId);row.put("created_at",System.currentTimeMillis());next.put(row);for(int i=0;i<old.length()&&next.length()<120;i++)next.put(old.opt(i));p.edit().putString(KEY,next.toString()).apply();}catch(Exception ignored){}}}
    public static JSONArray all(Context c){synchronized(LOCK){try{return new JSONArray(c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}}
    public static int count(Context c){return all(c).length();}
    public static void clear(Context c){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(KEY).apply();}
}

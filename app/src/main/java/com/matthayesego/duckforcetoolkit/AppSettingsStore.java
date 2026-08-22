package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

/** Player-controlled TornFCA preferences. No API keys or sensitive payloads are stored here. */
public final class AppSettingsStore {
    private static final String PREFS="tornfca_player_settings_v1";
    private AppSettingsStore(){}
    private static SharedPreferences p(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static boolean notificationsEnabled(Context c){return p(c).getBoolean("notifications_master",true);}
    public static void setNotificationsEnabled(Context c,boolean v){p(c).edit().putBoolean("notifications_master",v).apply();}
    public static boolean warAlerts(Context c){return p(c).getBoolean("notify_war",true);}
    public static void setWarAlerts(Context c,boolean v){p(c).edit().putBoolean("notify_war",v).apply();}
    public static boolean ocAlerts(Context c){return p(c).getBoolean("notify_oc",true);}
    public static void setOcAlerts(Context c,boolean v){p(c).edit().putBoolean("notify_oc",v).apply();}
    public static boolean chainAlerts(Context c){return p(c).getBoolean("notify_chain",true);}
    public static void setChainAlerts(Context c,boolean v){p(c).edit().putBoolean("notify_chain",v).apply();}
    public static boolean liveChainTracker(Context c){return p(c).getBoolean("live_chain_tracker",false);}
    public static void setLiveChainTracker(Context c,boolean v){p(c).edit().putBoolean("live_chain_tracker",v).apply();}
    public static boolean factionAlerts(Context c){return p(c).getBoolean("notify_faction",true);}
    public static void setFactionAlerts(Context c,boolean v){p(c).edit().putBoolean("notify_faction",v).apply();}
    public static boolean bankingAlerts(Context c){return p(c).getBoolean("notify_banking",true);}
    public static void setBankingAlerts(Context c,boolean v){p(c).edit().putBoolean("notify_banking",v).apply();}
    public static boolean chatAlerts(Context c){return p(c).getBoolean("notify_chat",true);}
    public static void setChatAlerts(Context c,boolean v){p(c).edit().putBoolean("notify_chat",v).apply();}
    public static boolean personalAlerts(Context c){return p(c).getBoolean("notify_personal",true);}
    public static void setPersonalAlerts(Context c,boolean v){p(c).edit().putBoolean("notify_personal",v).apply();}
    public static int warLeadMinutes(Context c){int v=p(c).getInt("war_lead_minutes",15);return v==30||v==60?v:15;}
    public static void setWarLeadMinutes(Context c,int v){p(c).edit().putInt("war_lead_minutes",v==30||v==60?v:15).apply();}
    public static boolean categoryEnabled(Context c,String type){if(!notificationsEnabled(c))return false;String t=type==null?"":type.toLowerCase(java.util.Locale.US);if("war".equals(t))return warAlerts(c);if("oc".equals(t))return ocAlerts(c);if("chain".equals(t))return chainAlerts(c);if("banking".equals(t))return bankingAlerts(c);if("faction".equals(t)||"announcement".equals(t)||"moderation".equals(t)||"report".equals(t))return factionAlerts(c);if("chat".equals(t))return chatAlerts(c);return personalAlerts(c);}
    public static String notificationPrefsJson(Context c){try{org.json.JSONObject o=new org.json.JSONObject();o.put("master",notificationsEnabled(c));o.put("war",warAlerts(c));o.put("oc",ocAlerts(c));o.put("chain",chainAlerts(c));o.put("live_chain_tracker",liveChainTracker(c));o.put("faction",factionAlerts(c));o.put("banking",bankingAlerts(c));o.put("chat",chatAlerts(c));o.put("personal",personalAlerts(c));o.put("war_lead_minutes",warLeadMinutes(c));return o.toString();}catch(Exception e){return"{}";}}
}

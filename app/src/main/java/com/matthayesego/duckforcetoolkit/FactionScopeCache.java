package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class FactionScopeCache {
    private static final String PREFS = "duckforce_scope_v060";
    private static final long TTL_MS = 5L * 60L * 1000L;

    private FactionScopeCache() {}

    public static final class Scope {
        public final int playerId;
        public final String playerName;
        public final int factionId;
        public final String factionName;
        public final String position;
        public final boolean factionApiAccess;

        Scope(int playerId,String playerName,int factionId,String factionName,String position,boolean factionApiAccess){
            this.playerId=playerId;this.playerName=playerName;this.factionId=factionId;this.factionName=factionName;this.position=position;this.factionApiAccess=factionApiAccess;
        }
    }

    public static Scope load(Context context,String apiKey){
        if(apiKey==null||apiKey.trim().isEmpty())return null;
        SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        long verifiedAt=p.getLong("verified_at",0L);
        if(verifiedAt<=0||System.currentTimeMillis()-verifiedAt>TTL_MS)return null;
        String expected=p.getString("key_fingerprint","");
        if(!fingerprint(apiKey).equals(expected))return null;
        int playerId=p.getInt("player_id",0),factionId=p.getInt("faction_id",0);
        if(playerId<=0||factionId<=0)return null;
        return new Scope(playerId,p.getString("player_name","Unknown"),factionId,p.getString("faction_name","Faction"),p.getString("position",""),p.getBoolean("faction_api",false));
    }

    public static void save(Context context,String apiKey,AuthSession session){
        if(apiKey==null||session==null)return;
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
                .putString("key_fingerprint",fingerprint(apiKey))
                .putLong("verified_at",System.currentTimeMillis())
                .putInt("player_id",session.playerId)
                .putString("player_name",session.playerName)
                .putInt("faction_id",session.factionId)
                .putString("faction_name",session.factionName)
                .putString("position",session.position)
                .putBoolean("faction_api",session.factionApiAccess)
                .apply();
    }

    public static long ageSeconds(Context context){
        long verifiedAt=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getLong("verified_at",0L);
        return verifiedAt<=0?-1L:Math.max(0L,(System.currentTimeMillis()-verifiedAt)/1000L);
    }

    public static boolean hasFreshScope(Context context){long age=ageSeconds(context);return age>=0&&age*1000L<TTL_MS;}

    public static void clear(Context context){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().apply();}

    private static String fingerprint(String value){
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] bytes=digest.digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format(java.util.Locale.US,"%02x",v&0xff));return b.toString();
        }catch(Exception e){return Integer.toHexString(value.hashCode());}
    }
}

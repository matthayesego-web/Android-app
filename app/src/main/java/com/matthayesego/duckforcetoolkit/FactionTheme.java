package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.graphics.Color;

/**
 * TornFCA theme boundary. The platform stays dark/neutral while each faction gets one stable,
 * restrained accent. A future shared backend can override the accent per faction without changing UI code.
 */
public final class FactionTheme {
    public final int accent;
    public final int accentSoft;
    public final int accentDark;
    public final int background;
    public final int surface;
    public final int border;
    public final String factionName;

    private static final int[] PREMIUM_ACCENTS = new int[]{
            Color.rgb(103,216,243), // arctic cyan
            Color.rgb(91,189,166),  // emerald teal
            Color.rgb(117,155,246), // royal blue
            Color.rgb(154,126,244), // violet
            Color.rgb(224,125,167), // rose
            Color.rgb(238,156,96),  // copper
            Color.rgb(225,190,92),  // champagne
            Color.rgb(105,203,154)  // jade
    };

    private FactionTheme(int accent,String factionName){
        this.accent=accent;
        this.accentSoft=blend(accent,Color.WHITE,.24f);
        this.accentDark=blend(accent,Color.BLACK,.68f);
        this.background=Color.rgb(5,9,14);
        this.surface=blend(Color.rgb(13,20,29),accent,.045f);
        this.border=blend(Color.rgb(43,56,72),accent,.12f);
        this.factionName=factionName==null||factionName.trim().isEmpty()?"Faction":factionName.trim();
    }

    public static FactionTheme forContext(Context context){
        String key=new SecureApiKeyStore(context).load();
        FactionScopeCache.Scope scope=FactionScopeCache.load(context,key);
        int factionId=scope==null?0:scope.factionId;
        String name=scope==null?"Faction":scope.factionName;
        int custom=context.getSharedPreferences("tornfca_theme_v1",Context.MODE_PRIVATE).getInt("accent_"+factionId,0);
        return new FactionTheme(custom!=0?custom:derivedAccent(factionId,name),name);
    }

    /** Reserved for the future faction-admin theme editor/shared backend. */
    public static void saveFactionAccent(Context context,int factionId,int color){
        if(factionId<=0)return;
        context.getSharedPreferences("tornfca_theme_v1",Context.MODE_PRIVATE).edit().putInt("accent_"+factionId,color).apply();
    }

    public static int derivedAccent(int factionId,String name){
        int h=17;
        h=31*h+factionId;
        if(name!=null)h=31*h+name.toLowerCase(java.util.Locale.ROOT).hashCode();
        int index=Math.floorMod(h,PREMIUM_ACCENTS.length);
        return PREMIUM_ACCENTS[index];
    }

    private static int blend(int from,int to,float amount){
        float a=Math.max(0f,Math.min(1f,amount));
        int r=Math.round(Color.red(from)*(1f-a)+Color.red(to)*a);
        int g=Math.round(Color.green(from)*(1f-a)+Color.green(to)*a);
        int b=Math.round(Color.blue(from)*(1f-a)+Color.blue(to)*a);
        return Color.rgb(r,g,b);
    }
}

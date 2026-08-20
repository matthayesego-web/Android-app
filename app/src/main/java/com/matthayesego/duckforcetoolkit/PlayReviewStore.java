package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Local-only state for the Google Play review sandbox.
 *
 * This store never represents a Torn identity and must never be used as backend authorization.
 */
public final class PlayReviewStore {
    public enum Persona { MEMBER, LEADER }

    private static final String PREFS="tornfca_play_review";
    private static final String KEY_ACTIVE="active";
    private static final String KEY_PERSONA="persona";
    private static final String KEY_ENTERED_AT="entered_at";

    private PlayReviewStore(){}

    public static boolean isActive(Context context){
        return prefs(context).getBoolean(KEY_ACTIVE,false);
    }

    public static Persona persona(Context context){
        String raw=prefs(context).getString(KEY_PERSONA,Persona.MEMBER.name());
        try{return Persona.valueOf(raw==null?Persona.MEMBER.name():raw);}catch(Exception ignored){return Persona.MEMBER;}
    }

    public static long enteredAt(Context context){return prefs(context).getLong(KEY_ENTERED_AT,0L);}

    public static void enter(Context context,Persona persona){
        prefs(context).edit()
                .putBoolean(KEY_ACTIVE,true)
                .putString(KEY_PERSONA,(persona==null?Persona.MEMBER:persona).name())
                .putLong(KEY_ENTERED_AT,System.currentTimeMillis())
                .apply();
    }

    public static void setPersona(Context context,Persona persona){
        if(!isActive(context))return;
        prefs(context).edit().putString(KEY_PERSONA,(persona==null?Persona.MEMBER:persona).name()).apply();
    }

    public static void clear(Context context){prefs(context).edit().clear().apply();}

    private static SharedPreferences prefs(Context context){
        return context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
}

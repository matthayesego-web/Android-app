package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypted device-local cache for a short-lived developer session token. */
public final class DeveloperSessionStore {
    private static final String PREFS="tornfca_developer_session_v1",ALIAS="tornfca_developer_session_key_v1";
    private static final String TOKEN="token",IV="iv",USERNAME="username",ROLE="role",EXPIRES="expires";
    private final Context app;private final SharedPreferences prefs;
    public DeveloperSessionStore(Context context){app=context.getApplicationContext();prefs=app.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}

    public synchronized void save(String token,String username,String role,long expiresAt)throws Exception{
        if(token==null||token.isBlank())throw new IllegalArgumentException("Developer session token missing.");
        SecretKey key=getOrCreateKey();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);byte[] enc=cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
        prefs.edit().putString(TOKEN,Base64.encodeToString(enc,Base64.NO_WRAP)).putString(IV,Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).putString(USERNAME,username==null?"":username).putString(ROLE,role==null?"developer":role).putLong(EXPIRES,expiresAt).apply();
    }
    public synchronized Session load(){
        long expiry=prefs.getLong(EXPIRES,0L);if(expiry<=System.currentTimeMillis()/1000L){clear();return null;}
        try{String enc=prefs.getString(TOKEN,null),iv=prefs.getString(IV,null);if(enc==null||iv==null){clear();return null;}KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);SecretKey key=(SecretKey)ks.getKey(ALIAS,null);if(key==null){clear();return null;}Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));String token=new String(cipher.doFinal(Base64.decode(enc,Base64.NO_WRAP)),StandardCharsets.UTF_8);return new Session(token,prefs.getString(USERNAME,"Developer"),prefs.getString(ROLE,"developer"),expiry);}catch(Exception e){clear();return null;}
    }
    public synchronized void clear(){prefs.edit().clear().apply();}
    public boolean isAdmin(){Session s=load();return s!=null&&("admin".equals(s.role)||"root".equals(s.role));}
    public boolean isRoot(){Session s=load();return s!=null&&"root".equals(s.role);}

    private SecretKey getOrCreateKey()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);java.security.Key existing=ks.getKey(ALIAS,null);if(existing instanceof SecretKey)return(SecretKey)existing;KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());return gen.generateKey();}

    public static final class Session{public final String token,username,role;public final long expiresAt;Session(String token,String username,String role,long expiresAt){this.token=token;this.username=username;this.role=role;this.expiresAt=expiresAt;}public boolean admin(){return"admin".equals(role)||"root".equals(role);}public boolean root(){return"root".equals(role);}}
}

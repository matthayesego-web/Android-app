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

/**
 * Device-local Torn API key storage.
 * A user may keep the key only for the current process or explicitly opt into encrypted 7/30/90-day retention.
 */
public final class SecureApiKeyStore {
    private static final String PREFS="duck_force_secure",ALIAS="duck_force_toolkit_api_key_v1",VALUE="api_key_ciphertext",IV="api_key_iv",EXPIRES_AT="api_key_expires_at",SAVED_DAYS="api_key_saved_days";
    private static volatile String sessionValue;private static volatile long sessionExpiresAt=Long.MAX_VALUE;private static volatile boolean sessionBackedByPersistence=false;private static volatile Boolean pendingPersist;private static volatile int pendingDays=30;
    private final SharedPreferences preferences;private final Context appContext;
    public SecureApiKeyStore(Context context){appContext=context.getApplicationContext();preferences=appContext.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static void prepareNextSave(boolean persist,int days){pendingPersist=persist;pendingDays=normalizeDays(days);}
    public synchronized void save(String value)throws Exception{if(value==null||value.trim().isEmpty())return;String clean=value.trim();Boolean decision=pendingPersist;pendingPersist=null;if(decision!=null){if(decision)persist(clean,pendingDays);else{clearPersistent();sessionValue=clean;sessionExpiresAt=Long.MAX_VALUE;sessionBackedByPersistence=false;}PushNotifications.syncIfReady(appContext);return;}if(sessionValue!=null&&sessionValue.equals(clean)){PushNotifications.syncIfReady(appContext);return;}String persisted=loadPersistent();if(persisted!=null&&persisted.equals(clean)){PushNotifications.syncIfReady(appContext);return;}sessionValue=clean;sessionExpiresAt=Long.MAX_VALUE;sessionBackedByPersistence=false;PushNotifications.syncIfReady(appContext);}
    public synchronized String load(){long now=System.currentTimeMillis();if(sessionValue!=null){if(sessionExpiresAt==Long.MAX_VALUE||sessionExpiresAt>now)return sessionValue;sessionValue=null;sessionExpiresAt=Long.MAX_VALUE;sessionBackedByPersistence=false;clearPersistent();}return loadPersistent();}
    public synchronized boolean isPersisted(){return preferences.contains(VALUE)&&preferences.getLong(EXPIRES_AT,0L)>System.currentTimeMillis();}
    public synchronized long persistedUntilMillis(){long expiry=preferences.getLong(EXPIRES_AT,0L);if(expiry<=System.currentTimeMillis()){if(expiry>0L)clearPersistent();return 0L;}return expiry;}
    public synchronized int savedDays(){return preferences.getInt(SAVED_DAYS,0);}
    public synchronized void clear(){sessionValue=null;sessionExpiresAt=Long.MAX_VALUE;sessionBackedByPersistence=false;pendingPersist=null;clearPersistent();}
    private void persist(String value,int days)throws Exception{SecretKey key=getOrCreateKey();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));long expires=System.currentTimeMillis()+days*24L*60L*60L*1000L;preferences.edit().putString(VALUE,Base64.encodeToString(encrypted,Base64.NO_WRAP)).putString(IV,Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).putLong(EXPIRES_AT,expires).putInt(SAVED_DAYS,days).apply();sessionValue=value;sessionExpiresAt=expires;sessionBackedByPersistence=true;}
    private String loadPersistent(){try{String ciphertext=preferences.getString(VALUE,null),iv=preferences.getString(IV,null);long expiry=preferences.getLong(EXPIRES_AT,0L);if(ciphertext==null||iv==null||expiry<=System.currentTimeMillis()){if(ciphertext!=null||iv!=null||expiry>0L)clearPersistent();return null;}KeyStore keyStore=KeyStore.getInstance("AndroidKeyStore");keyStore.load(null);SecretKey key=(SecretKey)keyStore.getKey(ALIAS,null);if(key==null){clearPersistent();return null;}Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));String value=new String(cipher.doFinal(Base64.decode(ciphertext,Base64.NO_WRAP)),StandardCharsets.UTF_8);sessionValue=value;sessionExpiresAt=expiry;sessionBackedByPersistence=true;return value;}catch(Exception e){clearPersistent();return null;}}
    private void clearPersistent(){preferences.edit().remove(VALUE).remove(IV).remove(EXPIRES_AT).remove(SAVED_DAYS).apply();if(sessionBackedByPersistence){sessionValue=null;sessionExpiresAt=Long.MAX_VALUE;sessionBackedByPersistence=false;}}
    private static int normalizeDays(int days){return days==7||days==30||days==90?days:30;}
    private SecretKey getOrCreateKey()throws Exception{KeyStore keyStore=KeyStore.getInstance("AndroidKeyStore");keyStore.load(null);java.security.Key existing=keyStore.getKey(ALIAS,null);if(existing instanceof SecretKey)return(SecretKey)existing;KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());return generator.generateKey();}
}

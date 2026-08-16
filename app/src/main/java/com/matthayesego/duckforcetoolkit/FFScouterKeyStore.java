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

/** Stores the user's registered FFScouter API key separately from the Torn key. */
public final class FFScouterKeyStore {
    private static final String PREFS="duck_force_ffscouter_secure";
    private static final String ALIAS="duck_force_ffscouter_key_v1";
    private static final String VALUE="ff_key_ciphertext";
    private static final String IV="ff_key_iv";
    private final SharedPreferences preferences;

    public FFScouterKeyStore(Context context){preferences=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}

    public void save(String value)throws Exception{
        SecretKey key=getOrCreateKey();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);
        byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        preferences.edit().putString(VALUE, Base64.encodeToString(encrypted,Base64.NO_WRAP)).putString(IV,Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).apply();
    }

    public String load(){try{String ciphertext=preferences.getString(VALUE,null),iv=preferences.getString(IV,null);if(ciphertext==null||iv==null)return null;KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);SecretKey key=(SecretKey)ks.getKey(ALIAS,null);if(key==null)return null;Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(ciphertext,Base64.NO_WRAP)),StandardCharsets.UTF_8);}catch(Exception e){return null;}}
    public void clear(){preferences.edit().remove(VALUE).remove(IV).apply();}

    private SecretKey getOrCreateKey()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);java.security.Key existing=ks.getKey(ALIAS,null);if(existing instanceof SecretKey)return(SecretKey)existing;KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());return generator.generateKey();}
}

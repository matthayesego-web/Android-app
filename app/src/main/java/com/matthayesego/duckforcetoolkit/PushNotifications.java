package com.matthayesego.duckforcetoolkit;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

/** Optional FCM client. It is inert until all Firebase client identifiers are configured. */
public final class PushNotifications {
    private static final String PREFS="tornfca_push_v1",TOKEN="token",LAST_SYNC="last_sync";
    private static volatile boolean initialized=false,syncing=false;
    private PushNotifications(){}

    public static boolean firebaseConfigured(){return present(BuildConfig.FIREBASE_APP_ID)&&present(BuildConfig.FIREBASE_API_KEY)&&present(BuildConfig.FIREBASE_PROJECT_ID)&&present(BuildConfig.FIREBASE_SENDER_ID);}
    public static boolean cloudConfigured(){return firebaseConfigured()&&CommunityBackendClient.isConfigured();}
    public static String status(Context c){if(!firebaseConfigured())return"Cloud push is not configured in this build.";if(!CommunityBackendClient.isConfigured())return"Firebase is ready; the TornFCA community backend is not connected.";String token=token(c);return token.isEmpty()?"Cloud push is configured; waiting for this device's FCM token.":"Cloud push is configured and this device has an FCM token.";}

    public static synchronized void initialize(Context context){
        if(initialized||context==null||!firebaseConfigured())return;
        Context app=context.getApplicationContext();
        try{
            try{FirebaseApp.getInstance();}
            catch(IllegalStateException missing){FirebaseOptions options=new FirebaseOptions.Builder().setApplicationId(BuildConfig.FIREBASE_APP_ID).setApiKey(BuildConfig.FIREBASE_API_KEY).setProjectId(BuildConfig.FIREBASE_PROJECT_ID).setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID).build();FirebaseApp.initializeApp(app,options);}
            FirebaseMessaging.getInstance().setAutoInitEnabled(true);
            initialized=true;
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(value->{if(value!=null&&!value.isBlank())onNewToken(app,value);});
        }catch(Exception ignored){initialized=false;}
    }

    public static void onNewToken(Context context,String value){if(context==null||value==null||value.isBlank())return;context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(TOKEN,value.trim()).apply();syncIfReady(context);}
    public static String token(Context context){return context==null?"":context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(TOKEN,"");}
    public static long lastSync(Context context){return context==null?0L:context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getLong(LAST_SYNC,0L);}

    public static void syncIfReady(Context context){
        if(context==null)return;initialize(context);if(!cloudConfigured()||syncing)return;
        Context app=context.getApplicationContext();String token=token(app);if(token.isBlank())return;String key=new SecureApiKeyStore(app).load();if(key==null||key.isBlank())return;
        syncing=true;new Thread(()->{try{CommunityBackendClient.registerPushToken(key,token,AppSettingsStore.notificationPrefsJson(app));app.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putLong(LAST_SYNC,System.currentTimeMillis()).apply();}catch(Exception ignored){}finally{syncing=false;}},"TornFCA-PushRegister").start();
    }

    public static void unregisterAsync(Context context){if(context==null||!CommunityBackendClient.isConfigured())return;Context app=context.getApplicationContext();String token=token(app),key=new SecureApiKeyStore(app).load();if(token.isBlank()||key==null||key.isBlank())return;new Thread(()->{try{CommunityBackendClient.unregisterPushToken(key,token);}catch(Exception ignored){}},"TornFCA-PushUnregister").start();}
    public static void requestCloudTest(Context context){if(context==null||!cloudConfigured())return;Context app=context.getApplicationContext();String key=new SecureApiKeyStore(app).load();if(key==null||key.isBlank())return;syncIfReady(app);new Thread(()->{try{CommunityBackendClient.sendPushTest(key);}catch(Exception ignored){}},"TornFCA-PushTest").start();}
    private static boolean present(String value){return value!=null&&!value.trim().isEmpty()&&!value.contains("###");}
}

package com.matthayesego.duckforcetoolkit;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/** Data-only FCM receiver so TornFCA applies its own categories, inbox and tenant checks. */
public class TornFcaMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token){PushNotifications.onNewToken(this,token);}
    @Override public void onMessageReceived(RemoteMessage message){
        if(message==null)return;Map<String,String> data=message.getData();String type=value(data,"type","personal"),title=value(data,"title","TornFCA"),body=value(data,"body","");int factionId=parseInt(value(data,"faction_id","0"));int current=currentFactionId();
        // A faction-scoped push is displayed only while this installation is currently authenticated
        // to that exact faction. This also drops stale delivery after logout or a faction change.
        if(factionId>0&&current!=factionId)return;
        if((title.isBlank()||body.isBlank())&&message.getNotification()!=null){if(title.isBlank())title=message.getNotification().getTitle();if(body.isBlank())body=message.getNotification().getBody();}
        NotificationCenter.receive(this,type,title==null?"TornFCA":title,body==null?"":body,factionId);
    }
    @Override public void onDeletedMessages(){int factionId=currentFactionId();if(factionId<=0)return;NotificationInboxStore.add(this,"personal","Some cloud messages expired","Open TornFCA to refresh current faction status.",factionId);}
    private String value(Map<String,String> data,String key,String fallback){String v=data==null?null:data.get(key);return v==null?fallback:v;}
    private int parseInt(String raw){try{return Integer.parseInt(raw);}catch(Exception e){return 0;}}
    private int currentFactionId(){String key=new SecureApiKeyStore(this).load();if(key==null)return 0;AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null)return hot.factionId;FactionScopeCache.Scope s=FactionScopeCache.load(this,key);return s==null?0:s.factionId;}
}

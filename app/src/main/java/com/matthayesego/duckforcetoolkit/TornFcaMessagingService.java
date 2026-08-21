package com.matthayesego.duckforcetoolkit;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/** Data-only FCM receiver so TornFCA applies its own categories, inbox, tenant checks and chat blocks. */
public class TornFcaMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token){PushNotifications.onNewToken(this,token);}
    @Override public void onMessageReceived(RemoteMessage message){
        if(message==null)return;Map<String,String> data=message.getData();String type=value(data,"type","personal"),title=value(data,"title","TornFCA"),body=value(data,"body","");int factionId=parseInt(value(data,"faction_id","0")),targetPlayerId=parseInt(value(data,"target_player_id","0"));int currentFaction=currentFactionId(),currentPlayer=currentPlayerId();
        // A faction-scoped push is displayed only while this installation is currently authenticated
        // to that exact faction. Player-targeted messages additionally require the exact player ID.
        if(factionId>0&&currentFaction!=factionId)return;
        if(targetPlayerId>0&&currentPlayer!=targetPlayerId)return;
        if("chat".equalsIgnoreCase(type)){
            int authorId=parseInt(value(data,"author_id","0"));
            if(authorId>0&&BlockedUserStore.isBlocked(this,factionId,authorId))return;
        }
        if((title.isBlank()||body.isBlank())&&message.getNotification()!=null){if(title.isBlank())title=message.getNotification().getTitle();if(body.isBlank())body=message.getNotification().getBody();}
        NotificationCenter.receive(this,type,title==null?"TornFCA":title,body==null?"":body,factionId);
    }
    @Override public void onDeletedMessages(){int factionId=currentFactionId();if(factionId<=0)return;NotificationInboxStore.add(this,"personal","Some cloud messages expired","Open TornFCA to refresh current faction status.",factionId);}
    private String value(Map<String,String> data,String key,String fallback){String v=data==null?null:data.get(key);return v==null?fallback:v;}
    private int parseInt(String raw){try{return Integer.parseInt(raw);}catch(Exception e){return 0;}}
    private int currentFactionId(){String key=new SecureApiKeyStore(this).load();if(key==null)return 0;AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null)return hot.factionId;FactionScopeCache.Scope s=FactionScopeCache.load(this,key);return s==null?0:s.factionId;}
    private int currentPlayerId(){String key=new SecureApiKeyStore(this).load();if(key==null)return 0;AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null)return hot.playerId;FactionScopeCache.Scope s=FactionScopeCache.load(this,key);return s==null?0:s.playerId;}
}

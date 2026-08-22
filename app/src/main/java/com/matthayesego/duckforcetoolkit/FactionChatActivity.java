package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** TornFCA app-native faction chat with instant local sends, fast polling and strict faction scoping. */
public class FactionChatActivity extends Activity {
    private static final long POLL_MS=5000L;
    private static final long CHAT_CACHE_MS=10L*60L*1000L;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Map<String,JSONObject> pendingById=new LinkedHashMap<>();
    private boolean active=false,loading=false,refreshing=false;
    private AuthSession session;
    private Spinner channelSpinner;
    private EditText messageField;
    private TextView freshnessView;
    private String lastRenderedSignature="";
    private String currentChannel="general";
    private String draftText="";
    private int identityRefreshFactionId=0;
    private final Runnable poll=this::pollOnce;

    private void pollOnce(){if(active){load(false);handler.postDelayed(poll,POLL_MS);}}
    @Override protected void onCreate(Bundle b){super.onCreate(b);renderLoading("Opening your faction community…");bootstrap();}
    @Override protected void onResume(){super.onResume();active=true;handler.removeCallbacks(poll);handler.postDelayed(poll,POLL_MS);PushNotifications.syncIfReady(this);}
    @Override protected void onPause(){active=false;handler.removeCallbacks(poll);super.onPause();}

    private void bootstrap(){
        if(!CommunityBackendClient.isConfigured()){renderOffline();return;}
        String key=new SecureApiKeyStore(this).load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key first.");return;}
        new Thread(()->{try{
            session=TornApiClient.cachedSession(key);if(session==null)session=TornApiClient.authenticate(key);
            JSONArray cached=StartupWarmCache.chat(session.factionId,"general",CHAT_CACHE_MS);
            runOnUiThread(()->{if(cached!=null)render(cached,"general",false);load(cached==null);});
        }catch(Exception e){renderError(e.getMessage()==null?"Unable to verify faction membership.":e.getMessage());}},"TornFCA-ChatBootstrap").start();
    }

    private void load(boolean full){
        if(loading||session==null)return;
        String key=new SecureApiKeyStore(this).load();if(key==null||key.isBlank())return;
        loading=true;refreshing=true;String channel=selectedChannel();runOnUiThread(this::updateFreshness);
        new Thread(()->{try{
            CommunityBackendClient.ChatSnapshot snapshot=CommunityBackendClient.getChatSnapshot(key,channel);
            JSONArray messages=snapshot.messages;String signature=messages.toString();
            runOnUiThread(()->{
                boolean factionChanged=applySnapshotIdentity(snapshot,key);
                String resolvedChannel=factionChanged?"general":channel;
                if(factionChanged&& !"general".equals(channel)){
                    refreshing=false;loading=false;currentChannel="general";load(true);return;
                }
                StartupWarmCache.putChat(session.factionId,resolvedChannel,messages);
                refreshing=false;
                if(full||factionChanged||!signature.equals(lastRenderedSignature))render(messages,resolvedChannel,true);else updateFreshness();
            });
        }catch(Exception e){
            String message=e.getMessage()==null?"Unable to load faction chat.":e.getMessage();
            runOnUiThread(()->{
                refreshing=false;updateFreshness();
                if("leadership".equals(channel)&&message.toLowerCase(java.util.Locale.US).contains("leadership")){
                    currentChannel="general";loading=false;load(true);return;
                }
                if(full&&lastRenderedSignature.isEmpty())renderError(message);
            });
        }finally{loading=false;}},"TornFCA-ChatLoad").start();
    }

    private boolean applySnapshotIdentity(CommunityBackendClient.ChatSnapshot snapshot,String key){
        if(snapshot==null||snapshot.playerId<=0||snapshot.factionId<=0)return false;
        AuthSession old=session;boolean sameFaction=old!=null&&old.playerId==snapshot.playerId&&old.factionId==snapshot.factionId;
        boolean changed=!sameFaction||old==null||!safe(old.position).equals(safe(snapshot.position))||!safe(old.factionName).equals(safe(snapshot.factionName));
        if(!changed)return false;
        session=new AuthSession(snapshot.playerId,safeName(snapshot.playerName,"Member"),snapshot.factionId,safeName(snapshot.factionName,"Faction"),safe(snapshot.position),sameFaction&&old.factionApiAccess,sameFaction?old.tier:AccessTier.GREEN,sameFaction?old.positions:new JSONArray(),sameFaction?old.abilities:new JSONArray(),sameFaction&&old.permissionsResolved);
        if(!sameFaction){
            pendingById.clear();lastRenderedSignature="";currentChannel="general";
            refreshFullFactionIdentity(key,snapshot.factionId);
            Toast.makeText(this,"Faction changed — chat moved to "+session.factionName+".",Toast.LENGTH_SHORT).show();
        }
        return !sameFaction;
    }

    private void refreshFullFactionIdentity(String key,int factionId){
        if(identityRefreshFactionId==factionId)return;identityRefreshFactionId=factionId;
        new Thread(()->{try{
            AuthSession fresh=TornApiClient.authenticateFreshFaction(key);
            FactionScopeCache.save(this,key,fresh);StartupWarmCache.putSession(fresh);
            runOnUiThread(()->{identityRefreshFactionId=0;if(session!=null&&session.playerId==fresh.playerId&&session.factionId==fresh.factionId){session=fresh;PushNotifications.syncIfReady(this);JSONArray cached=StartupWarmCache.chat(fresh.factionId,currentChannel,CHAT_CACHE_MS);if(cached!=null)render(cached,currentChannel,false);}});
        }catch(Exception ignored){runOnUiThread(()->identityRefreshFactionId=0);}},"TornFCA-ChatFactionRefresh").start();
    }

    private String selectedChannel(){if(channelSpinner==null||channelSpinner.getSelectedItem()==null)return currentChannel;String v=channelSpinner.getSelectedItem().toString().toLowerCase(java.util.Locale.US);if(v.startsWith("leadership"))return"leadership";if(v.startsWith("war"))return"war";if(v.startsWith("oc"))return"oc";return"general";}

    private void openChannel(String channel){
        if(session==null)return;currentChannel=channel;
        JSONArray cached=StartupWarmCache.chat(session.factionId,channel,CHAT_CACHE_MS);
        if(cached!=null)render(cached,channel,false);
        load(cached==null);
    }

    private void updateFreshness(){if(freshnessView!=null&&session!=null)freshnessView.setText(DataFreshness.label(StartupWarmCache.chatAgeMs(session.factionId,currentChannel),refreshing));}

    private void render(JSONArray messages,String selected,boolean scrollToBottom){
        if(session==null)return;
        if(messageField!=null)draftText=messageField.getText().toString();
        currentChannel=selected==null||selected.isBlank()?"general":selected;
        JSONArray serverMessages=messages==null?new JSONArray():messages;
        lastRenderedSignature=serverMessages.toString();
        JSONArray displayMessages=withPending(serverMessages,currentChannel,session.factionId);
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"Community","Faction Chat",session.factionName+" • live TornFCA community chat for your current faction");
        freshnessView=TornFcaUi.text(this,DataFreshness.label(StartupWarmCache.chatAgeMs(session.factionId,currentChannel),refreshing),11.5f,TornFcaUi.MUTED,false);LinearLayout.LayoutParams fsp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fsp.bottomMargin=TornFcaUi.dp(this,8);r.addView(freshnessView,fsp);

        boolean leadership=AccessPolicy.isLeaderPosition(session.position)&&!DeveloperPreviewStore.isMemberPreview(this);
        if(leadership){
            Button reports=TornFcaUi.button(this,"Reports & Moderation",TornFcaUi.GOLD);reports.setOnClickListener(v->startActivity(new Intent(this,CommunityModerationActivity.class)));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));rp.bottomMargin=TornFcaUi.dp(this,8);r.addView(reports,rp);
        }
        TextView safetyHint=TornFcaUi.text(this,"Tap another member's message for profile, report and block options.",11.5f,TornFcaUi.MUTED,false);LinearLayout.LayoutParams shp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);shp.bottomMargin=TornFcaUi.dp(this,8);r.addView(safetyHint,shp);

        List<String> channels=new ArrayList<>();channels.add("General");channels.add("War");channels.add("OC");if(leadership)channels.add("Leadership");
        if("leadership".equals(currentChannel)&&!containsChannel(channels,"leadership"))currentChannel="general";
        channelSpinner=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,channels);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);channelSpinner.setAdapter(adapter);
        for(int i=0;i<channels.size();i++)if(channels.get(i).toLowerCase(java.util.Locale.US).startsWith(currentChannel)){channelSpinner.setSelection(i);break;}
        channelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){boolean first=true;@Override public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){if(first){first=false;return;}String next=selectedChannel();if(!next.equals(currentChannel))openChannel(next);}@Override public void onNothingSelected(android.widget.AdapterView<?> p){}});
        r.addView(channelSpinner,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,50)));
        LinearLayout.LayoutParams spacer=new LinearLayout.LayoutParams(1,TornFcaUi.dp(this,10));r.addView(new View(this),spacer);

        int visible=0;
        for(int i=0;i<displayMessages.length();i++){
            JSONObject m=displayMessages.optJSONObject(i);if(m==null)continue;
            int authorId=m.optInt("author_id",0);boolean mine=authorId==session.playerId;boolean sending=m.optBoolean("_pending",false),failed=m.optBoolean("_failed",false);
            if(!mine&&!sending&&!failed&&BlockedUserStore.isBlocked(this,session.factionId,authorId))continue;
            visible++;
            String status=failed?"NOT SENT • ":sending?"SENDING • ":mine?"YOU • ":"";
            String eye=status+m.optString("channel","general").toUpperCase(java.util.Locale.US);String title=m.optString("author_name","Member");long ts=m.optLong("created_at",0L);
            String body=m.optString("message","")+(ts>0?"\n"+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(ts*1000L)):"");
            if(failed){String error=m.optString("_error","");if(!error.isBlank())body+="\n"+error;}
            int cardAccent=failed?TornFcaUi.RED:sending?TornFcaUi.GOLD:mine?TornFcaUi.GREEN:accent(currentChannel);
            LinearLayout card=TornFcaUi.card(this,eye,title,body,cardAccent);
            if(failed){String tempId=m.optString("id","");Button retry=TornFcaUi.button(this,"Retry Send",TornFcaUi.GOLD);retry.setOnClickListener(v->retryPending(tempId));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,42));rp.topMargin=TornFcaUi.dp(this,9);card.addView(retry,rp);}
            else if(!mine&&authorId>0){card.setClickable(true);card.setFocusable(true);card.setOnClickListener(v->showMemberActions(m,authorId,title));}
            TornFcaUi.add(this,r,card);
        }
        if(visible==0)TornFcaUi.add(this,r,TornFcaUi.card(this,"QUIET CHANNEL","No visible messages","Start the conversation, or wait for another faction member to post.",TornFcaUi.BORDER));

        int blocked=BlockedUserStore.count(this,session.factionId);
        if(blocked>0){
            LinearLayout safety=TornFcaUi.card(this,"CHAT SAFETY","Blocked users",blocked+" user"+(blocked==1?" is":"s are")+" hidden on this device for this faction.",TornFcaUi.PURPLE);
            Button clear=TornFcaUi.button(this,"Unblock All",TornFcaUi.BORDER);clear.setOnClickListener(v->confirmClearBlocks());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));cp.topMargin=TornFcaUi.dp(this,9);safety.addView(clear,cp);TornFcaUi.add(this,r,safety);
        }

        LinearLayout compose=TornFcaUi.card(this,"MESSAGE","Send to "+currentChannel,"Your message appears here immediately while TornFCA confirms it in the background. This room is tied to faction "+session.factionId+" and automatically moves if your Torn faction changes.",TornFcaUi.BLUE);
        messageField=new EditText(this);messageField.setHint("Write a message…");messageField.setHintTextColor(TornFcaUi.MUTED);messageField.setTextColor(TornFcaUi.TEXT);messageField.setMaxLines(4);messageField.setText(draftText);messageField.setPadding(TornFcaUi.dp(this,12),TornFcaUi.dp(this,8),TornFcaUi.dp(this,12),TornFcaUi.dp(this,8));messageField.setBackground(TornFcaUi.rounded(this,TornFcaUi.PANEL2,TornFcaUi.BORDER,11));LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,82));mp.topMargin=TornFcaUi.dp(this,9);compose.addView(messageField,mp);
        Button send=TornFcaUi.button(this,"Send Message",TornFcaUi.BLUE);send.setOnClickListener(v->send());LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));sp.topMargin=TornFcaUi.dp(this,8);compose.addView(send,sp);TornFcaUi.add(this,r,compose);
        Button refresh=TornFcaUi.button(this,refreshing?"Refreshing Chat…":"Refresh Chat",TornFcaUi.BORDER);refresh.setEnabled(!refreshing);refresh.setOnClickListener(v->load(false));r.addView(refresh,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46)));
        setContentView(s);s.requestApplyInsets();if(scrollToBottom)s.post(()->s.fullScroll(View.FOCUS_DOWN));
    }

    private void showMemberActions(JSONObject message,int authorId,String name){
        String[] actions={"View Torn profile","Report this message","Block user"};
        new AlertDialog.Builder(this).setTitle(name).setItems(actions,(dialog,which)->{
            if(which==0){try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.torn.com/profiles.php?XID="+authorId)));}catch(Exception e){Toast.makeText(this,"Unable to open Torn profile.",Toast.LENGTH_SHORT).show();}}
            else if(which==1)reportMessage(message);
            else if(which==2)blockUser(authorId,name);
        }).setNegativeButton("Cancel",null).show();
    }

    private JSONArray withPending(JSONArray server,String channel,int factionId){
        JSONArray out=new JSONArray();for(int i=0;i<server.length();i++)out.put(server.opt(i));
        for(JSONObject pending:pendingById.values())if(pending!=null&&pending.optInt("_faction_id",0)==factionId&&channel.equals(pending.optString("channel","general")))out.put(pending);
        return out;
    }

    private void send(){
        String text=messageField==null?"":messageField.getText().toString().trim();if(text.isEmpty())return;
        if(text.length()>1000){Toast.makeText(this,"Messages are limited to 1,000 characters.",Toast.LENGTH_SHORT).show();return;}
        String key=new SecureApiKeyStore(this).load();if(key==null||key.isBlank()||session==null)return;
        String channel=selectedChannel();int factionId=session.factionId;String tempId="local-"+UUID.randomUUID();
        JSONObject pending=new JSONObject();try{pending.put("id",tempId);pending.put("faction_id",factionId);pending.put("_faction_id",factionId);pending.put("channel",channel);pending.put("author_id",session.playerId);pending.put("author_name",session.playerName);pending.put("message",text);pending.put("created_at",System.currentTimeMillis()/1000L);pending.put("_pending",true);}catch(Exception ignored){}
        pendingById.put(tempId,pending);draftText="";messageField.setText("");
        JSONArray cached=StartupWarmCache.chat(factionId,channel,CHAT_CACHE_MS);render(cached==null?new JSONArray():cached,channel,true);
        sendPending(key,tempId,factionId,channel,text);
    }

    private void retryPending(String tempId){
        JSONObject pending=pendingById.get(tempId);if(pending==null||session==null)return;
        int factionId=pending.optInt("_faction_id",0);if(factionId!=session.factionId){pendingById.remove(tempId);Toast.makeText(this,"Your faction changed. Re-enter the message in the current faction chat.",Toast.LENGTH_LONG).show();return;}
        try{pending.put("_pending",true);pending.put("_failed",false);pending.remove("_error");}catch(Exception ignored){}
        JSONArray cached=StartupWarmCache.chat(factionId,pending.optString("channel","general"),CHAT_CACHE_MS);render(cached==null?new JSONArray():cached,pending.optString("channel","general"),true);
        String key=new SecureApiKeyStore(this).load();if(key!=null&&!key.isBlank())sendPending(key,tempId,factionId,pending.optString("channel","general"),pending.optString("message",""));
    }

    private void sendPending(String key,String tempId,int factionId,String channel,String text){
        new Thread(()->{try{
            JSONObject response=CommunityBackendClient.sendChatMessage(key,channel,text,factionId);JSONObject actual=response.optJSONObject("message");
            runOnUiThread(()->{
                pendingById.remove(tempId);applyResponseUser(response,key);
                int targetFaction=session==null?factionId:session.factionId;JSONArray base=StartupWarmCache.chat(targetFaction,channel,CHAT_CACHE_MS);JSONArray merged=upsert(base,actual);StartupWarmCache.putChat(targetFaction,channel,merged);
                if(active&&channel.equals(currentChannel))render(merged,channel,true);
            });
        }catch(Exception e){String error=e.getMessage()==null?"Unable to send message.":e.getMessage();runOnUiThread(()->{
            JSONObject pending=pendingById.get(tempId);if(pending!=null){try{pending.put("_pending",false);pending.put("_failed",true);pending.put("_error",error);}catch(Exception ignored){}JSONArray cached=StartupWarmCache.chat(factionId,channel,CHAT_CACHE_MS);render(cached==null?new JSONArray():cached,channel,true);}else Toast.makeText(this,error,Toast.LENGTH_LONG).show();
            if(error.toLowerCase(java.util.Locale.US).contains("faction changed")){currentChannel="general";load(true);}
        });}},"TornFCA-ChatSend").start();
    }

    private void applyResponseUser(JSONObject response,String key){
        if(response==null)return;CommunityBackendClient.ChatSnapshot snapshot=new CommunityBackendClient.ChatSnapshot(response);if(snapshot.playerId>0&&snapshot.factionId>0)applySnapshotIdentity(snapshot,key);
    }

    private JSONArray upsert(JSONArray base,JSONObject message){
        JSONArray out=new JSONArray();String id=message==null?"":message.optString("id","");boolean replaced=false;
        JSONArray safe=base==null?new JSONArray():base;for(int i=0;i<safe.length();i++){JSONObject row=safe.optJSONObject(i);if(row!=null&&!id.isBlank()&&id.equals(row.optString("id",""))){out.put(message);replaced=true;}else out.put(safe.opt(i));}
        if(message!=null&&!replaced)out.put(message);return out;
    }

    private void reportMessage(JSONObject message){
        EditText reason=new EditText(this);reason.setHint("Reason (optional)");reason.setSingleLine(false);reason.setMaxLines(3);reason.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(this).setTitle("Report message").setMessage("Send this message and its faction-scoped context to TornFCA moderation for review?").setView(reason).setNegativeButton("Cancel",null).setPositiveButton("Report",(d,w)->submitReport(message,reason.getText().toString().trim())).show();
    }
    private void submitReport(JSONObject message,String reason){String key=new SecureApiKeyStore(this).load();if(key==null)return;String id=message.optString("id","");if(id.isBlank()){Toast.makeText(this,"This message cannot be reported.",Toast.LENGTH_SHORT).show();return;}new Thread(()->{try{CommunityBackendClient.reportChatMessage(key,id,reason);runOnUiThread(()->Toast.makeText(this,"Report sent for review.",Toast.LENGTH_SHORT).show());}catch(Exception e){String m=e.getMessage()==null?"Unable to report this message.":e.getMessage();runOnUiThread(()->Toast.makeText(this,m,Toast.LENGTH_LONG).show());}},"TornFCA-ChatReport").start();}
    private void blockUser(int authorId,String name){new AlertDialog.Builder(this).setTitle("Block "+name+"?").setMessage("Their TornFCA faction-chat messages and chat push notifications will be hidden on this device for this faction. Blocking does not report them.").setNegativeButton("Cancel",null).setPositiveButton("Block",(d,w)->{BlockedUserStore.block(this,session.factionId,authorId,name);Toast.makeText(this,name+" blocked.",Toast.LENGTH_SHORT).show();JSONArray cached=StartupWarmCache.chat(session.factionId,currentChannel,CHAT_CACHE_MS);if(cached!=null)render(cached,currentChannel,false);else load(false);}).show();}
    private void confirmClearBlocks(){new AlertDialog.Builder(this).setTitle("Unblock all chat users?").setMessage("This clears your device-local block list for "+session.factionName+".").setNegativeButton("Cancel",null).setPositiveButton("Unblock All",(d,w)->{BlockedUserStore.clearFaction(this,session.factionId);JSONArray cached=StartupWarmCache.chat(session.factionId,currentChannel,CHAT_CACHE_MS);if(cached!=null)render(cached,currentChannel,false);else load(false);}).show();}

    private boolean containsChannel(List<String> channels,String prefix){for(String c:channels)if(c.toLowerCase(java.util.Locale.US).startsWith(prefix))return true;return false;}
    private String safe(String value){return value==null?"":value.trim();}
    private String safeName(String value,String fallback){String s=safe(value);return s.isEmpty()?fallback:s;}
    private int accent(String channel){if("leadership".equals(channel))return TornFcaUi.GOLD;if("war".equals(channel))return TornFcaUi.RED;if("oc".equals(channel))return TornFcaUi.PURPLE;return TornFcaUi.BLUE;}
    private void renderLoading(String message){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Community","Faction Chat",message);TornFcaUi.add(this,r,TornFcaUi.card(this,"LOADING","Connecting…","Verifying your faction and loading recent messages.",TornFcaUi.BLUE));setContentView(s);s.requestApplyInsets();}
    private void renderOffline(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Community","Faction Chat","Community service is not connected in this build.");TornFcaUi.add(this,r,TornFcaUi.card(this,"OFFLINE","Faction chat is ready for backend connection","The Android chat client is installed, but no TornFCA community backend URL was compiled into this build. Other faction/member features remain available.",TornFcaUi.GOLD));setContentView(s);s.requestApplyInsets();}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Community","Faction Chat","Unable to connect");TornFcaUi.add(this,r,TornFcaUi.card(this,"CHAT UNAVAILABLE","Could not load faction chat",message,TornFcaUi.RED));Button retry=TornFcaUi.button(this,"Retry",TornFcaUi.GOLD);retry.setOnClickListener(v->{renderLoading("Reconnecting…");bootstrap();});r.addView(retry,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48)));setContentView(s);s.requestApplyInsets();});}
}

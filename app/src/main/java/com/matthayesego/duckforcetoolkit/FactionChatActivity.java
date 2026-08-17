package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** TornFCA app-native faction chat with faction-scoped reporting and device-local user blocking. */
public class FactionChatActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active=false,loading=false;private AuthSession session;private Spinner channelSpinner;private EditText messageField;
    private final Runnable poll=this::pollOnce;
    private void pollOnce(){if(active){load(false);handler.postDelayed(poll,20000L);}}
    @Override protected void onCreate(Bundle b){super.onCreate(b);renderLoading("Opening your faction community…");bootstrap();}
    @Override protected void onResume(){super.onResume();active=true;handler.removeCallbacks(poll);handler.postDelayed(poll,20000L);PushNotifications.syncIfReady(this);}
    @Override protected void onPause(){active=false;handler.removeCallbacks(poll);super.onPause();}

    private void bootstrap(){if(!CommunityBackendClient.isConfigured()){renderOffline();return;}String key=new SecureApiKeyStore(this).load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key first.");return;}new Thread(()->{try{session=TornApiClient.cachedSession(key);if(session==null)session=TornApiClient.authenticate(key);runOnUiThread(()->load(true));}catch(Exception e){renderError(e.getMessage()==null?"Unable to verify faction membership.":e.getMessage());}},"TornFCA-ChatBootstrap").start();}
    private void load(boolean full){if(loading||session==null)return;String key=new SecureApiKeyStore(this).load();if(key==null)return;loading=true;String channel=selectedChannel();new Thread(()->{try{JSONArray messages=CommunityBackendClient.getChatMessages(key,channel);runOnUiThread(()->render(messages,channel));}catch(Exception e){if(full)renderError(e.getMessage()==null?"Unable to load faction chat.":e.getMessage());else runOnUiThread(()->Toast.makeText(this,"Chat refresh failed.",Toast.LENGTH_SHORT).show());}finally{loading=false;}},"TornFCA-ChatLoad").start();}
    private String selectedChannel(){if(channelSpinner==null||channelSpinner.getSelectedItem()==null)return"general";String v=channelSpinner.getSelectedItem().toString().toLowerCase(java.util.Locale.US);if(v.startsWith("leadership"))return"leadership";if(v.startsWith("war"))return"war";if(v.startsWith("oc"))return"oc";return"general";}

    private void render(JSONArray messages,String selected){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"Member Center","Faction Chat",session.factionName+" • TornFCA community chat (separate from Torn's native chat)");
        List<String> channels=new ArrayList<>();channels.add("General");channels.add("War");channels.add("OC");if(AccessPolicy.isLeaderPosition(session.position)&&!DeveloperPreviewStore.isMemberPreview(this))channels.add("Leadership");
        channelSpinner=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,channels);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);channelSpinner.setAdapter(adapter);
        for(int i=0;i<channels.size();i++)if(channels.get(i).toLowerCase(java.util.Locale.US).startsWith(selected)){channelSpinner.setSelection(i);break;}
        channelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){boolean first=true;@Override public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){if(first){first=false;return;}load(false);}@Override public void onNothingSelected(android.widget.AdapterView<?> p){}});
        r.addView(channelSpinner,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,50)));
        LinearLayout.LayoutParams spacer=new LinearLayout.LayoutParams(1,TornFcaUi.dp(this,10));r.addView(new android.view.View(this),spacer);

        int visible=0;
        for(int i=0;i<messages.length();i++){
            JSONObject m=messages.optJSONObject(i);if(m==null)continue;
            int authorId=m.optInt("author_id",0);boolean mine=authorId==session.playerId;
            if(!mine&&BlockedUserStore.isBlocked(this,session.factionId,authorId))continue;
            visible++;
            String eye=(mine?"YOU • ":"")+m.optString("channel","general").toUpperCase(java.util.Locale.US);String title=m.optString("author_name","Member");long ts=m.optLong("created_at",0L);String body=m.optString("message","")+(ts>0?"\n"+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(ts*1000L)):"");
            LinearLayout card=TornFcaUi.card(this,eye,title,body,mine?TornFcaUi.GREEN:accent(selected));
            if(!mine&&authorId>0){
                LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
                Button report=TornFcaUi.button(this,"Report",TornFcaUi.GOLD);report.setOnClickListener(v->reportMessage(m));
                Button block=TornFcaUi.button(this,"Block User",TornFcaUi.RED);block.setOnClickListener(v->blockUser(authorId,title));
                LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,TornFcaUi.dp(this,42),1f);LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,TornFcaUi.dp(this,42),1f);b.leftMargin=TornFcaUi.dp(this,7);actions.addView(report,a);actions.addView(block,b);
                LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,42));ap.topMargin=TornFcaUi.dp(this,9);card.addView(actions,ap);
            }
            TornFcaUi.add(this,r,card);
        }
        if(visible==0)TornFcaUi.add(this,r,TornFcaUi.card(this,"QUIET CHANNEL","No visible messages","Start the conversation, or refresh after changing your block list.",TornFcaUi.BORDER));

        int blocked=BlockedUserStore.count(this,session.factionId);
        if(blocked>0){
            LinearLayout safety=TornFcaUi.card(this,"CHAT SAFETY","Blocked users",blocked+" user"+(blocked==1?" is":"s are")+" hidden on this device for this faction.",TornFcaUi.PURPLE);
            Button clear=TornFcaUi.button(this,"Unblock All",TornFcaUi.BORDER);clear.setOnClickListener(v->confirmClearBlocks());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));cp.topMargin=TornFcaUi.dp(this,9);safety.addView(clear,cp);TornFcaUi.add(this,r,safety);
        }

        LinearLayout compose=TornFcaUi.card(this,"MESSAGE","Send to "+selected,"Messages are visible only to authenticated members of this faction. Leadership chat is separately permission-gated. Use Report or Block User on another member's message when needed.",TornFcaUi.BLUE);
        messageField=new EditText(this);messageField.setHint("Write a message…");messageField.setHintTextColor(TornFcaUi.MUTED);messageField.setTextColor(TornFcaUi.TEXT);messageField.setMaxLines(4);messageField.setPadding(TornFcaUi.dp(this,12),TornFcaUi.dp(this,8),TornFcaUi.dp(this,12),TornFcaUi.dp(this,8));messageField.setBackground(TornFcaUi.rounded(this,TornFcaUi.PANEL2,TornFcaUi.BORDER,11));LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,82));mp.topMargin=TornFcaUi.dp(this,9);compose.addView(messageField,mp);
        Button send=TornFcaUi.button(this,"Send Message",TornFcaUi.BLUE);send.setOnClickListener(v->send(send));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));sp.topMargin=TornFcaUi.dp(this,8);compose.addView(send,sp);TornFcaUi.add(this,r,compose);
        Button refresh=TornFcaUi.button(this,"Refresh Chat",TornFcaUi.BORDER);refresh.setOnClickListener(v->load(false));r.addView(refresh,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46)));
        setContentView(s);s.requestApplyInsets();
    }

    private void reportMessage(JSONObject message){
        EditText reason=new EditText(this);reason.setHint("Reason (optional)");reason.setSingleLine(false);reason.setMaxLines(3);reason.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(this).setTitle("Report message").setMessage("Send this message and its faction-scoped context to TornFCA moderation for review?").setView(reason).setNegativeButton("Cancel",null).setPositiveButton("Report",(d,w)->submitReport(message,reason.getText().toString().trim())).show();
    }
    private void submitReport(JSONObject message,String reason){String key=new SecureApiKeyStore(this).load();if(key==null)return;String id=message.optString("id","");if(id.isBlank()){Toast.makeText(this,"This message cannot be reported.",Toast.LENGTH_SHORT).show();return;}new Thread(()->{try{CommunityBackendClient.reportChatMessage(key,id,reason);runOnUiThread(()->Toast.makeText(this,"Report sent for review.",Toast.LENGTH_SHORT).show());}catch(Exception e){String m=e.getMessage()==null?"Unable to report this message.":e.getMessage();runOnUiThread(()->Toast.makeText(this,m,Toast.LENGTH_LONG).show());}},"TornFCA-ChatReport").start();}
    private void blockUser(int authorId,String name){new AlertDialog.Builder(this).setTitle("Block "+name+"?").setMessage("Their TornFCA faction-chat messages and chat push notifications will be hidden on this device for this faction. Blocking does not report them.").setNegativeButton("Cancel",null).setPositiveButton("Block",(d,w)->{BlockedUserStore.block(this,session.factionId,authorId,name);Toast.makeText(this,name+" blocked.",Toast.LENGTH_SHORT).show();load(false);}).show();}
    private void confirmClearBlocks(){new AlertDialog.Builder(this).setTitle("Unblock all chat users?").setMessage("This clears your device-local block list for "+session.factionName+".").setNegativeButton("Cancel",null).setPositiveButton("Unblock All",(d,w)->{BlockedUserStore.clearFaction(this,session.factionId);load(false);}).show();}

    private void send(Button button){String text=messageField==null?"":messageField.getText().toString().trim();if(text.isEmpty())return;if(text.length()>1000){Toast.makeText(this,"Messages are limited to 1,000 characters.",Toast.LENGTH_SHORT).show();return;}String key=new SecureApiKeyStore(this).load();if(key==null)return;String channel=selectedChannel();button.setEnabled(false);button.setText("Sending…");new Thread(()->{try{CommunityBackendClient.sendChatMessage(key,channel,text);runOnUiThread(()->{Toast.makeText(this,"Message sent.",Toast.LENGTH_SHORT).show();load(false);});}catch(Exception e){String m=e.getMessage()==null?"Unable to send message.":e.getMessage();runOnUiThread(()->{button.setEnabled(true);button.setText("Send Message");Toast.makeText(this,m,Toast.LENGTH_LONG).show();});}},"TornFCA-ChatSend").start();}
    private int accent(String channel){if("leadership".equals(channel))return TornFcaUi.GOLD;if("war".equals(channel))return TornFcaUi.RED;if("oc".equals(channel))return TornFcaUi.PURPLE;return TornFcaUi.BLUE;}
    private void renderLoading(String message){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Faction Chat",message);TornFcaUi.add(this,r,TornFcaUi.card(this,"LOADING","Connecting…","Verifying your faction and loading recent messages.",TornFcaUi.BLUE));setContentView(s);s.requestApplyInsets();}
    private void renderOffline(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Faction Chat","Community service is not connected in this build.");TornFcaUi.add(this,r,TornFcaUi.card(this,"OFFLINE","Faction chat is ready for backend connection","The Android chat client is installed, but no TornFCA community backend URL was compiled into this build. Other faction/member features remain available.",TornFcaUi.GOLD));setContentView(s);s.requestApplyInsets();}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Faction Chat","Unable to connect");TornFcaUi.add(this,r,TornFcaUi.card(this,"CHAT UNAVAILABLE","Could not load faction chat",message,TornFcaUi.RED));Button retry=TornFcaUi.button(this,"Retry",TornFcaUi.GOLD);retry.setOnClickListener(v->{renderLoading("Reconnecting…");bootstrap();});r.addView(retry,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48)));setContentView(s);s.requestApplyInsets();});}
}

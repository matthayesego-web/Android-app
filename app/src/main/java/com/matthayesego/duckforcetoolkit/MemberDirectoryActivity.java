package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Free, member-safe faction directory. Leadership analytics intentionally remain outside this surface. */
public class MemberDirectoryActivity extends Activity {
    private SecureApiKeyStore keyStore;
    private AuthSession session;
    private JSONArray members = new JSONArray();
    private boolean refreshing;

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);keyStore=new SecureApiKeyStore(this);open();}

    private void open(){
        String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key to view your faction directory.");return;}
        AuthSession hot=TornApiClient.cachedSession(key);
        int factionId=hot==null?0:hot.factionId;
        JSONArray cached=FactionMemberCache.load(factionId);
        if(cached!=null&&hot!=null){
            session=hot;
            members=cached;
            render("");
            refresh(false);
        }else{
            showLoading();
            refresh(true);
        }
    }

    private void showLoading(){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"Member Center","Faction Directory","Loading the current roster from Torn…");
        TornFcaUi.add(this,r,TornFcaUi.card(this,"MEMBER SAFE","Loading roster","This view contains current roster/status information only. Leadership analytics and historical intelligence are not exposed here.",TornFcaUi.BLUE));
        setContentView(s);s.requestApplyInsets();
    }

    private void refresh(boolean firstLoad){
        if(refreshing)return;
        String key=keyStore.load();if(key==null||key.isBlank())return;
        refreshing=true;
        new Thread(()->{
            try{
                AuthSession verified=TornApiClient.cachedSession(key);if(verified==null)verified=TornApiClient.authenticate(key);
                JSONObject root=TornApiClient.getJson("/faction/members",key);JSONArray data=root.optJSONArray("members");
                session=verified;members=data==null?new JSONArray():data;FactionMemberCache.save(verified.factionId,members);
                refreshing=false;
                runOnUiThread(()->render(""));
            }catch(Exception e){
                refreshing=false;
                String message=e.getMessage()==null?"Unable to refresh faction members.":e.getMessage();
                if(firstLoad)renderError(message);else runOnUiThread(()->Toast.makeText(this,"Roster refresh failed. Showing cached data.",Toast.LENGTH_SHORT).show());
            }
        },"TornFCA-MemberDirectory").start();
    }

    private void render(String filter){
        if(session==null){showLoading();return;}
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        long age=FactionMemberCache.ageMs(session.factionId);
        TornFcaUi.header(this,r,"Member Center","Faction Directory",session.factionName+" • current member-safe roster");
        TornFcaUi.add(this,r,TornFcaUi.card(this,"DATA STATUS","Roster snapshot",DataFreshness.label(age,refreshing),TornFcaUi.BORDER));

        EditText search=new EditText(this);search.setHint("Search member name or Torn ID");search.setHintTextColor(TornFcaUi.MUTED);search.setTextColor(TornFcaUi.TEXT);search.setSingleLine(true);search.setInputType(InputType.TYPE_CLASS_TEXT);search.setPadding(TornFcaUi.dp(this,14),0,TornFcaUi.dp(this,14),0);search.setBackground(TornFcaUi.rounded(this,TornFcaUi.PANEL2,TornFcaUi.BLUE,12));search.setText(filter==null?"":filter);
        r.addView(search,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,52)));
        Button go=TornFcaUi.button(this,"Search directory",TornFcaUi.BLUE);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));gp.topMargin=TornFcaUi.dp(this,8);r.addView(go,gp);
        Button all=TornFcaUi.button(this,"Show all members",TornFcaUi.BORDER);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));ap.topMargin=TornFcaUi.dp(this,7);r.addView(all,ap);
        Button refresh=TornFcaUi.button(this,refreshing?"Refreshing…":"Refresh roster",TornFcaUi.BORDER);refresh.setEnabled(!refreshing);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,44));rp.topMargin=TornFcaUi.dp(this,7);rp.bottomMargin=TornFcaUi.dp(this,14);r.addView(refresh,rp);
        go.setOnClickListener(v->render(search.getText().toString().trim()));all.setOnClickListener(v->render(""));refresh.setOnClickListener(v->refresh(false));

        List<JSONObject> visible=matching(filter);
        TornFcaUi.addSection(this,r,filter==null||filter.isBlank()?"CURRENT ROSTER":"SEARCH RESULTS");
        if(visible.isEmpty())TornFcaUi.add(this,r,TornFcaUi.card(this,"NO MATCH","No current member matched",filter==null||filter.isBlank()?"No roster entries were returned by Torn.":"Try another name or Torn ID.",TornFcaUi.GOLD));
        for(JSONObject member:visible)addMemberCard(r,member);

        TornFcaUi.add(this,r,TornFcaUi.card(this,"FREE CORE","Why this view stays simple","The free directory shows current faction membership, position, status, last action and basic assignment context. Participation history, inactivity trends and advanced member intelligence remain permission-aware leadership tooling.",TornFcaUi.GREEN));
        r.addView(TornFcaUi.footer(this,"TornFCA v"+TornFcaBrand.VERSION+" • member-safe directory"));
        setContentView(s);s.requestApplyInsets();
    }

    private List<JSONObject> matching(String filter){
        String q=filter==null?"":filter.trim().toLowerCase(Locale.US);List<JSONObject> out=new ArrayList<>();
        for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m==null)continue;String name=m.optString("name","");int id=m.optInt("id",0);if(q.isEmpty()||name.toLowerCase(Locale.US).contains(q)||String.valueOf(id).contains(q))out.add(m);}
        out.sort(Comparator.comparing(o->o.optString("name","").toLowerCase(Locale.US)));return out;
    }

    private void addMemberCard(LinearLayout root,JSONObject m){
        int id=m.optInt("id",0);String name=m.optString("name","Member"),position=m.optString("position","Member");
        JSONObject status=m.optJSONObject("status"),last=m.optJSONObject("last_action");
        String state=status==null?"Unknown":status.optString("state","Unknown"),detail=status==null?"":status.optString("description",status.optString("details",""));
        String lastState=last==null?"Unknown":last.optString("status","Unknown"),relative=last==null?"":last.optString("relative","");
        boolean online="Online".equalsIgnoreCase(lastState);StringBuilder body=new StringBuilder();body.append(position).append(" • ").append(state);if(!detail.isBlank())body.append(" — ").append(detail);body.append("\nLast action: ").append(lastState);if(!relative.isBlank())body.append(" • ").append(relative);body.append("\nOC: ").append(m.optBoolean("is_in_oc",false)?"Assigned":"Not assigned").append(" • Territory wall: ").append(m.optBoolean("is_on_wall",false)?"Yes":"No");
        LinearLayout card=TornFcaUi.card(this,"TORN #"+id,name,body.toString(),online?TornFcaUi.GREEN:TornFcaUi.BORDER);
        Button profile=TornFcaUi.button(this,"Open Torn profile",TornFcaUi.BLUE);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,43));pp.topMargin=TornFcaUi.dp(this,10);card.addView(profile,pp);profile.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.torn.com/profiles.php?XID="+id)));}catch(Exception ignored){}});
        TornFcaUi.add(this,root,card);
    }

    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Member Center","Faction Directory","Directory unavailable");TornFcaUi.add(this,r,TornFcaUi.card(this,"CONNECTION","Unable to load",message,TornFcaUi.RED));Button retry=TornFcaUi.button(this,"Retry",TornFcaUi.GOLD);retry.setOnClickListener(v->{showLoading();refresh(true);});r.addView(retry,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48)));setContentView(s);s.requestApplyInsets();});}
}

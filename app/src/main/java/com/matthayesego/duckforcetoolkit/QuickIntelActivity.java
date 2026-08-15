package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class QuickIntelActivity extends Activity {
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_PULSE = "PULSE";
    public static final String MODE_LOOKUP = "LOOKUP";

    private static final int BG=Color.rgb(8,12,18), PANEL=Color.rgb(20,27,38), PANEL2=Color.rgb(27,36,49), BORDER=Color.rgb(49,63,81);
    private static final int TEXT=Color.rgb(245,248,252), MUTED=Color.rgb(151,163,179), GOLD=Color.rgb(243,184,52), BLUE=Color.rgb(88,166,255), GOOD=Color.rgb(63,185,80), BAD=Color.rgb(248,81,73);

    private SecureApiKeyStore keyStore;
    private String mode;
    private String factionName;
    private JSONArray members = new JSONArray();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        keyStore = new SecureApiKeyStore(this);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        factionName = getIntent().getStringExtra(FactionOpsActivity.EXTRA_FACTION_NAME);
        if (mode == null) mode = MODE_PULSE;
        if (factionName == null || factionName.trim().isEmpty()) factionName = "Faction";
        showLoading();
        load();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,BORDER,11));return b;}
    private LinearLayout card(String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(13),dp(15),dp(13));c.setBackground(rounded(PANEL,stroke,16));c.addView(text(title,17,TEXT,true));if(body!=null&&!body.isEmpty()){TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);c.addView(b,p);}return c;}
    private void addCard(LinearLayout root,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(9);root.addView(c,p);}
    private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);s.setPadding(dp(16),dp(16),dp(16),dp(28));return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}

    private String title(){return MODE_LOOKUP.equals(mode)?"Member Quick Lookup":"Faction Pulse";}
    private void addHeader(LinearLayout r,String subtitle){Button back=button("← Companion");back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(124),dp(44)));TextView t=text(title(),27,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);r.addView(t,tp);TextView s=text(factionName+" • "+subtitle,13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(4);sp.bottomMargin=dp(14);r.addView(s,sp);}
    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Loading member snapshot…");addCard(r,card("Quick intel","Reading the current faction member list once, then presenting it locally for fast access.",BLUE));setContentView(s);}

    private void load(){String key=keyStore.load();if(key==null||key.trim().isEmpty()){renderError("Reconnect your Torn API key to use quick intel.");return;}new Thread(()->{try{JSONArray data=TornApiClient.getJson("/faction/members",key).optJSONArray("members");members=data==null?new JSONArray():data;runOnUiThread(()->{if(MODE_LOOKUP.equals(mode))renderLookup(null);else renderPulse();});}catch(Exception e){renderError(e.getMessage()==null?"Unable to load faction members.":e.getMessage());}}).start();}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Data unavailable");addCard(r,card("Unable to load",message,BAD));setContentView(s);});}

    private void renderPulse(){
        int total=members.length(),online=0,hospital=0,jail=0,travel=0,notOc=0,onWall=0,available=0;
        List<String> ready=new ArrayList<>();
        for(int i=0;i<members.length();i++){
            JSONObject m=members.optJSONObject(i);if(m==null)continue;
            JSONObject last=m.optJSONObject("last_action"), status=m.optJSONObject("status");
            String lastState=last==null?"":last.optString("status","");String state=status==null?"":status.optString("state","");String lower=state.toLowerCase(Locale.US);
            boolean isOnline="Online".equalsIgnoreCase(lastState);if(isOnline)online++;
            if(lower.contains("hospital"))hospital++;if(lower.contains("jail"))jail++;if(lower.contains("travel")||lower.contains("abroad"))travel++;
            if(!m.optBoolean("is_in_oc",false))notOc++;if(m.optBoolean("is_on_wall",false))onWall++;
            boolean blocked=lower.contains("hospital")||lower.contains("jail")||lower.contains("travel")||lower.contains("abroad");if(!blocked){available++;if(isOnline)ready.add(m.optString("name","Unknown"));}
        }
        Collections.sort(ready,String.CASE_INSENSITIVE_ORDER);
        ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Live member health snapshot");
        addCard(r,card("Faction snapshot",total+" members • "+online+" online • "+available+" available",GOOD));
        addCard(r,card("Availability",hospital+" hospital • "+jail+" jailed • "+travel+" traveling/abroad",BORDER));
        addCard(r,card("OC & territory",notOc+" not currently in an OC • "+onWall+" on territory wall",BLUE));
        if(ready.isEmpty())addCard(r,card("Online & available","No members matched the online-and-available filter.",BORDER));
        else {StringBuilder b=new StringBuilder();for(int i=0;i<Math.min(20,ready.size());i++){if(i>0)b.append(" • ");b.append(ready.get(i));}if(ready.size()>20)b.append(" • +").append(ready.size()-20).append(" more");addCard(r,card("Ready right now",b.toString(),GOLD));}
        setContentView(s);
    }

    private void renderLookup(String query){
        ScrollView s=shell();LinearLayout r=root(s);addHeader(r,"Search current faction members");
        EditText search=new EditText(this);search.setHint("Member name");search.setHintTextColor(MUTED);search.setTextColor(TEXT);search.setSingleLine(true);search.setInputType(InputType.TYPE_CLASS_TEXT);search.setPadding(dp(14),0,dp(14),0);search.setBackground(rounded(PANEL2,BORDER,11));if(query!=null)search.setText(query);r.addView(search,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50)));
        Button go=button("Search Member");LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));gp.topMargin=dp(8);gp.bottomMargin=dp(12);r.addView(go,gp);go.setOnClickListener(v->renderLookup(search.getText().toString().trim()));
        if(query==null||query.trim().isEmpty()){addCard(r,card("Instant lookup","Search by full or partial name. Results come from the member snapshot already loaded for this screen.",BLUE));setContentView(s);return;}
        String q=query.toLowerCase(Locale.US);List<JSONObject> matches=new ArrayList<>();
        for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(m!=null&&m.optString("name","").toLowerCase(Locale.US).contains(q))matches.add(m);}
        if(matches.isEmpty())addCard(r,card("No match","No current faction member matched “"+query+"”.",BAD));
        else for(JSONObject m:matches)addCard(r,memberCard(m));
        setContentView(s);
    }

    private LinearLayout memberCard(JSONObject m){
        String name=m.optString("name","Unknown");String position=m.optString("position","Member");JSONObject status=m.optJSONObject("status"),last=m.optJSONObject("last_action");String state=status==null?"Unknown":status.optString("state","Unknown");String lastState=last==null?"Unknown":last.optString("status","Unknown");String relative=last==null?"":last.optString("relative","");
        String body=position+"\nStatus: "+state+"\nLast action: "+lastState+(relative.isEmpty()?"":" • "+relative)+"\nOC: "+(m.optBoolean("is_in_oc",false)?"Assigned":"Not assigned")+" • Wall: "+(m.optBoolean("is_on_wall",false)?"Yes":"No");
        return card(name,body,"Online".equalsIgnoreCase(lastState)?GOOD:BORDER);
    }
}

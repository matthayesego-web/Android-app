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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

/** Root-only manager for the lightweight verified Torn-ID Developer Console allowlist. */
public class DeveloperAccessActivity extends Activity {
    private static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(15,20,28),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),GREEN=Color.rgb(63,185,80),RED=Color.rgb(248,81,73),PURPLE=Color.rgb(163,113,247);
    private DeveloperSessionStore store;private DeveloperSessionStore.Session session;private JSONArray access=new JSONArray();

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        store=new DeveloperSessionStore(this);session=store.load();
        if(session==null||!session.root()){Toast.makeText(this,"Root developer access required.",Toast.LENGTH_LONG).show();finish();return;}
        showLoading();load();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String v,float s,int c,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextSize(s);t.setTextColor(c);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String v,int stroke){Button b=new Button(this);b.setText(v);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL,stroke,12));return b;}
    private LinearLayout card(int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(rounded(PANEL,stroke,17));return c;}
    private void add(LinearLayout root,LinearLayout card){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);root.addView(card,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(18),r=dp(16),b=dp(28);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private EditText field(String hint,boolean number){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setSingleLine(true);e.setPadding(dp(12),0,dp(12),0);e.setBackground(rounded(BG,BORDER,10));if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER);return e;}

    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);r.addView(text("Developer Access",29,TEXT,true));LinearLayout c=card(GOLD);c.addView(text("Loading authorized Torn IDs…",17,TEXT,true));c.addView(text("Only the permanent Root ID can add or remove Developer Console access.",12,MUTED,false));add(r,c);setContentView(s);s.requestApplyInsets();}

    private void load(){new Thread(()->{try{JSONObject response=DeveloperBackendClient.idAccessList(session.token);JSONArray rows=response.optJSONArray("access");access=rows==null?new JSONArray():rows;runOnUiThread(this::render);}catch(Exception e){String m=e.getMessage()==null?"Unable to load developer access.":e.getMessage();runOnUiThread(()->{Toast.makeText(this,m,Toast.LENGTH_LONG).show();finish();});}},"TornFCA-DeveloperIdAccessLoad").start();}

    private void render(){
        ScrollView s=shell();LinearLayout r=root(s);
        Button back=button("← Developer Panel",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(172),dp(44)));
        TextView title=text("Developer Access",29,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);r.addView(title,tp);
        TextView sub=text("Root-only Torn ID allowlist",12,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.bottomMargin=dp(14);r.addView(sub,sp);

        LinearLayout add=card(PURPLE);add.addView(text("Authorize Torn ID",18,TEXT,true));add.addView(text("Enter a Torn player ID. That verified Torn account can open the hidden Developer Console, but cannot add or remove anyone else.",12,MUTED,false));
        EditText playerId=field("Torn player ID",true),label=field("Name / label (optional)",false);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));ip.topMargin=dp(9);add.addView(playerId,ip);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));lp.topMargin=dp(7);add.addView(label,lp);
        Button authorize=button("Authorize ID",PURPLE);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));ap.topMargin=dp(9);add.addView(authorize,ap);
        authorize.setOnClickListener(v->{long id=parseId(playerId.getText().toString());if(id<=0){Toast.makeText(this,"Enter a valid Torn player ID.",Toast.LENGTH_SHORT).show();return;}String name=label.getText().toString().trim();authorize.setEnabled(false);new Thread(()->{try{DeveloperBackendClient.addIdAccess(session.token,id,name);runOnUiThread(()->{Toast.makeText(this,"Torn ID authorized.",Toast.LENGTH_SHORT).show();load();});}catch(Exception e){String m=e.getMessage()==null?"Unable to authorize Torn ID.":e.getMessage();runOnUiThread(()->{authorize.setEnabled(true);Toast.makeText(this,m,Toast.LENGTH_LONG).show();});}},"TornFCA-DeveloperIdAdd").start();});
        add(r,add);

        TextView section=text("AUTHORIZED TORN IDS",11,MUTED,true);section.setLetterSpacing(.08f);LinearLayout.LayoutParams secp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);secp.topMargin=dp(5);secp.bottomMargin=dp(8);r.addView(section,secp);
        for(int i=0;i<access.length();i++){JSONObject row=access.optJSONObject(i);if(row!=null)add(r,accessCard(row));}

        LinearLayout safety=card(BORDER);safety.addView(text("Access boundary",17,TEXT,true));safety.addView(text("Your Torn ID is permanent Root and cannot be removed. Added IDs receive Developer access only. Removing an ID immediately revokes its active Developer Console sessions. The older password/TOTP developer-account infrastructure remains stored for future hardened mode.",12,MUTED,false));add(r,safety);
        setContentView(s);s.requestApplyInsets();
    }

    private LinearLayout accessCard(JSONObject row){
        long id=row.optLong("player_id",0);String name=row.optString("player_name",id>0?"Player "+id:"Developer");boolean immutable=row.optBoolean("immutable",false);String role=row.optString("role",immutable?"root":"developer");
        LinearLayout c=card(immutable?GOLD:GREEN);c.addView(text(name,18,TEXT,true));c.addView(text("Torn ID "+id+" • "+role.toUpperCase(),11,immutable?GOLD:GREEN,true));
        c.addView(text(immutable?"Permanent TornFCA Root. Only this ID can manage Developer Console access.":"Can open the Developer Console. Cannot add or remove other IDs.",11,MUTED,false));
        if(!immutable){Button remove=button("Remove Access",RED);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));rp.topMargin=dp(9);c.addView(remove,rp);remove.setOnClickListener(v->remove(id,name));}
        return c;
    }

    private void remove(long id,String name){new android.app.AlertDialog.Builder(this).setTitle("Remove "+name+"?").setMessage("This Torn ID will lose Developer Console access and its active developer sessions will be revoked.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->new Thread(()->{try{DeveloperBackendClient.removeIdAccess(session.token,id);runOnUiThread(()->{Toast.makeText(this,"Developer access removed.",Toast.LENGTH_SHORT).show();load();});}catch(Exception e){String m=e.getMessage()==null?"Unable to remove Torn ID.":e.getMessage();runOnUiThread(()->Toast.makeText(this,m,Toast.LENGTH_LONG).show());}},"TornFCA-DeveloperIdRemove").start()).show();}
    private static long parseId(String raw){try{return Long.parseLong(raw.trim());}catch(Exception e){return 0L;}}
}

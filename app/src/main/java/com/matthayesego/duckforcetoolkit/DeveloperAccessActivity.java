package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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

/** Root/Admin manager for delegated Developer Console accounts. */
public class DeveloperAccessActivity extends Activity {
    private static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(15,20,28),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),BLUE=Color.rgb(88,166,255),GREEN=Color.rgb(63,185,80),RED=Color.rgb(248,81,73),PURPLE=Color.rgb(163,113,247);
    private DeveloperSessionStore store;private DeveloperSessionStore.Session session;private JSONArray developers=new JSONArray();private String oneTimeCode="",oneTimeLabel="";
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);store=new DeveloperSessionStore(this);session=store.load();if(session==null||!session.admin()){Toast.makeText(this,"Admin developer session required.",Toast.LENGTH_LONG).show();finish();return;}showLoading();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String v,float s,int c,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextSize(s);t.setTextColor(c);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String v,int stroke){Button b=new Button(this);b.setText(v);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL,stroke,12));return b;}
    private LinearLayout card(int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(rounded(PANEL,stroke,17));return c;}
    private void add(LinearLayout root,LinearLayout card){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);root.addView(card,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(18),r=dp(16),b=dp(28);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setSingleLine(true);e.setPadding(dp(12),0,dp(12),0);e.setBackground(rounded(BG,BORDER,10));return e;}

    private void showLoading(){ScrollView s=shell();LinearLayout r=root(s);r.addView(text("Developer Access",29,TEXT,true));LinearLayout c=card(BLUE);c.addView(text("Loading delegated access…",17,TEXT,true));c.addView(text("Accounts are separate from Torn faction identity and use individual passwords plus authenticator secrets.",12,MUTED,false));add(r,c);setContentView(s);s.requestApplyInsets();}
    private void load(){new Thread(()->{try{JSONObject response=DeveloperBackendClient.accessList(session.token);JSONArray rows=response.optJSONArray("developers");developers=rows==null?new JSONArray():rows;runOnUiThread(this::render);}catch(Exception e){String m=e.getMessage()==null?"Unable to load developer access.":e.getMessage();runOnUiThread(()->{Toast.makeText(this,m,Toast.LENGTH_LONG).show();finish();});}},"TornFCA-DeveloperAccessLoad").start();}

    private void render(){ScrollView s=shell();LinearLayout r=root(s);Button back=button("← Developer Panel",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(172),dp(44)));TextView title=text("Developer Access",29,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);r.addView(title,tp);TextView sub=text("Signed in as "+session.username+" • "+session.role.toUpperCase()+" • individual 2FA credentials",12,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.bottomMargin=dp(14);r.addView(sub,sp);
        if(!oneTimeCode.isBlank()){LinearLayout invite=card(GREEN);invite.addView(text("ONE-TIME ENROLLMENT CODE",11,GREEN,true));invite.addView(text(oneTimeLabel,14,TEXT,true));TextView code=text(oneTimeCode,20,GOLD,true);code.setTextIsSelectable(true);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);cp.topMargin=dp(8);invite.addView(code,cp);invite.addView(text("Send this code privately. It expires after 24 hours and disappears from this screen after refresh.",11,MUTED,false));Button copy=button("Copy Enrollment Code",GREEN);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));bp.topMargin=dp(8);invite.addView(copy,bp);copy.setOnClickListener(v->copy(oneTimeCode));add(r,invite);}
        LinearLayout add=card(PURPLE);add.addView(text("Add Developer",18,TEXT,true));add.addView(text("Create a one-time invitation. The recipient creates their own password and unique authenticator enrollment.",12,MUTED,false));EditText username=field("Login username"),display=field("Display name");LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));fp.topMargin=dp(8);add.addView(username,fp);LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));dpv.topMargin=dp(7);add.addView(display,dpv);Spinner role=new Spinner(this);String[] roles=session.root()?new String[]{"Developer","Admin"}:new String[]{"Developer"};ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,roles);role.setAdapter(adapter);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));rp.topMargin=dp(7);add.addView(role,rp);Button create=button("Create Invitation",PURPLE);LinearLayout.LayoutParams cip=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));cip.topMargin=dp(8);add.addView(create,cip);create.setOnClickListener(v->{String u=username.getText().toString().trim(),d=display.getText().toString().trim(),rr=String.valueOf(role.getSelectedItem()).toLowerCase();if(u.isEmpty()){Toast.makeText(this,"Username required.",Toast.LENGTH_SHORT).show();return;}create.setEnabled(false);new Thread(()->{try{JSONObject response=DeveloperBackendClient.createInvite(session.token,u,d,rr);oneTimeCode=response.optString("invite_code","");oneTimeLabel="Invite for "+response.optString("display_name",u)+" • "+response.optString("role",rr);runOnUiThread(()->{Toast.makeText(this,"Invitation created.",Toast.LENGTH_SHORT).show();render();});}catch(Exception e){String m=e.getMessage()==null?"Unable to create invitation.":e.getMessage();runOnUiThread(()->{create.setEnabled(true);Toast.makeText(this,m,Toast.LENGTH_LONG).show();});}},"TornFCA-DeveloperInvite").start();});add(r,add);

        TextView section=text("AUTHORIZED DEVELOPERS",11,MUTED,true);section.setLetterSpacing(.08f);LinearLayout.LayoutParams secp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);secp.topMargin=dp(5);secp.bottomMargin=dp(8);r.addView(section,secp);
        for(int i=0;i<developers.length();i++){JSONObject dev=developers.optJSONObject(i);if(dev==null)continue;add(r,developerCard(dev));}
        LinearLayout safety=card(BORDER);safety.addView(text("Security boundary",17,TEXT,true));safety.addView(text("Revocation invalidates active developer sessions. Root Admin cannot be revoked here. These accounts grant the hidden Developer Console only; Torn faction, banking, moderation and Premium authority remain separately verified by their own services.",12,MUTED,false));add(r,safety);setContentView(s);s.requestApplyInsets();}

    private LinearLayout developerCard(JSONObject dev){
        String id=dev.optString("id","");
        String username=dev.optString("username","Developer");
        String name=dev.optString("display_name",username);
        String role=dev.optString("role","developer");
        boolean active=dev.optBoolean("active",false);
        LinearLayout c=card("root".equals(role)?GOLD:active?GREEN:BORDER);
        c.addView(text(name,18,TEXT,true));
        c.addView(text("@"+username+" • "+role.toUpperCase()+" • "+(active?"ACTIVE":"DISABLED / ENROLLMENT PENDING"),11,active?GREEN:MUTED,true));
        long last=dev.optLong("last_login",0);c.addView(text(last>0?"Last login recorded by backend.":"No successful login recorded yet.",11,MUTED,false));
        if(!"root".equals(role)){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rowp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));rowp.topMargin=dp(9);c.addView(row,rowp);Button reset=button("Reset / Re-enroll",PURPLE),revoke=button("Revoke",RED);row.addView(reset,new LinearLayout.LayoutParams(0,dp(44),1f));LinearLayout.LayoutParams rv=new LinearLayout.LayoutParams(0,dp(44),1f);rv.leftMargin=dp(6);row.addView(revoke,rv);reset.setOnClickListener(v->reset(id,name));revoke.setOnClickListener(v->revoke(id,name));}
        return c;
    }
    private void reset(String id,String name){new Thread(()->{try{JSONObject response=DeveloperBackendClient.resetEnrollment(session.token,id);oneTimeCode=response.optString("invite_code","");oneTimeLabel="Re-enrollment for "+name;runOnUiThread(()->{Toast.makeText(this,"Old sessions revoked. New enrollment created.",Toast.LENGTH_LONG).show();load();});}catch(Exception e){String m=e.getMessage()==null?"Unable to reset enrollment.":e.getMessage();runOnUiThread(()->Toast.makeText(this,m,Toast.LENGTH_LONG).show());}},"TornFCA-DeveloperReset").start();}
    private void revoke(String id,String name){new android.app.AlertDialog.Builder(this).setTitle("Revoke "+name+"?").setMessage("This immediately invalidates their developer sessions. They cannot sign in again unless you issue a new enrollment.").setNegativeButton("Cancel",null).setPositiveButton("Revoke",(d,w)->new Thread(()->{try{DeveloperBackendClient.revokeAccess(session.token,id);runOnUiThread(()->{Toast.makeText(this,"Developer access revoked.",Toast.LENGTH_SHORT).show();load();});}catch(Exception e){String m=e.getMessage()==null?"Unable to revoke access.":e.getMessage();runOnUiThread(()->Toast.makeText(this,m,Toast.LENGTH_LONG).show());}},"TornFCA-DeveloperRevoke").start()).show();}
    private void copy(String value){ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);if(cm!=null)cm.setPrimaryClip(ClipData.newPlainText("TornFCA developer enrollment",value));Toast.makeText(this,"Enrollment code copied.",Toast.LENGTH_SHORT).show();}
}

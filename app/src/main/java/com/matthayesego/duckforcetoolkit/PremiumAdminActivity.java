package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Hidden developer-only controls for the global premium entitlement backend. */
public class PremiumAdminActivity extends Activity {
    private static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(15,20,28),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),BLUE=Color.rgb(88,166,255),GREEN=Color.rgb(63,185,80),RED=Color.rgb(248,81,73);
    private SecureApiKeyStore keyStore;
    @Override protected void onCreate(Bundle b){super.onCreate(b);keyStore=new SecureApiKeyStore(this);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);render();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String v,float s,int c,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextSize(s);t.setTextColor(c);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String v,int stroke){Button b=new Button(this);b.setText(v);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL,stroke,12));return b;}
    private EditText field(String hint,boolean password,boolean number){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setSingleLine(true);e.setPadding(dp(13),0,dp(13),0);e.setBackground(rounded(BG,BORDER,11));if(password)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);else if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER);return e;}
    private LinearLayout card(int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(17),dp(16),dp(17),dp(16));c.setBackground(rounded(PANEL,stroke,17));return c;}
    private void add(LinearLayout root,LinearLayout card){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);root.addView(card,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(20),r=dp(16),bt=dp(28);s.setPadding(l,t,r,bt);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),bt+i.getSystemWindowInsetBottom());return i;});return s;}

    private void render(){
        ScrollView s=shell();LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);s.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        Button back=button("← Developer Panel",BORDER);back.setOnClickListener(v->finish());root.addView(back,new LinearLayout.LayoutParams(dp(170),dp(44)));
        TextView eye=text("TORNFCA • PREMIUM ADMIN",10,GOLD,true);eye.setLetterSpacing(.12f);eye.setGravity(Gravity.CENTER);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ep.topMargin=dp(18);root.addView(eye,ep);TextView title=text("Premium Controls",29,TEXT,true);title.setGravity(Gravity.CENTER);root.addView(title);TextView state=text(PremiumBackendClient.isConfigured()?"Global entitlement backend connected":"Premium backend URL is not configured in this build",12,PremiumBackendClient.isConfigured()?GREEN:RED,true);state.setGravity(Gravity.CENTER);LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);stp.topMargin=dp(5);stp.bottomMargin=dp(15);root.addView(state,stp);

        LinearLayout auth=card(GOLD);auth.addView(text("Developer authorization",18,TEXT,true));auth.addView(text("Server changes require both the verified TornFCA owner account and the developer password. The API key is used only to verify the request and is not stored by the backend.",12,MUTED,false));EditText password=field("Developer password",true,false);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));pp.topMargin=dp(10);auth.addView(password,pp);add(root,auth);

        LinearLayout pricing=card(BLUE);pricing.addView(text("Xanax conversion",18,TEXT,true));pricing.addView(text("Launch pricing is 1 Xanax = 7 Premium days. Incoming Xanax is detected from the owner's official Torn item-receive log; duplicate logs can never grant twice.",12,MUTED,false));EditText days=field("Days of Premium per Xanax",false,true);days.setText("7");LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));dpv.topMargin=dp(10);pricing.addView(days,dpv);EditText message=field("Required transfer message (blank = any Xanax)",false,false);message.setText("TORNFCA");LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));mp.topMargin=dp(8);pricing.addView(message,mp);Button save=button("Update Premium Conversion",BLUE);LinearLayout.LayoutParams svp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));svp.topMargin=dp(9);pricing.addView(save,svp);save.setOnClickListener(v->{int d=parseInt(days.getText().toString(),7);runAdmin((key)->PremiumBackendClient.updateConfig(key,password.getText().toString(),d,message.getText().toString()),"Premium conversion updated.");});add(root,pricing);

        LinearLayout grant=card(GREEN);grant.addView(text("Complimentary Premium",18,TEXT,true));grant.addView(text("Gift Premium to any Torn player without requiring a Xanax payment. This owner-only action uses the same server-verified entitlement as paid Premium, so it follows the player's Torn ID across factions and expires normally.",12,MUTED,false));EditText player=field("Torn player ID",false,true);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));gp.topMargin=dp(10);grant.addView(player,gp);EditText grantDays=field("Complimentary days to grant",false,true);grantDays.setText("30");LinearLayout.LayoutParams gdp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));gdp.topMargin=dp(8);grant.addView(grantDays,gdp);Button give=button("Grant Complimentary Premium",GREEN);LinearLayout.LayoutParams gbp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));gbp.topMargin=dp(9);grant.addView(give,gbp);give.setOnClickListener(v->{int id=parseInt(player.getText().toString(),0),d=parseInt(grantDays.getText().toString(),0);if(id<=0||d<=0){Toast.makeText(this,"Enter a valid player ID and number of days.",Toast.LENGTH_SHORT).show();return;}runAdmin((key)->PremiumBackendClient.grantComplimentary(key,password.getText().toString(),id,d),"Complimentary Premium granted.");});add(root,grant);

        LinearLayout note=card(BORDER);note.addView(text("Activation paths",17,TEXT,true));note.addView(text("Paid: player transfers Xanax in Torn → owner log 4103 appears → backend scanner validates item #206 and sender → entitlement expiry is extended.\n\nComplimentary: owner enters a Torn player ID and number of days above → the backend extends the same entitlement without a payment.\n\nTornFCA refreshes only the verified signed-in player's entitlement cache; no client can query another player's entitlement.",12,MUTED,false));add(root,note);
        setContentView(s);s.requestApplyInsets();
    }

    private interface AdminCall{Object run(String apiKey)throws Exception;}
    private void runAdmin(AdminCall call,String success){if(!PremiumBackendClient.isConfigured()){Toast.makeText(this,"Premium backend is not configured in this build.",Toast.LENGTH_LONG).show();return;}String apiKey=keyStore.load();if(apiKey==null||apiKey.trim().isEmpty()){Toast.makeText(this,"Signed-in Torn API key is required.",Toast.LENGTH_LONG).show();return;}new Thread(()->{try{call.run(apiKey);runOnUiThread(()->Toast.makeText(this,success,Toast.LENGTH_LONG).show());}catch(Exception e){String m=e.getMessage()==null?"Premium admin action failed.":e.getMessage();runOnUiThread(()->Toast.makeText(this,m,Toast.LENGTH_LONG).show());}}).start();}
    private static int parseInt(String raw,int fallback){try{return Integer.parseInt(raw.trim());}catch(Exception e){return fallback;}}
}

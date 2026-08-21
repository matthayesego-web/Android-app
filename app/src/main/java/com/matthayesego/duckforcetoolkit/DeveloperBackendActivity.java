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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/** Owner-only UI for TornFCA's remote maintenance/version/feature policy and aggregate user telemetry. */
public class DeveloperBackendActivity extends Activity {
    private static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(15,20,28),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),BLUE=Color.rgb(88,166,255),GREEN=Color.rgb(63,185,80),RED=Color.rgb(248,81,73);
    private SecureApiKeyStore keyStore;
    private JSONObject config=new JSONObject();
    private JSONObject userStats=new JSONObject();

    @Override protected void onCreate(Bundle b){super.onCreate(b);keyStore=new SecureApiKeyStore(this);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);showLoading();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String v,float s,int c,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextSize(s);t.setTextColor(c);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String v,int stroke){Button b=new Button(this);b.setText(v);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL,stroke,12));return b;}
    private EditText field(String hint,boolean password,boolean number){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setSingleLine(!"Message shown during maintenance/disable".equals(hint));e.setPadding(dp(13),dp(8),dp(13),dp(8));e.setBackground(rounded(BG,BORDER,11));if(password)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);else if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER);return e;}
    private LinearLayout card(int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(17),dp(16),dp(17),dp(16));c.setBackground(rounded(PANEL,stroke,17));return c;}
    private void add(LinearLayout root,LinearLayout card){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);root.addView(card,p);}
    private CheckBox check(String label,boolean checked){CheckBox c=new CheckBox(this);c.setText(label);c.setTextColor(TEXT);c.setChecked(checked);return c;}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(20),r=dp(16),bt=dp(28);s.setPadding(l,t,r,bt);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),bt+i.getSystemWindowInsetBottom());return i;});return s;}

    private LinearLayout base(ScrollView s){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);s.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));Button back=button("← Developer Panel",BORDER);back.setOnClickListener(v->finish());root.addView(back,new LinearLayout.LayoutParams(dp(170),dp(44)));TextView eye=text("TORNFCA • CONTROL PLANE",10,GOLD,true);eye.setLetterSpacing(.12f);eye.setGravity(Gravity.CENTER);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ep.topMargin=dp(18);root.addView(eye,ep);TextView title=text("Backend Control",29,TEXT,true);title.setGravity(Gravity.CENTER);root.addView(title);return root;}

    private void showLoading(){ScrollView s=shell();LinearLayout root=base(s);TextView state=text(DeveloperBackendClient.isConfigured()?"Verifying owner access…":"Developer backend URL is not configured in this build.",13,DeveloperBackendClient.isConfigured()?GOLD:RED,true);state.setGravity(Gravity.CENTER);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(10);root.addView(state,p);setContentView(s);s.requestApplyInsets();}

    private void load(){if(!DeveloperBackendClient.isConfigured())return;String key=keyStore.load();if(key==null||key.trim().isEmpty()){Toast.makeText(this,"Signed-in Torn API key is required.",Toast.LENGTH_LONG).show();return;}new Thread(()->{try{JSONObject response=DeveloperBackendClient.readConfig(key);JSONObject c=response.optJSONObject("config");JSONObject stats=response.optJSONObject("user_stats");config=c==null?new JSONObject():c;userStats=stats==null?new JSONObject():stats;RemoteFeaturePolicy.applyVerifiedConfig(this,config);runOnUiThread(this::render);}catch(Exception e){String m=e.getMessage()==null?"Unable to load developer backend.":e.getMessage();runOnUiThread(()->{Toast.makeText(this,m,Toast.LENGTH_LONG).show();showLoading();});}},"TornFCA-DeveloperBackendLoad").start();}

    private void render(){ScrollView s=shell();LinearLayout root=base(s);TextView status=text("Verified remote policy • changes are server-side and audited",12,GREEN,true);status.setGravity(Gravity.CENTER);LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);st.topMargin=dp(7);st.bottomMargin=dp(14);root.addView(status,st);

        LinearLayout users=card(BLUE);users.addView(text("User tracker",18,TEXT,true));users.addView(text("Aggregate verified TornFCA usage. Current = users seen within the last 24 hours.",12,MUTED,false));LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rp.topMargin=dp(12);users.addView(row,rp);row.addView(statBlock("Current total",String.valueOf(userStats.optInt("current_total",0)),"24h active"),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));row.addView(statBlock("Unique users",String.valueOf(userStats.optInt("total_unique",0)),"All-time verified"),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));add(root,users);

        LinearLayout auth=card(GOLD);auth.addView(text("Authorization",18,TEXT,true));auth.addView(text("Saving requires the verified TornFCA owner account plus the developer password. The password and API key are not stored by this screen.",12,MUTED,false));EditText password=field("Developer password",true,false);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));ap.topMargin=dp(10);auth.addView(password,ap);add(root,auth);

        LinearLayout global=card(RED);global.addView(text("Global policy",18,TEXT,true));CheckBox maintenance=check("Maintenance mode",config.optBoolean("maintenance_mode",false));global.addView(maintenance);EditText minimum=field("Minimum supported versionCode",false,true);minimum.setText(String.valueOf(config.optInt("minimum_version_code",0)));LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));mp.topMargin=dp(8);global.addView(minimum,mp);EditText message=field("Message shown during maintenance/disable",false,false);message.setText(config.optString("beta_message",""));message.setMinLines(2);message.setMaxLines(4);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(86));bp.topMargin=dp(8);global.addView(message,bp);add(root,global);

        LinearLayout features=card(BLUE);features.addView(text("Remote feature switches",18,TEXT,true));features.addView(text("These are emergency/server policy switches. They are separate from local Developer Console simulations.",12,MUTED,false));CheckBox activity=check("Disable Activity",config.optBoolean("disable_activity",false));CheckBox war=check("Disable War / WarPay",config.optBoolean("disable_war",false));CheckBox chain=check("Disable Chain",config.optBoolean("disable_chain",false));CheckBox oc=check("Disable OC",config.optBoolean("disable_oc",false));CheckBox pulse=check("Disable Pulse",config.optBoolean("disable_pulse",false));CheckBox lookup=check("Disable Lookup",config.optBoolean("disable_lookup",false));CheckBox premium=check("Disable Premium",config.optBoolean("disable_premium",false));features.addView(activity);features.addView(war);features.addView(chain);features.addView(oc);features.addView(pulse);features.addView(lookup);features.addView(premium);add(root,features);

        Button save=button("Save Remote Policy",GREEN);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52));sp.bottomMargin=dp(14);root.addView(save,sp);save.setOnClickListener(v->{JSONObject next=new JSONObject();try{next.put("maintenance_mode",maintenance.isChecked());next.put("minimum_version_code",parseInt(minimum.getText().toString()));next.put("beta_message",message.getText().toString());next.put("disable_activity",activity.isChecked());next.put("disable_war",war.isChecked());next.put("disable_chain",chain.isChecked());next.put("disable_oc",oc.isChecked());next.put("disable_pulse",pulse.isChecked());next.put("disable_lookup",lookup.isChecked());next.put("disable_premium",premium.isChecked());}catch(Exception ignored){}save(password.getText().toString(),next);});
        LinearLayout note=card(BORDER);note.addView(text("Safety boundary",17,TEXT,true));note.addView(text("Remote switches affect normal feature routing only. The developer route remains available for recovery, and network failure uses the last verified policy rather than blocking startup.",12,MUTED,false));add(root,note);
        setContentView(s);s.requestApplyInsets();
    }

    private LinearLayout statBlock(String label,String value,String detail){LinearLayout block=new LinearLayout(this);block.setOrientation(LinearLayout.VERTICAL);block.setPadding(dp(4),dp(4),dp(4),dp(4));TextView v=text(value,28,TEXT,true);v.setGravity(Gravity.CENTER);block.addView(v);TextView l=text(label,12,GOLD,true);l.setGravity(Gravity.CENTER);block.addView(l);TextView d=text(detail,10,MUTED,false);d.setGravity(Gravity.CENTER);block.addView(d);return block;}

    private void save(String password,JSONObject next){
        if(password==null||password.isEmpty()){Toast.makeText(this,"Developer password required.",Toast.LENGTH_SHORT).show();return;}
        String key=keyStore.load();
        new Thread(()->{try{JSONObject response=DeveloperBackendClient.writeConfig(key,password,next);JSONObject saved=response.optJSONObject("config");JSONObject stats=response.optJSONObject("user_stats");if(saved==null)saved=next;if(stats!=null)userStats=stats;config=saved;RemoteFeaturePolicy.applyVerifiedConfig(this,saved);runOnUiThread(()->{Toast.makeText(this,"Remote policy saved.",Toast.LENGTH_LONG).show();render();});}catch(Exception e){String m=e.getMessage()==null?"Unable to save remote policy.":e.getMessage();runOnUiThread(()->Toast.makeText(this,m,Toast.LENGTH_LONG).show());}},"TornFCA-DeveloperBackendSave").start();
    }
    private static int parseInt(String raw){try{return Math.max(0,Integer.parseInt(raw.trim()));}catch(Exception e){return 0;}}
}

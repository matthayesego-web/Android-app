package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Hidden triple-tap developer entry. Password is the authorization boundary; Torn identity is only context. */
public class DeveloperGateActivity extends Activity {
    private static final String DEVELOPER_ACCESS_SHA256="AD039B0643FE2CD75558E56B90955252ED3F56CE6B2B7AA90CD1ED3BC22AC6AB";
    private static final int BG=Color.rgb(6,9,13),PANEL=Color.rgb(15,20,28),BORDER=Color.rgb(45,55,69),TEXT=Color.rgb(244,246,249),MUTED=Color.rgb(154,164,178),GOLD=Color.rgb(241,194,106),BAD=Color.rgb(248,81,73),BLUE=Color.rgb(88,166,255);
    private SecureApiKeyStore keyStore;

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);keyStore=new SecureApiKeyStore(this);renderPassword(null);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(18),t=dp(22),r=dp(18),b=dp(28);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}

    private void renderPassword(String error){ScrollView s=shell();LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setGravity(Gravity.CENTER_HORIZONTAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));TextView brand=text("DUCK FORCE • HIDDEN CHANNEL",11,GOLD,true);brand.setLetterSpacing(.14f);brand.setGravity(Gravity.CENTER);r.addView(brand);TextView title=text("Developer Console",30,TEXT,true);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(7);tp.bottomMargin=dp(18);r.addView(title,tp);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(18),dp(18),dp(18));card.setBackground(rounded(PANEL,BLUE,18));card.addView(text("Developer password",19,TEXT,true));TextView body=text("This console is intentionally absent from normal navigation. Anyone entrusted with the developer password can enter it from the hidden triple-tap gesture.",13,MUTED,false);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(6);card.addView(body,bp);EditText pass=new EditText(this);pass.setHint("Developer password");pass.setHintTextColor(MUTED);pass.setTextColor(TEXT);pass.setSingleLine(true);pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);pass.setPadding(dp(14),0,dp(14),0);pass.setBackground(rounded(BG,BORDER,12));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54));pp.topMargin=dp(15);card.addView(pass,pp);Button unlock=new Button(this);unlock.setText("Open Developer Panel");unlock.setAllCaps(false);unlock.setTextColor(Color.rgb(24,17,8));unlock.setTextSize(15);unlock.setTypeface(Typeface.DEFAULT,Typeface.BOLD);unlock.setBackground(rounded(GOLD,GOLD,12));LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));up.topMargin=dp(11);card.addView(unlock,up);TextView status=text(error==null?"Triple tap footer + password":error,12,error==null?MUTED:BAD,false);status.setGravity(Gravity.CENTER);LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);st.topMargin=dp(9);card.addView(status,st);r.addView(card,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));unlock.setOnClickListener(v->{String entered=pass.getText().toString();if(!DEVELOPER_ACCESS_SHA256.equals(sha256(entered))){status.setText("Incorrect developer password.");status.setTextColor(BAD);pass.setText("");return;}resolveContextAndOpen();});setContentView(s);s.requestApplyInsets();}

    private void resolveContextAndOpen(){String key=keyStore.load();if(key==null||key.trim().isEmpty()){openPanel(null);return;}FactionScopeCache.Scope cached=FactionScopeCache.load(this,key);if(cached!=null){Intent i=new Intent(this,DeveloperPanelActivity.class);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_ID,cached.factionId);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_NAME,cached.factionName);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_API,cached.factionApiAccess);i.putExtra(DeveloperConsoleActivity.EXTRA_POSITION,cached.position);startActivity(i);finish();return;}new Thread(()->{AuthSession session=null;try{session=TornApiClient.authenticate(key);FactionScopeCache.save(this,key,session);}catch(Exception ignored){}AuthSession finalSession=session;runOnUiThread(()->openPanel(finalSession));}).start();}
    private void openPanel(AuthSession session){Intent i=new Intent(this,DeveloperPanelActivity.class);if(session!=null){i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_ID,session.factionId);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_NAME,session.factionName);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_API,session.factionApiAccess);i.putExtra(DeveloperConsoleActivity.EXTRA_POSITION,session.position);}startActivity(i);finish();}
    private static String sha256(String value){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] digest=md.digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:digest)b.append(String.format("%02X",x));return b.toString();}catch(Exception e){return"";}}
}

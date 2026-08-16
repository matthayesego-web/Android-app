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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Pre-release access gate. The plaintext preview code is never stored in source control. */
public class AccessGateActivity extends Activity {
    private static final String PREVIEW_ACCESS_SHA256 = "A09FECAF99100B9EB3BC4F06A6023D9CBDC29AC667990348F5326327396BA962";
    private static final int BG=Color.rgb(5,9,14), PANEL=Color.rgb(13,20,29), BORDER=Color.rgb(44,58,74), TEXT=Color.rgb(244,247,251), MUTED=Color.rgb(150,163,181), ACCENT=Color.rgb(103,216,243), GOLD=Color.rgb(242,197,107), BAD=Color.rgb(248,81,73);
    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);render(null);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int start,int end,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(18),t=dp(24),r=dp(18),b=dp(30);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private void render(String error){
        ScrollView scroll=shell();LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER_HORIZONTAL);hero.setPadding(dp(20),dp(24),dp(20),dp(24));hero.setBackground(gradient(Color.rgb(18,39,51),Color.rgb(9,15,22),BORDER,26));
        ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.tornfca_mark);mark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);mark.setContentDescription("TornFCA");hero.addView(mark,new LinearLayout.LayoutParams(dp(124),dp(124)));
        TextView brand=text("TORNFCA",13,ACCENT,true);brand.setLetterSpacing(.22f);brand.setGravity(Gravity.CENTER);hero.addView(brand);
        TextView title=text("Faction Companion",31,TEXT,true);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(5);hero.addView(title,tp);
        TextView sub=text("Your faction. Your command layer.",13,MUTED,false);sub.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(6);hero.addView(sub,sp);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.bottomMargin=dp(14);root.addView(hero,hp);

        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(18),dp(18),dp(18));card.setBackground(rounded(PANEL,ACCENT,19));card.addView(text("Enter beta access code",19,TEXT,true));
        TextView info=text("Unlock TornFCA first. Your Torn API key then verifies your faction, identity and real permissions. Once verified, TornFCA adapts its accent theme to your faction.",13,MUTED,false);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ip.topMargin=dp(7);card.addView(info,ip);
        EditText code=new EditText(this);code.setHint("Access code");code.setHintTextColor(MUTED);code.setTextColor(TEXT);code.setSingleLine(true);code.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);code.setPadding(dp(14),0,dp(14),0);code.setBackground(rounded(BG,BORDER,12));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54));cp.topMargin=dp(16);card.addView(code,cp);
        Button unlock=new Button(this);unlock.setText("Unlock TornFCA Beta");unlock.setAllCaps(false);unlock.setTextColor(Color.rgb(5,16,21));unlock.setTextSize(15);unlock.setTypeface(Typeface.DEFAULT,Typeface.BOLD);unlock.setBackground(rounded(ACCENT,ACCENT,12));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));bp.topMargin=dp(12);card.addView(unlock,bp);
        TextView status=text(error==null?"v0.9.9 beta • private pre-release":error,12,error==null?MUTED:BAD,false);status.setGravity(Gravity.CENTER);LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);stp.topMargin=dp(10);card.addView(status,stp);
        TextView note=text("TornFCA is an independent community faction companion. Faction-specific styling appears after authentication.",11,MUTED,false);note.setGravity(Gravity.CENTER);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);np.topMargin=dp(12);card.addView(note,np);
        root.addView(card,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        unlock.setOnClickListener(v->{String entered=code.getText().toString();if(entered.trim().isEmpty()){status.setText("Enter the beta access code.");status.setTextColor(BAD);return;}if(!PREVIEW_ACCESS_SHA256.equals(sha256(entered))){status.setText("Incorrect access code.");status.setTextColor(BAD);code.setText("");return;}Intent i=new Intent(this,TornFcaActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();});
        setContentView(scroll);scroll.requestApplyInsets();
    }
    private static String sha256(String value){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] digest=md.digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:digest)b.append(String.format("%02X",x));return b.toString();}catch(Exception e){return"";}}
}

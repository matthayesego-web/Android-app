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

/** Pre-release access gate. The plaintext preview code is never stored in source control. */
public class AccessGateActivity extends Activity {
    private static final String PREVIEW_ACCESS_SHA256 = "A09FECAF99100B9EB3BC4F06A6023D9CBDC29AC667990348F5326327396BA962";
    private static final int BG=Color.rgb(6,9,13), PANEL=Color.rgb(15,20,28), BORDER=Color.rgb(45,55,69), TEXT=Color.rgb(244,246,249), MUTED=Color.rgb(154,164,178), GOLD=Color.rgb(241,194,106), BAD=Color.rgb(248,81,73);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        render(null);
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}

    private void render(String error){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);scroll.setPadding(dp(18),dp(28),dp(18),dp(28));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView brand=text("DUCK FORCE",13,GOLD,true);brand.setLetterSpacing(.22f);brand.setGravity(Gravity.CENTER);root.addView(brand);
        TextView title=text("Companion Preview",30,TEXT,true);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(8);root.addView(title,tp);
        TextView sub=text("Private pre-release access",14,MUTED,false);sub.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(6);sp.bottomMargin=dp(20);root.addView(sub,sp);

        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(18),dp(18),dp(18));card.setBackground(rounded(PANEL,BORDER,18));
        card.addView(text("Enter preview access code",19,TEXT,true));
        TextView info=text("This preview is restricted before Torn sign-in. After this code is accepted, your Torn API key still verifies Duck Force membership and permissions.",13,MUTED,false);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ip.topMargin=dp(6);card.addView(info,ip);

        EditText code=new EditText(this);code.setHint("Access code");code.setHintTextColor(MUTED);code.setTextColor(TEXT);code.setSingleLine(true);code.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);code.setPadding(dp(14),0,dp(14),0);code.setBackground(rounded(BG,BORDER,12));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54));cp.topMargin=dp(16);card.addView(code,cp);
        Button unlock=new Button(this);unlock.setText("Unlock Preview");unlock.setAllCaps(false);unlock.setTextColor(Color.rgb(24,17,8));unlock.setTextSize(15);unlock.setTypeface(Typeface.DEFAULT,Typeface.BOLD);unlock.setBackground(rounded(GOLD,GOLD,12));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));bp.topMargin=dp(12);card.addView(unlock,bp);
        TextView status=text(error==null?"v0.9.0 pre-release • access required every launch":error,12,error==null?MUTED:BAD,false);status.setGravity(Gravity.CENTER);LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);stp.topMargin=dp(10);card.addView(status,stp);
        root.addView(card,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        unlock.setOnClickListener(v->{String entered=code.getText().toString();if(entered.trim().isEmpty()){status.setText("Enter the preview access code.");status.setTextColor(BAD);return;}if(!PREVIEW_ACCESS_SHA256.equals(sha256(entered))){status.setText("Incorrect access code.");status.setTextColor(BAD);code.setText("");return;}Intent i=new Intent(this,V090CompanionActivity.class);startActivity(i);finish();});
        setContentView(scroll);
    }

    private static String sha256(String value){
        try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] digest=md.digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:digest)b.append(String.format("%02X",x));return b.toString();}catch(Exception e){return "";}
    }
}

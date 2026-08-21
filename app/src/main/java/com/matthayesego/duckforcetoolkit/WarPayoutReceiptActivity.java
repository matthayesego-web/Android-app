package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

/** Leadership-only, app-private WarPay receipt viewer. */
public class WarPayoutReceiptActivity extends Activity {
    public static final String EXTRA_WAR_ID = "war_id";
    public static final String EXTRA_POSITION = "position";

    private static final int BG=Color.rgb(5,8,12),PANEL=Color.rgb(12,18,26),PANEL2=Color.rgb(8,13,20),BORDER=Color.rgb(36,47,61),TEXT=Color.rgb(246,248,251),MUTED=Color.rgb(145,155,169),GOLD=Color.rgb(241,190,86),BLUE=Color.rgb(82,153,235),GREEN=Color.rgb(76,190,102),RED=Color.rgb(239,88,82);
    private int warId;
    private String position;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        warId=getIntent().getIntExtra(EXTRA_WAR_ID,0);position=getIntent().getStringExtra(EXTRA_POSITION);if(position==null)position="Member";
        if(!AccessPolicy.isLeaderPosition(position)){renderError("WarPay receipts are restricted to faction leadership.");return;}
        JSONObject receipt=WarPayoutReceiptStore.load(this,warId);if(receipt==null){renderError("No saved receipt exists on this device for war #"+warId+".");return;}render(receipt);
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String value,int accent){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,accent,13));return b;}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(18),t=dp(16),r=dp(18),bt=dp(28);s.setPadding(l,t,r,bt);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),bt+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private LinearLayout card(int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(17),dp(15),dp(17),dp(15));c.setBackground(rounded(PANEL,stroke,18));return c;}
    private void add(LinearLayout r,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);r.addView(c,p);}

    private void render(JSONObject receipt){
        ScrollView s=shell();LinearLayout r=root(s);Button back=button("← WarPay",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(132),dp(44)));
        TextView brand=text("LEADERSHIP • WARPAY RECEIPT",10,GOLD,true);brand.setLetterSpacing(.12f);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(16);r.addView(brand,bp);r.addView(text("War #"+receipt.optInt("war_id",warId)+" Receipt",29,TEXT,true));
        long created=receipt.optLong("created_at",0);String when=created>0?DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(created)):"Unknown time";TextView sub=text("Saved locally on this device • "+when,12.5f,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(5);sp.bottomMargin=dp(14);r.addView(sub,sp);
        LinearLayout summary=card(GREEN);summary.addView(text(money(receipt.optLong("total_paid",0))+" total member payout",21,TEXT,true));summary.addView(text("Pool "+money(receipt.optLong("pool",0))+" • penalties retained "+money(receipt.optLong("total_penalty",0))+" • "+receipt.optInt("member_count",0)+" members",12.5f,MUTED,false));add(r,summary);
        JSONArray rows=receipt.optJSONArray("rows");for(int i=0;rows!=null&&i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;LinearLayout c=card(Color.TRANSPARENT);c.addView(text((i+1)+". "+row.optString("name","Member")+" ["+row.optInt("player_id",0)+"]",16.5f,TEXT,true));String detail=money(row.optLong("net",0))+" net • gross "+money(row.optLong("gross",0))+" • "+row.optInt("war_hits",0)+" war hits • "+row.optInt("outside_hits",0)+" outside • "+String.format(Locale.US,"%.2f",row.optDouble("respect",0d))+" respect";long penalty=row.optLong("penalty",0);if(penalty>0)detail+="\nPenalty: -"+money(penalty)+(row.optString("reason","").isBlank()?"":" • "+row.optString("reason",""));c.addView(text(detail,12.5f,penalty>0?GOLD:MUTED,false));add(r,c);}
        Button copy=button("Copy Full Receipt",GREEN);copy.setOnClickListener(v->copy(WarPayoutReceiptStore.text(receipt)));r.addView(copy,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48)));
        Button share=button("Share Receipt",BLUE);LinearLayout.LayoutParams shp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));shp.topMargin=dp(8);r.addView(share,shp);share.setOnClickListener(v->share(WarPayoutReceiptStore.text(receipt)));
        TextView foot=text("Receipt data is app-private and device-local until the shared faction backend is deployed.",10.5f,MUTED,false);foot.setGravity(Gravity.CENTER);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=dp(12);r.addView(foot,fp);setContentView(s);s.requestApplyInsets();
    }

    private void copy(String value){ClipboardManager c=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);if(c!=null){c.setPrimaryClip(ClipData.newPlainText("WarPay Receipt",value));Toast.makeText(this,"Receipt copied.",Toast.LENGTH_SHORT).show();}}
    private void share(String value){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,value);startActivity(Intent.createChooser(i,"Share WarPay receipt"));}
    private static String money(long value){return "$"+NumberFormat.getIntegerInstance(Locale.US).format(value);}
    private void renderError(String message){ScrollView s=shell();LinearLayout r=root(s);Button back=button("← Back",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(120),dp(44)));LinearLayout c=card(RED);c.addView(text("Receipt unavailable",20,TEXT,true));c.addView(text(message,13,MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(14);r.addView(c,p);setContentView(s);s.requestApplyInsets();}
}

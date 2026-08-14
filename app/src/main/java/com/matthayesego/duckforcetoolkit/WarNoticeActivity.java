package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

public class WarNoticeActivity extends Activity {
    public static final String EXTRA_FACTION_ID = "faction_id";
    public static final String EXTRA_FACTION_NAME = "faction_name";
    public static final String EXTRA_CAN_PUBLISH = "can_publish";

    private static final int BG = Color.rgb(8, 12, 18), PANEL = Color.rgb(20, 27, 38), PANEL2 = Color.rgb(27, 36, 49);
    private static final int BORDER = Color.rgb(49, 63, 81), ACCENT = Color.rgb(243, 184, 52), TEXT = Color.rgb(245, 248, 252);
    private static final int MUTED = Color.rgb(151, 163, 179), GOOD = Color.rgb(63, 185, 80), BLUE = Color.rgb(88, 166, 255);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SecureApiKeyStore keyStore;
    private int factionId;
    private boolean canPublish;
    private TextView warHeadline, warDetail;
    private LinearLayout notices;
    private WarStatus currentWar = WarStatus.none();

    private final Runnable ticker = new Runnable() {
        @Override public void run() { renderWarText(); handler.postDelayed(this, 1000); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        keyStore = new SecureApiKeyStore(this);
        factionId = getIntent().getIntExtra(EXTRA_FACTION_ID, 0);
        canPublish = getIntent().getBooleanExtra(EXTRA_CAN_PUBLISH, false);
        buildUi(); refreshAll(); handler.post(ticker);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable rounded(int fill, int stroke, int radius) { GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d; }
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout card(int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(rounded(PANEL,stroke,17));return c;}
    private Button button(String label,boolean primary){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(primary?Color.rgb(23,17,7):TEXT);b.setBackground(primary?rounded(ACCENT,ACCENT,11):rounded(PANEL2,BORDER,11));return b;}

    @SuppressWarnings("deprecation")
    private void buildUi() {
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        int l=dp(16),t=dp(16),r=dp(16),b=dp(28);scroll.setPadding(l,t,r,b);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(l+insets.getSystemWindowInsetLeft(),t+insets.getSystemWindowInsetTop(),r+insets.getSystemWindowInsetRight(),b+insets.getSystemWindowInsetBottom());return insets;});
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        Button back=button("← Companion",false);back.setOnClickListener(v->finish());root.addView(back,new LinearLayout.LayoutParams(dp(122),dp(44)));
        TextView title=text("War & Leadership Notices",27,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);tp.bottomMargin=dp(12);root.addView(title,tp);

        LinearLayout war=card(ACCENT);warHeadline=text("Checking ranked war…",21,TEXT,true);warDetail=text("",13,MUTED,false);war.addView(warHeadline);LinearLayout.LayoutParams wdp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);wdp.topMargin=dp(6);war.addView(warDetail,wdp);Button refresh=button("Refresh War / Notices",false);refresh.setOnClickListener(v->refreshAll());LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));rp.topMargin=dp(12);war.addView(refresh,rp);root.addView(war);

        TextView ntitle=text("LEADERSHIP MESSAGE BOARD",12,MUTED,true);ntitle.setLetterSpacing(.08f);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);np.topMargin=dp(18);np.bottomMargin=dp(8);root.addView(ntitle,np);
        notices=new LinearLayout(this);notices.setOrientation(LinearLayout.VERTICAL);root.addView(notices);

        if(canPublish){
            LinearLayout publish=card(BLUE);publish.addView(text("Post leadership notice",18,TEXT,true));
            EditText subject=new EditText(this);subject.setHint("Title");subject.setHintTextColor(MUTED);subject.setTextColor(TEXT);subject.setSingleLine(true);subject.setBackground(rounded(BG,BORDER,10));subject.setPadding(dp(12),0,dp(12),0);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));sp.topMargin=dp(10);publish.addView(subject,sp);
            EditText message=new EditText(this);message.setHint("Message to faction members");message.setHintTextColor(MUTED);message.setTextColor(TEXT);message.setMinLines(3);message.setGravity(Gravity.TOP);message.setBackground(rounded(BG,BORDER,10));message.setPadding(dp(12),dp(10),dp(12),dp(10));LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(100));mp.topMargin=dp(8);publish.addView(message,mp);
            EditText hours=new EditText(this);hours.setHint("Expires after hours");hours.setText("72");hours.setTextColor(TEXT);hours.setHintTextColor(MUTED);hours.setSingleLine(true);hours.setInputType(InputType.TYPE_CLASS_NUMBER);hours.setBackground(rounded(BG,BORDER,10));hours.setPadding(dp(12),0,dp(12),0);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));hp.topMargin=dp(8);publish.addView(hours,hp);
            Button post=button("Publish Notice",true);post.setOnClickListener(v->{String titleValue=subject.getText().toString().trim(),messageValue=message.getText().toString().trim();int ttl=Math.max(1,Integer.parseInt(hours.getText().toString().trim().isEmpty()?"72":hours.getText().toString().trim()));if(titleValue.isEmpty()||messageValue.isEmpty()){Toast.makeText(this,"Title and message are required.",Toast.LENGTH_SHORT).show();return;}post.setEnabled(false);String key=keyStore.load();new Thread(()->{try{long expiresAt=(System.currentTimeMillis()/1000L)+ttl*3600L;CompanionBackendClient.publishNotice(key,titleValue,messageValue,expiresAt);runOnUiThread(()->{subject.setText("");message.setText("");Toast.makeText(this,"Notice published.",Toast.LENGTH_SHORT).show();post.setEnabled(true);loadNotices();});}catch(Exception e){runOnUiThread(()->{Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();post.setEnabled(true);});}}).start();});LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));pp.topMargin=dp(10);publish.addView(post,pp);
            LinearLayout.LayoutParams pubp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);pubp.topMargin=dp(10);root.addView(publish,pubp);
        }
        setContentView(scroll);scroll.requestApplyInsets();
    }

    private void refreshAll(){String key=keyStore.load();if(key==null||key.isEmpty())return;new Thread(()->{try{currentWar=WarStatus.from(TornApiClient.getJson("/faction/wars",key),factionId);}catch(Exception ignored){currentWar=WarStatus.none();}runOnUiThread(this::renderWarText);}).start();loadNotices();}
    private void renderWarText(){if(warHeadline==null||warDetail==null)return;long now=System.currentTimeMillis()/1000L;warHeadline.setText(currentWar.headline(now));warDetail.setText(currentWar.detail(now));warHeadline.setTextColor(currentWar.isLive(now)?GOOD:TEXT);}

    private void loadNotices(){
        notices.removeAllViews();
        if(!CompanionBackendClient.isConfigured()){addNoticeCard("Notice board ready","Shared backend deployment is still pending. War countdown works now; faction-wide leadership messages will switch on when the backend URL is connected.","",BORDER);return;}
        String key=keyStore.load();new Thread(()->{try{JSONArray rows=CompanionBackendClient.getNotices(key);runOnUiThread(()->renderNotices(rows));}catch(Exception e){runOnUiThread(()->addNoticeCard("Unable to load notices",e.getMessage(),"",BORDER));}}).start();
    }

    private void renderNotices(JSONArray rows){notices.removeAllViews();if(rows==null||rows.length()==0){addNoticeCard("No active notices","Leadership announcements will appear here.","",BORDER);return;}for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;long created=row.optLong("created_at",0);String meta=row.optString("author_name","Leadership");if(created>0)meta+=" • "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(created*1000L));addNoticeCard(row.optString("title","Notice"),row.optString("message",""),meta,ACCENT);}}
    private void addNoticeCard(String title,String message,String meta,int stroke){LinearLayout c=card(stroke);c.addView(text(title,17,TEXT,true));TextView body=text(message==null?"":message,13,MUTED,false);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(5);c.addView(body,bp);if(meta!=null&&!meta.isEmpty()){TextView m=text(meta,11,MUTED,false);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);mp.topMargin=dp(7);c.addView(m,mp);}LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);cp.bottomMargin=dp(8);notices.addView(c,cp);}

    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}

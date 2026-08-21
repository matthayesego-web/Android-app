package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private static final int MUTED = Color.rgb(151, 163, 179), GOOD = Color.rgb(63, 185, 80), BLUE = Color.rgb(88, 166, 255), RED = Color.rgb(239, 88, 82);
    private static final long NOTICE_CACHE_MS = 60L * 60L * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SecureApiKeyStore keyStore;
    private int factionId;
    private String factionName="Faction";
    private boolean canPublish;
    private boolean noticesRefreshing;
    private TextView warHeadline, warDetail, noticeFreshness;
    private LinearLayout notices;
    private WarStatus currentWar = WarStatus.none();

    private final Runnable ticker = new Runnable() {
        @Override public void run() { renderWarText(); handler.postDelayed(this, 1000L); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        keyStore = new SecureApiKeyStore(this);
        factionId = getIntent().getIntExtra(EXTRA_FACTION_ID, 0);
        String suppliedName=getIntent().getStringExtra(EXTRA_FACTION_NAME);if(suppliedName!=null&&!suppliedName.isBlank())factionName=suppliedName;
        canPublish = getIntent().getBooleanExtra(EXTRA_CAN_PUBLISH, false) && !MemberPresentationPolicy.memberPreview(this);
        String key=keyStore.load();
        if(key!=null&&!key.isBlank()){
            AuthSession hot=TornApiClient.cachedSession(key);FactionScopeCache.Scope scope=hot==null?FactionScopeCache.load(this,key):null;
            if(hot!=null){factionId=hot.factionId;factionName=hot.factionName;if(!MemberPresentationPolicy.memberPreview(this))canPublish=AccessPolicy.isLeaderPosition(hot.position)||canPublish;}
            else if(scope!=null){factionId=scope.factionId;factionName=scope.factionName;if(!MemberPresentationPolicy.memberPreview(this))canPublish=AccessPolicy.isLeaderPosition(scope.position)||canPublish;}
        }
        startScreen();
    }

    @Override protected void onResume(){
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override protected void onPause(){
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    private void startScreen(){buildUi();renderWarmData();refreshAll(false);}
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable rounded(int fill, int stroke, int radius) { GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d; }
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout card(int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(rounded(PANEL,stroke,17));return c;}
    private Button button(String label,boolean primary){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(primary?Color.rgb(23,17,7):TEXT);b.setBackground(primary?rounded(ACCENT,ACCENT,11):rounded(PANEL2,BORDER,11));return b;}

    @SuppressWarnings("deprecation")
    private void buildUi() {
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        int l=dp(16),t=dp(16),r=dp(16),b=dp(90);scroll.setPadding(l,t,r,b);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(l+insets.getSystemWindowInsetLeft(),t+insets.getSystemWindowInsetTop(),r+insets.getSystemWindowInsetRight(),b+insets.getSystemWindowInsetBottom());return insets;});
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        Button back=button("← Companion",false);back.setOnClickListener(v->finish());root.addView(back,new LinearLayout.LayoutParams(dp(122),dp(44)));
        TextView title=text("Faction Announcements",30,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);root.addView(title,tp);
        TextView intro=text("Leadership announcements stay here until deleted. New announcements briefly appear at the top of TornFCA and can be tapped to open this page.",13,MUTED,false);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ip.topMargin=dp(4);ip.bottomMargin=dp(14);root.addView(intro,ip);

        LinearLayout war=card(ACCENT);warHeadline=text("Checking ranked war…",21,TEXT,true);warDetail=text("",13,MUTED,false);war.addView(warHeadline);LinearLayout.LayoutParams wdp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);wdp.topMargin=dp(6);war.addView(warDetail,wdp);Button refresh=button("Refresh War / Announcements",false);refresh.setOnClickListener(v->{Toast.makeText(this,"Refreshing faction updates…",Toast.LENGTH_SHORT).show();refreshAll(true);});LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));rp.topMargin=dp(12);war.addView(refresh,rp);root.addView(war);

        TextView ntitle=text("ACTIVE ANNOUNCEMENTS",12,ACCENT,true);ntitle.setLetterSpacing(.08f);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);np.topMargin=dp(20);root.addView(ntitle,np);
        noticeFreshness=text("Waiting for faction announcements",11.5f,MUTED,false);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=dp(3);fp.bottomMargin=dp(10);root.addView(noticeFreshness,fp);
        notices=new LinearLayout(this);notices.setOrientation(LinearLayout.VERTICAL);root.addView(notices);

        if(canPublish){
            LinearLayout publish=card(BLUE);publish.addView(text("Post faction announcement",19,TEXT,true));
            TextView helper=text("Announcements stay active until leadership deletes them. Members receive the brief in-app banner and the configured Android push notification.",12.5f,MUTED,false);LinearLayout.LayoutParams hp0=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp0.topMargin=dp(4);publish.addView(helper,hp0);
            EditText subject=new EditText(this);subject.setHint("Announcement title");subject.setHintTextColor(MUTED);subject.setTextColor(TEXT);subject.setSingleLine(true);subject.setBackground(rounded(BG,BORDER,10));subject.setPadding(dp(12),0,dp(12),0);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));sp.topMargin=dp(12);publish.addView(subject,sp);
            EditText message=new EditText(this);message.setHint("Message to faction members");message.setHintTextColor(MUTED);message.setTextColor(TEXT);message.setMinLines(3);message.setGravity(Gravity.TOP);message.setBackground(rounded(BG,BORDER,10));message.setPadding(dp(12),dp(10),dp(12),dp(10));LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(112));mp.topMargin=dp(8);publish.addView(message,mp);
            Button post=button("Publish Announcement",true);post.setOnClickListener(v->{String titleValue=subject.getText().toString().trim(),messageValue=message.getText().toString().trim();if(titleValue.isEmpty()||messageValue.isEmpty()){Toast.makeText(this,"Title and message are required.",Toast.LENGTH_SHORT).show();return;}post.setEnabled(false);post.setText("Verifying & publishing…");String key=keyStore.load();new Thread(()->{try{
                AuthSession verified=TornApiClient.authenticateFreshFaction(key);if(!AccessPolicy.isLeaderPosition(verified.position))throw new Exception("Leader or Co-leader verification is required to publish faction announcements.");
                CompanionBackendClient.publishNotice(key,titleValue,messageValue,0L);
                runOnUiThread(()->{subject.setText("");message.setText("");Toast.makeText(this,"Announcement published to the faction.",Toast.LENGTH_SHORT).show();post.setEnabled(true);post.setText("Publish Announcement");loadNotices();});
            }catch(Exception e){String error=e.getMessage()==null?"Unable to publish faction announcement.":e.getMessage();runOnUiThread(()->{Toast.makeText(this,error,Toast.LENGTH_LONG).show();post.setEnabled(true);post.setText("Publish Announcement");});}}).start();});LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));pp.topMargin=dp(10);publish.addView(post,pp);
            LinearLayout.LayoutParams pubp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);pubp.topMargin=dp(12);root.addView(publish,pubp);
        }
        setContentView(scroll);scroll.requestApplyInsets();
    }

    private void renderWarmData(){
        if(factionId<=0)return;
        JSONObject war=StartupWarmCache.war(factionId,2L*60L*1000L);if(war!=null){try{currentWar=WarStatus.from(war,factionId);}catch(Exception ignored){}}
        JSONArray cached=StartupWarmCache.notices(this,factionId,NOTICE_CACHE_MS);if(cached!=null)renderNotices(cached);else renderNotices(new JSONArray());
        renderWarText();updateNoticeFreshness();
    }

    private void refreshAll(boolean manual){String key=keyStore.load();if(key==null||key.isEmpty())return;new Thread(()->{try{JSONObject root=TornApiClient.getJson("/faction/wars",key);StartupWarmCache.putWar(factionId,root);currentWar=WarStatus.from(root,factionId);}catch(Exception ignored){}runOnUiThread(this::renderWarText);}).start();loadNotices();}
    private void renderWarText(){if(warHeadline==null||warDetail==null)return;long now=System.currentTimeMillis()/1000L;warHeadline.setText(currentWar.headline(now));warDetail.setText(currentWar.detail(now));warHeadline.setTextColor(currentWar.isLive(now)?GOOD:TEXT);}

    private void loadNotices(){
        if(!CompanionBackendClient.isConfigured()){if(notices.getChildCount()==0)addNoticeCard("","Announcement board ready","Shared backend is not configured in this build.","",BORDER);return;}
        String key=keyStore.load();if(key==null||key.isBlank())return;
        noticesRefreshing=true;updateNoticeFreshness();
        new Thread(()->{try{
            JSONArray rows=CompanionBackendClient.getNotices(key);StartupWarmCache.putNotices(this,factionId,rows);
            runOnUiThread(()->{noticesRefreshing=false;renderNotices(rows);});
        }catch(Exception e){runOnUiThread(()->{noticesRefreshing=false;updateNoticeFreshness();if(notices.getChildCount()==0)addNoticeCard("","Unable to refresh announcements",e.getMessage(),"",BORDER);});}}).start();
    }

    private void updateNoticeFreshness(){if(noticeFreshness!=null)noticeFreshness.setText(DataFreshness.label(StartupWarmCache.noticesAgeMs(this,factionId),noticesRefreshing));}
    private void renderNotices(JSONArray rows){
        notices.removeAllViews();
        if(rows==null||rows.length()==0){addNoticeCard("","No active announcements","Leadership announcements will stay here until deleted.","",BORDER);updateNoticeFreshness();return;}
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            long created=row.optLong("created_at",0);String meta=row.optString("author_name","Leadership");if(created>0)meta+=" • "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(created*1000L));
            addNoticeCard(row.optString("id",""),row.optString("title","Announcement"),row.optString("message",""),meta,ACCENT);
        }
        updateNoticeFreshness();
    }

    private void addNoticeCard(String noticeId,String title,String message,String meta,int stroke){
        LinearLayout c=card(stroke);
        c.addView(text(title,19,TEXT,true));
        TextView body=text(message==null?"":message,14,TEXT,false);body.setLineSpacing(0f,1.08f);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(7);c.addView(body,bp);
        if(meta!=null&&!meta.isEmpty()){TextView m=text(meta,11.5f,MUTED,false);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);mp.topMargin=dp(10);c.addView(m,mp);}
        if(canPublish&&noticeId!=null&&!noticeId.isBlank()){
            Button delete=button("Delete announcement",false);delete.setTextColor(RED);delete.setOnClickListener(v->confirmDelete(noticeId,title,delete));LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));dpv.topMargin=dp(12);c.addView(delete,dpv);
        }
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);cp.bottomMargin=dp(12);notices.addView(c,cp);
    }

    private void confirmDelete(String noticeId,String title,Button button){
        new AlertDialog.Builder(this)
                .setTitle("Delete announcement?")
                .setMessage("Delete “"+title+"” for all faction members? This cannot be undone.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Delete",(dialog,which)->deleteNotice(noticeId,button))
                .show();
    }

    private void deleteNotice(String noticeId,Button button){
        String key=keyStore.load();if(key==null||key.isBlank())return;
        button.setEnabled(false);button.setText("Deleting…");
        new Thread(()->{try{
            AuthSession verified=TornApiClient.authenticateFreshFaction(key);if(!AccessPolicy.isLeaderPosition(verified.position))throw new Exception("Leader or Co-leader verification is required to delete faction announcements.");
            CompanionBackendClient.deleteNotice(key,noticeId);
            runOnUiThread(()->{Toast.makeText(this,"Announcement deleted.",Toast.LENGTH_SHORT).show();loadNotices();});
        }catch(Exception e){String error=e.getMessage()==null?"Unable to delete faction announcement.":e.getMessage();runOnUiThread(()->{button.setEnabled(true);button.setText("Delete announcement");Toast.makeText(this,error,Toast.LENGTH_LONG).show();});}}).start();
    }

    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}

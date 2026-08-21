package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** TornFCA launcher and cold-process warm-start gate. */
public class AccessGateActivity extends Activity {
    private static final long MIN_VISIBLE_MS = 900L;
    private static final long MAX_VISIBLE_MS = 12000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startedAtMs;
    private boolean launched;
    private TextView status, detail;
    private ProgressBar progress;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(TornFcaUi.BG);
        getWindow().setNavigationBarColor(TornFcaUi.BG);
        TornFcaCommandRuntime.install(getApplication());
        if(!LegalAcceptanceStore.hasAcceptedCurrent(this)){
            Intent i=new Intent(this,LegalActivity.class);
            i.putExtra(LegalActivity.EXTRA_REQUIRE_ACCEPTANCE,true);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);finish();return;
        }

        // A review session is entirely synthetic and must never warm live Torn/backend/Firebase services.
        if(PlayReviewStore.isActive(this)){
            Intent review=new Intent(this,PlayReviewActivity.class);
            review.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(review);finish();return;
        }

        String saved=new SecureApiKeyStore(this).load();
        if(saved==null||saved.trim().isEmpty()){
            buildUnauthenticatedChoice();
            return;
        }
        startNormalWarmup();
    }

    private void buildUnauthenticatedChoice(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(28),dp(56),dp(28),dp(38));root.setBackgroundColor(TornFcaUi.BG);
        ImageView mark=new ImageView(this);mark.setImageResource(TornFcaCommandRuntime.isBetaBuild()?R.drawable.tornfca_beta_crest:R.drawable.tornfca_mark);mark.setContentDescription("TornFCA");mark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);root.addView(mark,new LinearLayout.LayoutParams(dp(100),dp(100)));
        TextView brand=text(TornFcaCommandRuntime.topBrand(),12,TornFcaUi.GOLD,true);brand.setLetterSpacing(.16f);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(20);root.addView(brand,bp);
        TextView title=text("Faction companion",28,TornFcaUi.TEXT,true);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(7);root.addView(title,tp);
        TextView intro=text("Connect a Torn account for live faction tools, or use the isolated store-review sandbox when evaluating TornFCA for Google Play.",13,TornFcaUi.MUTED,false);intro.setGravity(Gravity.CENTER);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ip.topMargin=dp(8);ip.bottomMargin=dp(22);root.addView(intro,ip);

        Button live=TornFcaUi.button(this,"Continue to TornFCA",TornFcaUi.GOLD);live.setOnClickListener(v->{live.setEnabled(false);startNormalWarmup();});root.addView(live,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50)));
        Button review=TornFcaUi.button(this,"Google Play Review Access",TornFcaUi.PURPLE);review.setOnClickListener(v->{Intent i=new Intent(this,PlayReviewActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();});LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));rp.topMargin=dp(10);root.addView(review,rp);
        TextView foot=text("Review Access uses synthetic data only. It never authenticates to Torn or writes to TornFCA production services.",10.5f,TornFcaUi.MUTED,false);foot.setGravity(Gravity.CENTER);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=dp(16);root.addView(foot,fp);
        setContentView(root);
    }

    private void startNormalWarmup(){
        // Backgrounding/reopening an already-running TornFCA process must not repeat the full cache cycle.
        if(StartupWarmup.hasStartedThisProcess()){
            openHome("");
            return;
        }

        startedAtMs=System.currentTimeMillis();
        buildLoadingScreen();
        handler.postDelayed(()->openHome("Core startup complete. Remaining services will refresh in the background."),MAX_VISIBLE_MS);
        StartupWarmup.start(this,new StartupWarmup.Listener(){
            @Override public void onProgress(String message,int completed,int total){
                runOnUiThread(()->{
                    if(status!=null)status.setText(message);
                    if(progress!=null){progress.setMax(Math.max(1,total));progress.setProgress(Math.max(0,Math.min(total,completed)));}
                    if(detail!=null)detail.setText(completed+" of "+total+" startup services ready");
                });
            }
            @Override public void onFinished(StartupWarmup.Result result){
                runOnUiThread(()->{
                    if(result!=null&&detail!=null&&!result.warning.isBlank())detail.setText(result.warning);
                    long elapsed=System.currentTimeMillis()-startedAtMs;
                    handler.postDelayed(()->openHome(result==null?"":result.warning),Math.max(0L,MIN_VISIBLE_MS-elapsed));
                });
            }
        });
    }

    private void buildLoadingScreen(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(28),dp(70),dp(28),dp(42));root.setBackgroundColor(TornFcaUi.BG);
        ImageView mark=new ImageView(this);mark.setImageResource(TornFcaCommandRuntime.isBetaBuild()?R.drawable.tornfca_beta_crest:R.drawable.tornfca_mark);mark.setContentDescription("TornFCA");mark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);root.addView(mark,new LinearLayout.LayoutParams(dp(104),dp(104)));
        TextView brand=text(TornFcaCommandRuntime.topBrand(),12,TornFcaUi.GOLD,true);brand.setLetterSpacing(.16f);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(24);root.addView(brand,bp);
        TextView title=text("Preparing your companion",27,TornFcaUi.TEXT,true);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(8);root.addView(title,tp);
        TextView intro=text("Warming TornFCA once for this app session so the screens you open next can use ready data instead of starting cold.",13,TornFcaUi.MUTED,false);intro.setGravity(Gravity.CENTER);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ip.topMargin=dp(8);ip.bottomMargin=dp(24);root.addView(intro,ip);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(17),dp(18),dp(17));GradientDrawable bg=TornFcaUi.gradient(this,TornFcaUi.PANEL,TornFcaUi.PANEL2,TornFcaUi.BLUE,18);card.setBackground(bg);
        status=text("Starting TornFCA services…",17,TornFcaUi.TEXT,true);card.addView(status);
        detail=text("Checking reusable startup data",12,TornFcaUi.MUTED,false);LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);dpv.topMargin=dp(5);card.addView(detail,dpv);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(false);progress.setMax(5);progress.setProgress(0);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(7));pp.topMargin=dp(16);card.addView(progress,pp);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);cp.topMargin=dp(6);root.addView(card,cp);
        TextView foot=text("This full warm-up runs again only after TornFCA starts in a new Android process.",10.5f,TornFcaUi.MUTED,false);foot.setGravity(Gravity.CENTER);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=dp(18);root.addView(foot,fp);
        setContentView(root);
    }

    private void openHome(String warning){
        if(launched||isFinishing())return;launched=true;handler.removeCallbacksAndMessages(null);
        Intent i=TornFcaCommandRuntime.homeIntent(this,"Home");
        if(warning!=null&&!warning.isBlank())i.putExtra("startup_warning",warning);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);finish();
    }

    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}

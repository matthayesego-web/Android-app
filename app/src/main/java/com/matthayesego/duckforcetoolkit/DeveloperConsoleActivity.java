package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DeveloperConsoleActivity extends Activity {
    public static final String EXTRA_FACTION_ID = "faction_id";
    public static final String EXTRA_FACTION_NAME = "faction_name";
    public static final String EXTRA_FACTION_API = "faction_api";
    public static final String EXTRA_POSITION = "position";

    private static final int BG=Color.rgb(8,12,18), PANEL=Color.rgb(20,27,38), PANEL2=Color.rgb(27,36,49), BORDER=Color.rgb(49,63,81);
    private static final int TEXT=Color.rgb(245,248,252), MUTED=Color.rgb(151,163,179), GOLD=Color.rgb(243,184,52), BLUE=Color.rgb(88,166,255), GOOD=Color.rgb(63,185,80), BAD=Color.rgb(248,81,73);
    private SecureApiKeyStore keyStore;
    private int factionId;
    private String factionName;
    private boolean factionApi;
    private String position;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        keyStore=new SecureApiKeyStore(this);factionId=getIntent().getIntExtra(EXTRA_FACTION_ID,0);factionName=getIntent().getStringExtra(EXTRA_FACTION_NAME);factionApi=getIntent().getBooleanExtra(EXTRA_FACTION_API,false);position=getIntent().getStringExtra(EXTRA_POSITION);if(factionName==null)factionName="Faction";if(position==null)position="Unknown";render();
    }

    private boolean effectiveFactionApi(){return factionApi&&!DeveloperSettings.simulatePublicOnly(this);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,BORDER,11));return b;}
    private LinearLayout card(String title,String body,int stroke){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(rounded(PANEL,stroke,17));c.addView(text(title,18,TEXT,true));if(body!=null&&!body.isEmpty()){TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(6);c.addView(b,p);}return c;}
    private void addCard(LinearLayout root,LinearLayout card){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(10);root.addView(card,p);}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);int l=dp(16),t=dp(16),r=dp(16),b=dp(28);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}

    private void render(){
        ScrollView s=shell();LinearLayout r=root(s);Button back=button("← Companion");back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(124),dp(44)));
        TextView title=text("Developer Console",27,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);tp.bottomMargin=dp(4);r.addView(title,tp);TextView sub=text("v0.7 prep control surface • read-only testing unless explicitly stated",13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.bottomMargin=dp(14);r.addView(sub,sp);

        boolean multi=DeveloperSettings.multiFactionPreview(this);
        addCard(r,card("Current tenant",factionName+" ["+factionId+"] • "+position+"\nFaction API access: "+(factionApi?"YES":"NO")+"\nEffective test access: "+(effectiveFactionApi()?"FACTION API":"PUBLIC-ONLY")+"\nTenant architecture: "+(multi?"PREVIEW":"SINGLE-FACTION")+"\nBackend configured: "+(CompanionBackendClient.isConfigured()?"YES":"NO"),BLUE));

        long cacheAge=FactionScopeCache.ageSeconds(this);String cacheText=FactionScopeCache.hasFreshScope(this)?"Fresh faction scope cache • age "+cacheAge+" sec • expires after 5 minutes.":"No fresh faction scope cache. The next feature launch will re-verify Torn identity and faction.";LinearLayout cache=card("Faction scope verification",cacheText,BORDER);Button fresh=button("Force Fresh Faction Verification");fresh.setOnClickListener(v->{FactionScopeCache.clear(this);Toast.makeText(this,"Faction scope cache cleared.",Toast.LENGTH_SHORT).show();render();});LinearLayout.LayoutParams freshp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));freshp.topMargin=dp(7);cache.addView(fresh,freshp);addCard(r,cache);

        LinearLayout quick=card("Feature test launchers","Open any quick-access module using the current developer settings and authenticated faction scope.",BLUE);
        addLaunchButton(quick,"Open Activity Tracker",FeatureRouterActivity.TARGET_ACTIVITY);
        addLaunchButton(quick,"Open War Participation",FeatureRouterActivity.TARGET_WAR);
        addLaunchButton(quick,"Open Chain Command Center",FeatureRouterActivity.TARGET_CHAIN);
        addLaunchButton(quick,"Open OC Tracker",FeatureRouterActivity.TARGET_OC);
        addLaunchButton(quick,"Open Faction Pulse",FeatureRouterActivity.TARGET_PULSE);
        addLaunchButton(quick,"Open Member Lookup",FeatureRouterActivity.TARGET_LOOKUP);
        Button premiumLaunch=button("Open Premium Preview");premiumLaunch.setOnClickListener(v->startActivity(new Intent(this,PremiumPreviewActivity.class)));LinearLayout.LayoutParams plp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));plp.topMargin=dp(7);quick.addView(premiumLaunch,plp);
        addCard(r,quick);

        LinearLayout multiCard=card("Multi-faction architecture preview",multi?"ON — faction-scoped tools are marked for tenant-preview testing. The production sign-in gate remains Duck Force-only until the multi-faction release gate is deliberately opened.":"OFF — production and developer behavior remain single-faction. New data remains scoped by authenticated faction ID.",multi?GOOD:GOLD);Button multiButton=button(multi?"Turn Tenant Preview Off":"Turn Tenant Preview On");multiButton.setOnClickListener(v->{DeveloperSettings.setMultiFactionPreview(this,!DeveloperSettings.multiFactionPreview(this));render();});multiCard.addView(multiButton,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46)));addCard(r,multiCard);

        boolean publicOnly=DeveloperSettings.simulatePublicOnly(this);LinearLayout publicCard=card("Permission simulation",publicOnly?"PUBLIC-ONLY simulation is ON. Faction API-only features behave as though this member lacks Faction API Access.":"Using the real authenticated Torn permission state.",publicOnly?GOLD:BORDER);Button publicButton=button(publicOnly?"Use Real Permissions":"Simulate Public-Only Member");publicButton.setOnClickListener(v->{DeveloperSettings.setSimulatePublicOnly(this,!DeveloperSettings.simulatePublicOnly(this));render();});publicCard.addView(publicButton,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46)));addCard(r,publicCard);

        boolean premiumSim=DeveloperSettings.simulatePremium(this);LinearLayout premiumCard=card("Premium entitlement simulation",premiumSim?"PREMIUM simulation is ON. This only affects local developer testing and does not represent a real payment or backend entitlement.":"Premium simulation is OFF. Real premium will require a backend-verified Torn player entitlement.",premiumSim?GOOD:GOLD);Button premiumButton=button(premiumSim?"Use Real Entitlement State":"Simulate Premium Member");premiumButton.setOnClickListener(v->{DeveloperSettings.setSimulatePremium(this,!DeveloperSettings.simulatePremium(this));render();});premiumCard.addView(premiumButton,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46)));addCard(r,premiumCard);

        TextView featureTitle=text("FEATURE SWITCHES",12,MUTED,true);featureTitle.setLetterSpacing(.08f);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=dp(5);fp.bottomMargin=dp(8);r.addView(featureTitle,fp);
        addFeatureToggle(r,"Activity Tracker",DeveloperSettings.FEATURE_ACTIVITY,"faction-log participation scanner");
        addFeatureToggle(r,"War Participation",DeveloperSettings.FEATURE_WAR,"live hit participation + completed ranked-war report");
        addFeatureToggle(r,"Chain Command Center",DeveloperSettings.FEATURE_CHAIN,"live chain and member-readiness snapshot");
        addFeatureToggle(r,"OC Tracker",DeveloperSettings.FEATURE_OC,"recruiting, planning and recent completed organized crimes");
        addFeatureToggle(r,"Faction Pulse",DeveloperSettings.FEATURE_PULSE,"fast faction health and availability snapshot");
        addFeatureToggle(r,"Member Lookup",DeveloperSettings.FEATURE_LOOKUP,"local search across the loaded faction member snapshot");
        addFeatureToggle(r,"Premium Preview",DeveloperSettings.FEATURE_PREMIUM_PREVIEW,"locked premium concepts and entitlement architecture preview");

        LinearLayout lookback=card("Activity scanner controls","Lookback: "+DeveloperSettings.activityDays(this)+" days • API page cap: "+DeveloperSettings.activityMaxPages(this)+" pages per news-category batch (100 rows/page).",BORDER);
        LinearLayout daysRow=new LinearLayout(this);daysRow.setOrientation(LinearLayout.HORIZONTAL);int[] daysValues={7,14,30};for(int d:daysValues){Button b=button(d+" days");b.setOnClickListener(v->{DeveloperSettings.setActivityDays(this,d);render();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1f);if(d!=7)p.leftMargin=dp(6);daysRow.addView(b,p);}lookback.addView(daysRow);
        LinearLayout pageRow=new LinearLayout(this);pageRow.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams prp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));prp.topMargin=dp(7);int[] pageValues={5,10,20};for(int pages:pageValues){Button b=button(pages+" pages");b.setOnClickListener(v->{DeveloperSettings.setActivityMaxPages(this,pages);render();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1f);if(pages!=5)p.leftMargin=dp(6);pageRow.addView(b,p);}lookback.addView(pageRow,prp);addCard(r,lookback);

        boolean verbose=DeveloperSettings.verboseDiagnostics(this);LinearLayout verboseCard=card("Diagnostic detail",verbose?"Verbose errors are enabled for developer endpoint checks.":"Concise endpoint status only.",BORDER);Button verboseButton=button(verbose?"Use Concise Diagnostics":"Enable Verbose Diagnostics");verboseButton.setOnClickListener(v->{DeveloperSettings.setVerboseDiagnostics(this,!DeveloperSettings.verboseDiagnostics(this));render();});verboseCard.addView(verboseButton,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46)));addCard(r,verboseCard);

        LinearLayout diag=card("API diagnostics","Run a read-only endpoint sweep using the encrypted API key currently stored on this device.",GOLD);Button run=button("Run Endpoint Diagnostics");run.setOnClickListener(v->runDiagnostics());diag.addView(run,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46)));addCard(r,diag);

        LinearLayout reset=card("Reset prep controls","Restores all feature modules, single-faction mode, real permission checks, real entitlement state, 30-day/20-page activity scanning and concise diagnostics. Banking data is not touched.",BORDER);Button resetButton=button("Reset Developer Settings");resetButton.setOnClickListener(v->{DeveloperSettings.reset(this);FactionScopeCache.clear(this);render();});reset.addView(resetButton,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46)));addCard(r,reset);

        setContentView(s);s.requestApplyInsets();
    }

    private void addLaunchButton(LinearLayout parent,String label,String target){Button b=button(label);b.setOnClickListener(v->{Intent i=new Intent(this,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,target);startActivity(i);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));p.topMargin=dp(7);parent.addView(b,p);}

    private void addFeatureToggle(LinearLayout root,String label,String feature,String description){boolean enabled=DeveloperSettings.featureEnabled(this,feature);LinearLayout c=card(label,description+"\nStatus: "+(enabled?"ENABLED":"DISABLED"),enabled?GOOD:BORDER);Button b=button(enabled?"Disable for Testing":"Enable Feature");b.setOnClickListener(v->{DeveloperSettings.setFeatureEnabled(this,feature,!DeveloperSettings.featureEnabled(this,feature));render();});c.addView(b,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44)));addCard(root,c);}

    private void runDiagnostics(){
        ScrollView s=shell();LinearLayout r=root(s);addCard(r,card("Diagnostics running","Testing Torn read endpoints without changing faction data…",GOLD));setContentView(s);s.requestApplyInsets();String key=keyStore.load();if(key==null||key.isEmpty()){showDiagnostics(java.util.Collections.singletonList("FAIL • API key not available"));return;}
        new Thread(()->{List<String> rows=new ArrayList<>();test(rows,key,"User faction","/user/faction");test(rows,key,"Faction members","/faction/members");test(rows,key,"Faction wars","/faction/wars");test(rows,key,"Faction chain","/faction/chain");if(effectiveFactionApi()){test(rows,key,"OC recruiting","/faction/crimes?cat=recruiting&limit=1");test(rows,key,"OC planning","/faction/crimes?cat=planning&limit=1");test(rows,key,"OC completed","/faction/crimes?cat=completed&limit=1");test(rows,key,"Faction news","/faction/news?cat=main&limit=1");}else rows.add("SKIP • Faction API-only endpoints (real or simulated public-only access)");showDiagnostics(rows);}).start();
    }

    private void test(List<String> rows,String key,String label,String path){try{JSONObject result=TornApiClient.getJson(path,key);rows.add("PASS • "+label+(DeveloperSettings.verboseDiagnostics(this)?" • fields="+String.valueOf(result.names()):""));}catch(Exception e){String detail=DeveloperSettings.verboseDiagnostics(this)?" • "+String.valueOf(e.getMessage()):"";rows.add("FAIL • "+label+detail);}}

    private void showDiagnostics(List<String> rows){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);Button back=button("← Developer Console");back.setOnClickListener(v->render());r.addView(back,new LinearLayout.LayoutParams(dp(170),dp(44)));TextView title=text("API Diagnostics",27,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(14);tp.bottomMargin=dp(14);r.addView(title,tp);for(String row:rows)addCard(r,card(row.startsWith("PASS")?"PASS":row.startsWith("SKIP")?"SKIP":"FAIL",row,row.startsWith("PASS")?GOOD:row.startsWith("SKIP")?GOLD:BAD));setContentView(s);s.requestApplyInsets();});}
}

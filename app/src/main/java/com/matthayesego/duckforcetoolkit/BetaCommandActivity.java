package com.matthayesego.duckforcetoolkit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import java.io.File;
import java.util.Locale;

/** Canonical TornFCA command shell used by Development and production command runtime. */
public class BetaCommandActivity extends TornFcaActivity {
    public static final String EXTRA_SECTION="section";
    private static final String FRAME_TAG="tornfca-command-shell-v2";

    private LinearLayout pageHost;
    private LinearLayout bottomNav;
    private Identity identity;
    private String currentSection="Home";
    private String currentPlanLabel="FREE";
    private Runnable submenuBack;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){
                if(submenuBack!=null){Runnable back=submenuBack;submenuBack=null;back.run();return;}
                setEnabled(false);getOnBackPressedDispatcher().onBackPressed();setEnabled(true);
            }
        });
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);setIntent(intent);
        if(pageHost!=null)renderRequested(intent==null?null:intent.getStringExtra(EXTRA_SECTION));
    }

    @Override public void setContentView(View view){
        ViewGroup active=findViewById(android.R.id.content);
        if(active!=null&&isCommandShell(active)&&isLegacyAuthenticatedHome(view)){
            Identity now=resolveIdentity();if(isResolvedIdentity(now))identity=now;return;
        }
        super.setContentView(view);
        ViewGroup root=findViewById(android.R.id.content);if(root==null)return;
        forceCurrentVersion(root);if(isLegacyAuthenticatedHome(root))installCommandShell(root);
    }

    @Override protected void onResume(){
        super.onResume();ViewGroup root=findViewById(android.R.id.content);if(root==null)return;
        forceCurrentVersion(root);
        if(isLegacyAuthenticatedHome(root))installCommandShell(root);
        else if(isCommandShell(root)){
            Identity now=resolveIdentity();
            if(!isResolvedIdentity(now)&&isResolvedIdentity(identity))applyCachedAvatar(root);
            else if(identity==null||!identity.sameAs(now))installCommandShell(root);
            else if(!planLabel(now.playerId).equals(currentPlanLabel))installCommandShell(root);
            else applyCachedAvatar(root);
        }
        if(identity!=null&&identity.playerId>0)PremiumBackendClient.refreshAsync(this,identity.playerId,this::refreshPlanBadgeIfNeeded);
    }

    private boolean isCommandShell(ViewGroup root){return root.getChildCount()>0&&FRAME_TAG.equals(root.getChildAt(0).getTag());}
    private boolean isLegacyAuthenticatedHome(View root){if(root==null)return false;if(root instanceof ViewGroup&&isCommandShell((ViewGroup)root))return false;return containsText(root,"Welcome back,")&&containsText(root,"War");}
    private boolean isResolvedIdentity(Identity value){return value!=null&&value.playerId>0;}

    private void installCommandShell(ViewGroup host){
        String requested=pageHost!=null?currentSection:(getIntent()==null?null:getIntent().getStringExtra(EXTRA_SECTION));
        Identity resolved=resolveIdentity();if(isResolvedIdentity(resolved)||identity==null)identity=resolved;
        host.removeAllViews();getWindow().setStatusBarColor(TornFcaCommandUi.BG);getWindow().setNavigationBarColor(TornFcaCommandUi.BG);

        LinearLayout frame=TornFcaCommandUi.vertical(this);frame.setTag(FRAME_TAG);frame.setBackgroundColor(TornFcaCommandUi.BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setBackgroundColor(TornFcaCommandUi.BG);
        int l=TornFcaCommandUi.dp(this,13),t=TornFcaCommandUi.dp(this,7),r=TornFcaCommandUi.dp(this,13),b=TornFcaCommandUi.dp(this,22);
        scroll.setPadding(l,t,r,b);scroll.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b);return i;});

        LinearLayout column=TornFcaCommandUi.vertical(this);addBrandBar(column);addIdentityHero(column);
        pageHost=TornFcaCommandUi.vertical(this);column.addView(pageHost,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.addView(column,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        bottomNav=buildBottomNav();frame.addView(bottomNav,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        host.addView(frame,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        scroll.requestApplyInsets();bottomNav.requestApplyInsets();renderRequested(requested);host.post(()->applyCachedAvatar(host));
    }

    private Identity resolveIdentity(){
        String key=new SecureApiKeyStore(this).load();String player="Member",faction="Faction",position="Member";int playerId=0,factionId=0;boolean factionApi=false;
        if(key!=null&&!key.isBlank()){
            AuthSession hot=TornApiClient.cachedSession(key);
            if(hot!=null){player=clean(hot.playerName,"Member");faction=clean(hot.factionName,"Faction");position=clean(hot.position,"Member");playerId=hot.playerId;factionId=hot.factionId;factionApi=hot.factionApiAccess;}
            else{FactionScopeCache.Scope scope=FactionScopeCache.load(this,key);if(scope!=null){player=clean(scope.playerName,"Member");faction=clean(scope.factionName,"Faction");position=clean(scope.position,"Member");playerId=scope.playerId;factionId=scope.factionId;factionApi=scope.factionApiAccess;}}
        }
        boolean preview=DeveloperPreviewStore.isMemberPreview(this);if(preview)position="Member Preview";
        return new Identity(playerId,player,factionId,faction,position,factionApi,!preview&&AccessPolicy.isLeaderPosition(position));
    }

    private String clean(String value,String fallback){return value==null||value.trim().isEmpty()?fallback:value.trim();}
    private String planLabel(int playerId){if(playerId<=0)return"FREE";boolean verified=PremiumEntitlementStore.TIER_PREMIUM.equals(PremiumEntitlementStore.tier(this,playerId));boolean premium=PremiumAccess.has(this,playerId,PremiumAccess.PERSONAL_INSIGHTS);return premium?(verified?"PREMIUM":"PREMIUM PREVIEW"):"FREE";}
    private void refreshPlanBadgeIfNeeded(){if(isFinishing()||identity==null)return;ViewGroup root=findViewById(android.R.id.content);if(root==null||!isCommandShell(root))return;String now=planLabel(identity.playerId);if(!now.equals(currentPlanLabel))installCommandShell(root);}
    private void applyCachedAvatar(View root){if(identity==null||identity.playerId<=0||root==null)return;ImageView avatar=findAvatar(root);if(avatar==null)return;File file=new File(getCacheDir(),"torn-profile-"+identity.playerId+".img");if(!file.exists())return;try{Bitmap bitmap=BitmapFactory.decodeFile(file.getAbsolutePath());if(bitmap!=null)avatar.setImageBitmap(bitmap);}catch(Exception ignored){}}
    private ImageView findAvatar(View view){if(view instanceof ImageView&&"tornfca-profile-avatar".equals(view.getTag()))return(ImageView)view;if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){ImageView f=findAvatar(g.getChildAt(i));if(f!=null)return f;}}return null;}

    private void addBrandBar(LinearLayout root){
        LinearLayout bar=TornFcaCommandUi.horizontal(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(TornFcaCommandUi.dp(this,7),TornFcaCommandUi.dp(this,7),TornFcaCommandUi.dp(this,7),TornFcaCommandUi.dp(this,9));
        ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.tornfca_beta_crest);mark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);bar.addView(mark,new LinearLayout.LayoutParams(TornFcaCommandUi.dp(this,38),TornFcaCommandUi.dp(this,38)));
        TextView label=TornFcaCommandUi.text(this,TornFcaCommandRuntime.topBrand(),13.5f,TornFcaCommandUi.GOLD_2,true);label.setLetterSpacing(.18f);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);lp.leftMargin=TornFcaCommandUi.dp(this,10);bar.addView(label,lp);
        TextView bell=TornFcaCommandUi.text(this,"◉",18,TornFcaCommandUi.GOLD_2,false);bell.setGravity(Gravity.CENTER);bell.setContentDescription("Notifications");bell.setClickable(true);bell.setFocusable(true);bell.setOnClickListener(v->openActivity(NotificationInboxActivity.class));bar.addView(bell,new LinearLayout.LayoutParams(TornFcaCommandUi.dp(this,42),TornFcaCommandUi.dp(this,42)));
        root.addView(bar,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addIdentityHero(LinearLayout root){
        LinearLayout hero=TornFcaCommandUi.horizontal(this);hero.setGravity(Gravity.CENTER_VERTICAL);hero.setPadding(TornFcaCommandUi.dp(this,14),TornFcaCommandUi.dp(this,13),TornFcaCommandUi.dp(this,14),TornFcaCommandUi.dp(this,13));hero.setBackground(TornFcaCommandUi.gradient(this,new int[]{Color.rgb(15,21,31),Color.rgb(8,12,22),Color.rgb(20,14,43)},24,Color.argb(190,238,185,83),1));hero.setElevation(TornFcaCommandUi.dp(this,5));
        ImageView avatar=new ImageView(this);avatar.setTag("tornfca-profile-avatar");avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);avatar.setImageResource(R.drawable.tornfca_beta_crest);GradientDrawable bg=new GradientDrawable();bg.setShape(GradientDrawable.OVAL);bg.setColor(TornFcaCommandUi.PANEL_3);bg.setStroke(TornFcaCommandUi.dp(this,2),TornFcaCommandUi.GOLD);avatar.setBackground(bg);avatar.setClipToOutline(true);avatar.setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View v,Outline o){o.setOval(0,0,v.getWidth(),v.getHeight());}});hero.addView(avatar,new LinearLayout.LayoutParams(TornFcaCommandUi.dp(this,92),TornFcaCommandUi.dp(this,92)));
        LinearLayout copy=TornFcaCommandUi.vertical(this);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);cp.leftMargin=TornFcaCommandUi.dp(this,13);hero.addView(copy,cp);
        TextView beta=TornFcaCommandUi.text(this,TornFcaCommandRuntime.versionBadge(),9.5f,TornFcaCommandUi.PURPLE_2,true);beta.setLetterSpacing(.10f);copy.addView(beta);
        TextView name=TornFcaCommandUi.text(this,identity.playerName,27,TornFcaCommandUi.TEXT,true);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);np.topMargin=TornFcaCommandUi.dp(this,6);copy.addView(name,np);
        TextView meta=TornFcaCommandUi.text(this,identity.factionName+"  •  "+identity.position,12.5f,TornFcaCommandUi.MUTED,false);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);mp.topMargin=TornFcaCommandUi.dp(this,4);copy.addView(meta,mp);
        String plan=planLabel(identity.playerId);currentPlanLabel=plan;boolean premium=!"FREE".equals(plan);TextView planView=TornFcaCommandUi.text(this,plan,10.5f,premium?TornFcaCommandUi.GOLD_2:TornFcaCommandUi.BLUE,true);planView.setLetterSpacing(.10f);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);pp.topMargin=TornFcaCommandUi.dp(this,7);copy.addView(planView,pp);
        TextView expand=TornFcaCommandUi.text(this,"⌄",22,TornFcaCommandUi.PURPLE_2,false);expand.setGravity(Gravity.CENTER);hero.addView(expand,new LinearLayout.LayoutParams(TornFcaCommandUi.dp(this,38),TornFcaCommandUi.dp(this,44)));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.bottomMargin=TornFcaCommandUi.dp(this,18);root.addView(hero,hp);
    }

    private LinearLayout buildBottomNav(){
        LinearLayout nav=TornFcaCommandUi.horizontal(this);nav.setGravity(Gravity.CENTER);nav.setPadding(TornFcaCommandUi.dp(this,6),TornFcaCommandUi.dp(this,7),TornFcaCommandUi.dp(this,6),TornFcaCommandUi.dp(this,7));nav.setBackground(TornFcaCommandUi.gradient(this,new int[]{Color.rgb(12,14,28),Color.rgb(5,9,15)},24,Color.rgb(42,51,70),1));nav.setElevation(TornFcaCommandUi.dp(this,10));
        nav.addView(navItem("Home",R.drawable.ic_nav_home,this::renderHome),navLp());
        nav.addView(navItem("Faction",R.drawable.ic_nav_faction,this::renderFaction),navLp());
        nav.addView(navItem("War",R.drawable.ic_nav_war,this::renderWar),navLp());
        if(identity!=null&&identity.leader)nav.addView(navItem("Leadership",R.drawable.ic_nav_leadership,this::renderLeadership),navLp());
        nav.addView(navItem("More",R.drawable.ic_nav_more,this::renderMore),navLp());
        int base=TornFcaCommandUi.dp(this,7);nav.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(TornFcaCommandUi.dp(this,6),TornFcaCommandUi.dp(this,7),TornFcaCommandUi.dp(this,6),base+i.getSystemWindowInsetBottom());return i;});return nav;
    }

    private LinearLayout.LayoutParams navLp(){return new LinearLayout.LayoutParams(0,TornFcaCommandUi.dp(this,70),1f);}
    private LinearLayout navItem(String label,int icon,Runnable action){LinearLayout item=TornFcaCommandUi.vertical(this);item.setTag("command-nav:"+label);item.setGravity(Gravity.CENTER);item.setClickable(true);item.setFocusable(true);item.setOnClickListener(v->action.run());ImageView iv=new ImageView(this);Drawable d=getDrawable(icon);if(d!=null){d=d.mutate();d.setTint(TornFcaCommandUi.STEEL);iv.setImageDrawable(d);}item.addView(iv,new LinearLayout.LayoutParams(TornFcaCommandUi.dp(this,26),TornFcaCommandUi.dp(this,26)));TextView text=TornFcaCommandUi.text(this,label,9.2f,TornFcaCommandUi.MUTED,false);text.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=TornFcaCommandUi.dp(this,5);item.addView(text,tp);return item;}
    private void selectNav(String label){if(bottomNav==null)return;for(int i=0;i<bottomNav.getChildCount();i++){View raw=bottomNav.getChildAt(i);if(!(raw instanceof LinearLayout))continue;LinearLayout item=(LinearLayout)raw;boolean selected=("command-nav:"+label).equals(String.valueOf(item.getTag()));item.setBackground(selected?TornFcaCommandUi.gradient(this,new int[]{Color.rgb(62,44,23),Color.rgb(31,24,21)},18,TornFcaCommandUi.GOLD,1):null);item.setElevation(selected?TornFcaCommandUi.dp(this,5):0);for(int j=0;j<item.getChildCount();j++){View child=item.getChildAt(j);if(child instanceof ImageView){Drawable d=((ImageView)child).getDrawable();if(d!=null)d.setTint(selected?TornFcaCommandUi.GOLD_2:TornFcaCommandUi.STEEL);}else if(child instanceof TextView)((TextView)child).setTextColor(selected?TornFcaCommandUi.GOLD_2:TornFcaCommandUi.MUTED);}}}

    private void renderRequested(String section){
        String s=section==null?"":section.trim().toLowerCase(Locale.US);
        if(s.equals("faction")||s.equals("members"))renderFaction();
        else if(s.equals("war")||s.equals("operations"))renderWar();
        else if(s.equals("leadership")) {if(identity!=null&&identity.leader)renderLeadership();else renderWar();}
        else if(s.equals("training"))renderTraining();
        else if(s.equals("leadershippeople"))renderLeadershipPeople();
        else if(s.equals("leadershipwarintel"))renderLeadershipWarIntel();
        else if(s.equals("leadershipfinance"))renderLeadershipFinance();
        else if(s.equals("leadershipadmin"))renderLeadershipAdmin();
        else if(s.equals("more"))renderMore();
        else renderHome();
    }

    private void beginTop(String section,String title,String subtitle,int accent){
        currentSection=section;submenuBack=null;Intent intent=getIntent();if(intent!=null)intent.putExtra(EXTRA_SECTION,section);pageHost.removeAllViews();selectNav(section);
        LinearLayout heading=TornFcaCommandUi.sectionHeading(this,title,subtitle,accent);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.bottomMargin=TornFcaCommandUi.dp(this,16);pageHost.addView(heading,hp);
    }

    private void beginSub(String route,String navSection,String parent,String title,String subtitle,int accent,Runnable back){
        currentSection=route;submenuBack=back;Intent intent=getIntent();if(intent!=null)intent.putExtra(EXTRA_SECTION,route);pageHost.removeAllViews();selectNav(navSection);
        TextView backButton=TornFcaCommandUi.primaryButton(this,"← "+parent,TornFcaCommandUi.LINE,()->{submenuBack=null;back.run();});LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(TornFcaCommandUi.dp(this,170),TornFcaCommandUi.dp(this,42));bp.bottomMargin=TornFcaCommandUi.dp(this,14);pageHost.addView(backButton,bp);
        LinearLayout heading=TornFcaCommandUi.sectionHeading(this,title,subtitle,accent);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.bottomMargin=TornFcaCommandUi.dp(this,16);pageHost.addView(heading,hp);
    }

    private void renderHome(){
        beginTop("Home","Command Center","Your personal starting point. Faction, warfare and leadership now have dedicated homes.",TornFcaCommandUi.GOLD);
        addQuickGrid(
                new Tile(R.drawable.ic_nav_home,"My Day","Daily snapshot",TornFcaCommandUi.GREEN,()->openActivity(MemberDailyActivity.class)),
                new Tile(R.drawable.ic_nav_training,"Training","Progress & guides",TornFcaCommandUi.PURPLE,this::renderTraining),
                new Tile(R.drawable.ic_nav_war,"War Prep","Get ready",TornFcaCommandUi.GOLD,()->openActivity(WarPrepActivity.class)),
                new Tile(R.drawable.ic_nav_more,"Alerts","Inbox",TornFcaCommandUi.BLUE,()->openActivity(NotificationInboxActivity.class)));
        addFeatured("FEATURED","My Day","Bars, cooldowns, organized crime, chain and warfare readiness in one personal command view.","Open My Day",TornFcaCommandUi.PURPLE,()->openActivity(MemberDailyActivity.class));
        LinearLayout readiness=TornFcaCommandUi.panel(this);addPanelTitle(readiness,"Personal Shortcuts","Every tile opens a focused personal tool");
        addMetrics(readiness,
                new Metric("TODAY","OPEN","My Day","Daily view",TornFcaCommandUi.GREEN,()->openActivity(MemberDailyActivity.class)),
                new Metric("TRAIN","GO","Training","Progress",TornFcaCommandUi.PURPLE,this::renderTraining),
                new Metric("ALERTS","INBOX","Notifications","Review",TornFcaCommandUi.BLUE,()->openActivity(NotificationInboxActivity.class)));
        addPanel(readiness);addFooter();
    }

    private void renderTraining(){
        String tracked=trainingBaselineLabel();beginSub("Training","Home","Home","Training & Progress","Personal training tools live under Home instead of occupying permanent navigation.",TornFcaCommandUi.GREEN,this::renderHome);
        addStatusHero("PERSONAL TRACKING","My Training Progress",tracked+" • Battle-stat and Xanax progress stays private to your device.","TRACK",TornFcaCommandUi.PURPLE,()->openActivity(TrainingProgressActivity.class));
        LinearLayout tools=TornFcaCommandUi.panel(this);addPanelTitle(tools,"Training Tools","Guidance, progress and personal preparation");
        addAction(tools,R.drawable.ic_nav_training,"Training Center","Faction-published training guidance and resources","Open",TornFcaCommandUi.PURPLE,()->openActivity(TrainingCenterActivity.class));
        addAction(tools,R.drawable.ic_nav_training,"My Training Progress","Battle-stat and Xanax baseline tracking","Track",TornFcaCommandUi.GREEN,()->openActivity(TrainingProgressActivity.class));
        addAction(tools,R.drawable.ic_nav_war,"My War Prep","Personal readiness checklist","Prep",TornFcaCommandUi.GOLD,()->openActivity(WarPrepActivity.class));
        addPanel(tools);addFooter();
    }

    private void renderFaction(){
        beginTop("Faction","Faction","Communication, roster information and shared faction tools.",TornFcaCommandUi.PURPLE);
        addFeatured("FACTION CHAT","Faction Chat","Your single normal chat entry point. Torn Chat remains available from inside the chat screen.","Open Faction Chat",TornFcaCommandUi.BLUE,()->openActivity(FactionChatActivity.class));
        addDuoGrid(
                new Tile(R.drawable.ic_nav_faction,"Overview","Faction status",TornFcaCommandUi.GOLD,()->openActivity(MemberFactionActivity.class)),
                new Tile(R.drawable.ic_nav_faction,"Directory","Search roster",TornFcaCommandUi.PURPLE,()->openActivity(MemberDirectoryActivity.class)),
                new Tile(R.drawable.ic_nav_more,"Resources","Rules & guides",TornFcaCommandUi.BLUE,()->openActivity(FactionResourcesActivity.class)),
                new Tile(R.drawable.ic_nav_more,"Announcements","Faction notices",TornFcaCommandUi.GOLD,this::openAnnouncements));
        LinearLayout tools=TornFcaCommandUi.panel(this);addPanelTitle(tools,"Faction Tools","Shared status and intelligence without duplicating chat");
        addAction(tools,R.drawable.ic_nav_faction,"My Organized Crime","Your OC assignment and readiness","OC",TornFcaCommandUi.GREEN,()->openFeature(FeatureRouterActivity.TARGET_OC));
        addAction(tools,R.drawable.ic_nav_war,"Chain Status","Current chain context and participation","Chain",TornFcaCommandUi.GOLD,()->openFeature(FeatureRouterActivity.TARGET_CHAIN));
        addAction(tools,R.drawable.ic_nav_faction,"Faction Strength Intel","Optional FFScouter strength estimates","Intel",TornFcaCommandUi.PURPLE,()->openFeature(FeatureRouterActivity.TARGET_STRENGTH));
        addPanel(tools);addFooter();
    }

    private void renderWar(){
        beginTop("War","War","Ranked War, chain, territories and personal readiness stay together.",TornFcaCommandUi.RED);
        addFeatured("RANKED WAR","Ranked War Command","Current matchup, score, timing, participation, opponent intel and completed-war history.","Open Ranked War",TornFcaCommandUi.RED,()->openScopedActivity(WarCenterActivity.class));
        addDuoGrid(
                new Tile(R.drawable.ic_nav_war,"Ranked War","Matchup & score",TornFcaCommandUi.RED,()->openScopedActivity(WarCenterActivity.class)),
                new Tile(R.drawable.ic_nav_war,"Chain Status","Current chain",TornFcaCommandUi.GOLD,()->openFeature(FeatureRouterActivity.TARGET_CHAIN)),
                new Tile(R.drawable.ic_nav_war,"Territories","Walls & assaults",TornFcaCommandUi.GOLD,()->openScopedActivity(TerritoryWarActivity.class)),
                new Tile(R.drawable.ic_nav_training,"War Prep","Personal readiness",TornFcaCommandUi.GREEN,()->openActivity(WarPrepActivity.class)));
        addFooter();
    }

    private void renderLeadership(){
        if(identity==null||!identity.leader){renderWar();return;}
        beginTop("Leadership","Leadership","Leadership-only work is grouped by job instead of mixed into member pages.",TornFcaCommandUi.GOLD);
        addFeatured("PRIORITY","Needs Attention","Review inactivity, war gaps, OC gaps and availability exceptions that may need leadership action.","Review Attention",TornFcaCommandUi.GOLD,()->openActivity(LeadershipAttentionActivity.class));
        addDuoGrid(
                new Tile(R.drawable.ic_nav_faction,"People & Activity","Members & participation",TornFcaCommandUi.PURPLE,this::renderLeadershipPeople),
                new Tile(R.drawable.ic_nav_war,"War & Intel","Spies & war analysis",TornFcaCommandUi.RED,this::renderLeadershipWarIntel),
                new Tile(R.drawable.ic_nav_more,"Finance & Assets","Banking, caches, armory",TornFcaCommandUi.GREEN,this::renderLeadershipFinance),
                new Tile(R.drawable.ic_nav_leadership,"Faction Admin","Publishing & moderation",TornFcaCommandUi.BLUE,this::renderLeadershipAdmin));
        addFooter();
    }

    private void renderLeadershipPeople(){
        if(identity==null||!identity.leader){renderWar();return;}
        beginSub("LeadershipPeople","Leadership","Leadership","People & Activity","Member review and faction participation tools.",TornFcaCommandUi.PURPLE,this::renderLeadership);
        LinearLayout panel=TornFcaCommandUi.panel(this);addPanelTitle(panel,"People & Activity","Review members without mixing in finance or administration");
        addAction(panel,R.drawable.ic_nav_faction,"Activity Tracker","Faction-wide participation and activity scan","Open",TornFcaCommandUi.PURPLE,()->openFeature(FeatureRouterActivity.TARGET_ACTIVITY));
        addAction(panel,R.drawable.ic_nav_faction,"Faction Pulse","Member health, inactivity and availability at a glance","Pulse",TornFcaCommandUi.GOLD,()->openFeature(FeatureRouterActivity.TARGET_PULSE));
        addAction(panel,R.drawable.ic_nav_faction,"Member Dossier","Leadership member lookup and research","Lookup",TornFcaCommandUi.BLUE,()->openFeature(FeatureRouterActivity.TARGET_LOOKUP));
        addPanel(panel);addFooter();
    }

    private void renderLeadershipWarIntel(){
        if(identity==null||!identity.leader){renderWar();return;}
        beginSub("LeadershipWarIntel","Leadership","Leadership","War & Intel","Official spies, strength estimates and Ranked War payout analysis.",TornFcaCommandUi.RED,this::renderLeadership);
        LinearLayout panel=TornFcaCommandUi.panel(this);addPanelTitle(panel,"War & Intel","Keep opponent research and war analysis in one place");
        addAction(panel,R.drawable.ic_nav_faction,"Spy Intel","Official Torn faction stat reports with full/partial spy labels","Spies",TornFcaCommandUi.PURPLE,()->openScopedActivity(SpyIntelActivity.class));
        addAction(panel,R.drawable.ic_nav_faction,"Faction Strength Intel","Optional FFScouter estimates and strength comparison","Intel",TornFcaCommandUi.BLUE,()->openFeature(FeatureRouterActivity.TARGET_STRENGTH));
        addAction(panel,R.drawable.ic_nav_war,"War Payout Calculator","Completed Ranked War participation and payout calculation","Payout",TornFcaCommandUi.RED,()->openFeature(FeatureRouterActivity.TARGET_WAR_PAYOUT));
        addPanel(panel);addFooter();
    }

    private void renderLeadershipFinance(){
        if(identity==null||!identity.leader){renderWar();return;}
        beginSub("LeadershipFinance","Leadership","Leadership","Finance & Assets","Banking, Ranked War cache valuation and armory control.",TornFcaCommandUi.GREEN,this::renderLeadership);
        LinearLayout panel=TornFcaCommandUi.panel(this);addPanelTitle(panel,"Finance & Assets","Money and faction inventory tools stay together");
        addAction(panel,R.drawable.ic_nav_more,"Banking","Faction payout requests and leadership queue","Banking",TornFcaCommandUi.BLUE,()->openFeature(FeatureRouterActivity.TARGET_BANKING));
        addAction(panel,R.drawable.ic_nav_war,"RW Cache Market Advisor","Compare war reward caches with current market and trader leads","Advisor",TornFcaCommandUi.GOLD,()->openScopedActivity(CacheMarketAdvisorActivity.class));
        addAction(panel,R.drawable.ic_nav_more,"Armory Auditor","Audit armory items, member totals, deposits and restocks","Audit",TornFcaCommandUi.GREEN,this::openArmory);
        addPanel(panel);addFooter();
    }

    private void renderLeadershipAdmin(){
        if(identity==null||!identity.leader){renderWar();return;}
        beginSub("LeadershipAdmin","Leadership","Leadership","Faction Admin","Publishing, training administration and community moderation.",TornFcaCommandUi.BLUE,this::renderLeadership);
        LinearLayout panel=TornFcaCommandUi.panel(this);addPanelTitle(panel,"Faction Admin","Administrative work stays out of ordinary member navigation");
        addAction(panel,R.drawable.ic_nav_more,"Faction Announcements","Publish or manage current faction notices","Manage",TornFcaCommandUi.GOLD,this::openAnnouncements);
        addAction(panel,R.drawable.ic_nav_training,"Guide & Training Management","Publish faction-scoped guides and training expectations","Manage",TornFcaCommandUi.PURPLE,()->openActivity(TrainingAdminActivity.class));
        addAction(panel,R.drawable.ic_nav_leadership,"Reports & Moderation","Review faction chat reports and moderation queue","Review",TornFcaCommandUi.RED,()->openActivity(CommunityModerationActivity.class));
        addPanel(panel);addFooter();
    }

    private void renderMore(){
        beginTop("More","More","Settings, alerts, feedback, privacy and optional app services.",TornFcaCommandUi.BLUE);
        addDuoGrid(
                new Tile(R.drawable.ic_nav_more,"Settings","Preferences",TornFcaCommandUi.GOLD,()->openActivity(SettingsActivity.class)),
                new Tile(R.drawable.ic_nav_more,"Alerts","Notification inbox",TornFcaCommandUi.BLUE,()->openActivity(NotificationInboxActivity.class)),
                new Tile(R.drawable.ic_nav_more,"Premium","Optional extras",TornFcaCommandUi.GOLD,()->openActivity(PremiumPreviewActivity.class)),
                new Tile(R.drawable.ic_nav_more,"Feedback","Bugs & requests",TornFcaCommandUi.PURPLE,()->openActivity(FeedbackActivity.class)));
        LinearLayout info=TornFcaCommandUi.panel(this);addPanelTitle(info,"App & Privacy","Less-frequent app controls and information");
        addAction(info,R.drawable.ic_nav_more,"Feedback & Requests","Send a bug report, feature request or usability note","Send",TornFcaCommandUi.PURPLE,()->openActivity(FeedbackActivity.class));
        addAction(info,R.drawable.ic_nav_more,"Legal & Privacy","Privacy Policy, Terms, EULA and acknowledgement","Review",TornFcaCommandUi.BLUE,()->openActivity(LegalActivity.class));
        addAction(info,R.drawable.ic_nav_more,"About Torn FCA","Version, privacy approach and third-party services","About",TornFcaCommandUi.BLUE,()->openActivity(AboutActivity.class));
        addPanel(info);addFooter();
    }

    private String trainingBaselineLabel(){if(identity==null||identity.playerId<=0||identity.factionId<=0)return"Open tracker";SharedPreferences p=getSharedPreferences("tornfca_training_progress_v1",MODE_PRIVATE);long at=p.getLong("p"+identity.playerId+"_f"+identity.factionId+"_at",0L);if(at<=0)return"New baseline";long days=Math.max(1,(System.currentTimeMillis()-at+86399999L)/86400000L);return days+" day"+(days==1?"":"s")+" tracked";}

    private void addQuickGrid(Tile... specs){LinearLayout row=TornFcaCommandUi.horizontal(this);row.setGravity(Gravity.TOP);for(int i=0;i<specs.length;i++){Tile s=specs[i];LinearLayout tile=TornFcaCommandUi.quickTile(this,s.icon,s.title,s.subtitle,s.accent,s.action);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,TornFcaCommandUi.dp(this,118),1f);if(i>0)lp.leftMargin=TornFcaCommandUi.dp(this,7);row.addView(tile,lp);}LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rp.bottomMargin=TornFcaCommandUi.dp(this,14);pageHost.addView(row,rp);}
    private void addDuoGrid(Tile... specs){for(int start=0;start<specs.length;start+=2){LinearLayout row=TornFcaCommandUi.horizontal(this);for(int offset=0;offset<2&&start+offset<specs.length;offset++){Tile s=specs[start+offset];LinearLayout tile=TornFcaCommandUi.duoTile(this,s.icon,s.title,s.subtitle,s.accent,s.action);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,TornFcaCommandUi.dp(this,82),1f);if(offset>0)lp.leftMargin=TornFcaCommandUi.dp(this,8);row.addView(tile,lp);}LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rp.bottomMargin=TornFcaCommandUi.dp(this,start+2>=specs.length?14:8);pageHost.addView(row,rp);}}
    private void addFeatured(String badge,String title,String body,String cta,int accent,Runnable action){FrameLayout f=TornFcaCommandUi.featuredPanel(this,badge,title,body,cta,accent,action);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaCommandUi.dp(this,210));lp.bottomMargin=TornFcaCommandUi.dp(this,15);pageHost.addView(f,lp);}
    private void addStatusHero(String badge,String title,String body,String center,int accent,Runnable action){LinearLayout hero=TornFcaCommandUi.statusHero(this,badge,title,body,center,accent,action);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=TornFcaCommandUi.dp(this,15);pageHost.addView(hero,lp);}
    private void addPanelTitle(LinearLayout panel,String title,String subtitle){LinearLayout line=TornFcaCommandUi.horizontal(this);line.setGravity(Gravity.CENTER_VERTICAL);View accent=new View(this);accent.setBackground(TornFcaCommandUi.solid(this,TornFcaCommandUi.GOLD,2,Color.TRANSPARENT,0));line.addView(accent,new LinearLayout.LayoutParams(TornFcaCommandUi.dp(this,3),TornFcaCommandUi.dp(this,30)));LinearLayout copy=TornFcaCommandUi.vertical(this);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);cp.leftMargin=TornFcaCommandUi.dp(this,9);line.addView(copy,cp);copy.addView(TornFcaCommandUi.text(this,title,15,TornFcaCommandUi.TEXT,true));TextView sub=TornFcaCommandUi.text(this,subtitle,10.5f,TornFcaCommandUi.MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=TornFcaCommandUi.dp(this,2);copy.addView(sub,sp);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=TornFcaCommandUi.dp(this,11);panel.addView(line,lp);}
    private void addMetrics(LinearLayout panel,Metric... specs){LinearLayout row=TornFcaCommandUi.horizontal(this);for(int i=0;i<specs.length;i++){Metric s=specs[i];LinearLayout view=TornFcaCommandUi.metricTile(this,s.eyebrow,s.center,s.title,s.detail,s.accent,s.action);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,TornFcaCommandUi.dp(this,132),1f);if(i>0)lp.leftMargin=TornFcaCommandUi.dp(this,7);row.addView(view,lp);}panel.addView(row,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));}
    private void addAction(LinearLayout panel,int icon,String title,String subtitle,String value,int accent,Runnable action){LinearLayout row=TornFcaCommandUi.actionRow(this,icon,title,subtitle,value,accent,action);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.topMargin=panel.getChildCount()>1?TornFcaCommandUi.dp(this,7):0;panel.addView(row,lp);}
    private void addPanel(LinearLayout panel){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=TornFcaCommandUi.dp(this,15);pageHost.addView(panel,lp);}
    private void addFooter(){TextView footer=TornFcaCommandUi.text(this,TornFcaCommandRuntime.footer(identity==null?"Faction":identity.factionName),10,TornFcaCommandUi.MUTED,false);footer.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=TornFcaCommandUi.dp(this,10);pageHost.addView(footer,lp);}

    private void openActivity(Class<?> target){startActivity(new Intent(this,target));}
    private void openScopedActivity(Class<?> target){Intent i=new Intent(this,target);putScope(i);startActivity(i);}
    private void openFeature(String target){Intent i=new Intent(this,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,target);startActivity(i);}
    private void openArmory(){Intent i=new Intent(this,ToolHostActivity.class);i.putExtra(ToolHostActivity.EXTRA_TOOL,"ARMORY");startActivity(i);}
    private void openAnnouncements(){if(identity==null)identity=resolveIdentity();Intent i=new Intent(this,WarNoticeActivity.class);i.putExtra(WarNoticeActivity.EXTRA_FACTION_ID,identity.factionId);i.putExtra(WarNoticeActivity.EXTRA_FACTION_NAME,identity.factionName);i.putExtra(WarNoticeActivity.EXTRA_CAN_PUBLISH,identity.leader);startActivity(i);}
    private void putScope(Intent i){if(identity==null)identity=resolveIdentity();i.putExtra(FactionOpsActivity.EXTRA_FACTION_ID,identity.factionId);i.putExtra(FactionOpsActivity.EXTRA_FACTION_NAME,identity.factionName);i.putExtra(FactionOpsActivity.EXTRA_FACTION_API,identity.factionApi);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_ID,identity.factionId);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_NAME,identity.factionName);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_API,identity.factionApi);i.putExtra(DeveloperConsoleActivity.EXTRA_POSITION,identity.position);}
    private void forceCurrentVersion(View view){if(view instanceof TextView){TextView t=(TextView)view;CharSequence raw=t.getText();if(raw!=null){String s=raw.toString().replaceAll("v0\\.9\\.\\d+","v"+TornFcaBrand.VERSION);if(!s.equals(raw.toString()))t.setText(s);}}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)forceCurrentVersion(g.getChildAt(i));}}
    private boolean containsText(View view,String needle){if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&raw.toString().toLowerCase(Locale.US).contains(needle.toLowerCase(Locale.US)))return true;}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)if(containsText(g.getChildAt(i),needle))return true;}return false;}

    private static final class Tile{final int icon,accent;final String title,subtitle;final Runnable action;Tile(int icon,String title,String subtitle,int accent,Runnable action){this.icon=icon;this.title=title;this.subtitle=subtitle;this.accent=accent;this.action=action;}}
    private static final class Metric{final String eyebrow,center,title,detail;final int accent;final Runnable action;Metric(String eyebrow,String center,String title,String detail,int accent,Runnable action){this.eyebrow=eyebrow;this.center=center;this.title=title;this.detail=detail;this.accent=accent;this.action=action;}}
    private static final class Identity{final int playerId,factionId;final String playerName,factionName,position;final boolean factionApi,leader;Identity(int playerId,String playerName,int factionId,String factionName,String position,boolean factionApi,boolean leader){this.playerId=playerId;this.playerName=playerName;this.factionId=factionId;this.factionName=factionName;this.position=position;this.factionApi=factionApi;this.leader=leader;}boolean sameAs(Identity o){return o!=null&&playerId==o.playerId&&factionId==o.factionId&&factionApi==o.factionApi&&leader==o.leader&&playerName.equals(o.playerName)&&factionName.equals(o.factionName)&&position.equals(o.position);}}
}

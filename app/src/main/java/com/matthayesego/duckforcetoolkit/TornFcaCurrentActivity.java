package com.matthayesego.duckforcetoolkit;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

/**
 * Current TornFCA player shell.
 *
 * The authenticated legacy stack remains underneath for proven session/bootstrap behavior, while
 * this activity owns the visible mobile navigation. Top-level pages stay intentionally small and
 * deeper tools are grouped behind clear submenus.
 */
public class TornFcaCurrentActivity extends TornFcaActivity {
    public static final String EXTRA_SECTION="section";
    private static final String FRAME_TAG="tornfca-current-frame";
    // Compatibility references keep older internal launchers/audits pointing at the current IA.
    @SuppressWarnings("unused") private static final String WAR_ROUTE_COMPAT=FeatureRouterActivity.TARGET_WAR;
    @SuppressWarnings("unused") private static final Class<?> MEMBER_INDEX_COMPAT=MemberCenterActivity.class;

    private LinearLayout pageHost;
    private LinearLayout bottomNav;
    private Identity identity;
    private String currentSection="Home";
    private Runnable submenuBack;
    private boolean currentShellInstalled;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){
                if(submenuBack!=null){Runnable back=submenuBack;submenuBack=null;back.run();return;}
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });
    }

    @Override public void setContentView(View view){
        super.setContentView(view);
        ViewGroup root=findViewById(android.R.id.content);
        if(root==null)return;
        forceCurrentVersion(root);
        if(isLegacyAuthenticatedHome(root))installCurrentShell(root);
    }

    @Override protected void onResume(){
        super.onResume();
        ViewGroup root=findViewById(android.R.id.content);
        if(root==null)return;
        forceCurrentVersion(root);
        if(isLegacyAuthenticatedHome(root))installCurrentShell(root);
        else if(isCurrentShell(root))refreshIdentityIfNeeded(root);
    }

    private boolean isCurrentShell(ViewGroup root){
        return root.getChildCount()>0&&FRAME_TAG.equals(root.getChildAt(0).getTag());
    }

    private boolean isLegacyAuthenticatedHome(View root){
        if(root==null)return false;
        if(root instanceof ViewGroup&&isCurrentShell((ViewGroup)root))return false;
        return containsText(root,"Welcome back,")&&containsText(root,"War");
    }

    private void installCurrentShell(ViewGroup host){
        String requested=currentShellInstalled?currentSection:(getIntent()==null?null:getIntent().getStringExtra(EXTRA_SECTION));
        identity=resolveIdentity();
        host.removeAllViews();

        LinearLayout frame=new LinearLayout(this);
        frame.setTag(FRAME_TAG);
        frame.setOrientation(LinearLayout.VERTICAL);
        frame.setBackgroundColor(TornFcaUi.BG);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(TornFcaUi.BG);
        int left=TornFcaUi.dp(this,16),top=TornFcaUi.dp(this,12),right=TornFcaUi.dp(this,16),bottom=TornFcaUi.dp(this,22);
        scroll.setPadding(left,top,right,bottom);
        scroll.setOnApplyWindowInsetsListener((v,i)->{
            v.setPadding(left+i.getSystemWindowInsetLeft(),top+i.getSystemWindowInsetTop(),right+i.getSystemWindowInsetRight(),bottom);
            return i;
        });

        LinearLayout column=new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        addIdentityHeader(column);

        pageHost=new LinearLayout(this);
        pageHost.setOrientation(LinearLayout.VERTICAL);
        column.addView(pageHost,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.addView(column,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        bottomNav=buildBottomNav();
        frame.addView(bottomNav,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        host.addView(frame,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        scroll.requestApplyInsets();
        bottomNav.requestApplyInsets();

        currentShellInstalled=true;
        renderTop(requested);
    }

    private void refreshIdentityIfNeeded(ViewGroup root){
        Identity now=resolveIdentity();
        if(identity==null||!identity.sameAs(now))installCurrentShell(root);
    }

    private Identity resolveIdentity(){
        String key=new SecureApiKeyStore(this).load();
        String player="Member",faction="Faction",position="Member";
        int playerId=0,factionId=0;
        boolean factionApi=false;
        if(key!=null&&!key.isBlank()){
            AuthSession hot=TornApiClient.cachedSession(key);
            if(hot!=null){
                player=clean(hot.playerName,"Member");
                faction=clean(hot.factionName,"Faction");
                position=clean(hot.position,"Member");
                playerId=hot.playerId;
                factionId=hot.factionId;
                factionApi=hot.factionApiAccess;
            }else{
                FactionScopeCache.Scope scope=FactionScopeCache.load(this,key);
                if(scope!=null){
                    player=clean(scope.playerName,"Member");
                    faction=clean(scope.factionName,"Faction");
                    position=clean(scope.position,"Member");
                    playerId=scope.playerId;
                    factionId=scope.factionId;
                    factionApi=scope.factionApiAccess;
                }
            }
        }
        boolean preview=DeveloperPreviewStore.isMemberPreview(this);
        if(preview)position="Member Preview";
        boolean leader=!preview&&AccessPolicy.isLeaderPosition(position);
        return new Identity(playerId,player,factionId,faction,position,factionApi,leader);
    }

    private String clean(String value,String fallback){
        return value==null||value.trim().isEmpty()?fallback:value.trim();
    }

    private void addIdentityHeader(LinearLayout root){
        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(TornFcaUi.dp(this,2),TornFcaUi.dp(this,6),TornFcaUi.dp(this,2),TornFcaUi.dp(this,16));

        ImageView avatar=new ImageView(this);
        avatar.setTag("tornfca-profile-avatar");
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setImageResource(R.drawable.duckforce_noir_art);
        GradientDrawable avatarBg=new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(TornFcaUi.PANEL);
        avatarBg.setStroke(TornFcaUi.dp(this,1),TornFcaUi.GOLD);
        avatar.setBackground(avatarBg);
        avatar.setClipToOutline(true);
        avatar.setOutlineProvider(new ViewOutlineProvider(){
            @Override public void getOutline(View view,Outline outline){
                outline.setOval(0,0,view.getWidth(),view.getHeight());
            }
        });
        header.addView(avatar,new LinearLayout.LayoutParams(TornFcaUi.dp(this,72),TornFcaUi.dp(this,72)));

        LinearLayout copy=new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);

        TextView brand=TornFcaUi.text(this,"TORNFCA • v"+TornFcaBrand.VERSION,9.5f,TornFcaUi.GOLD,true);
        brand.setLetterSpacing(.10f);
        copy.addView(brand);

        TextView name=TornFcaUi.text(this,identity.playerName,27,TornFcaUi.TEXT,true);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        np.topMargin=TornFcaUi.dp(this,3);
        copy.addView(name,np);

        TextView meta=TornFcaUi.text(this,identity.factionName+" • "+identity.position,12.5f,TornFcaUi.MUTED,false);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        mp.topMargin=TornFcaUi.dp(this,3);
        copy.addView(meta,mp);

        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
        cp.leftMargin=TornFcaUi.dp(this,14);
        header.addView(copy,cp);
        root.addView(header,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private LinearLayout buildBottomNav(){
        LinearLayout nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(TornFcaUi.dp(this,5),TornFcaUi.dp(this,7),TornFcaUi.dp(this,5),TornFcaUi.dp(this,6));
        nav.setBackground(TornFcaUi.gradient(this,TornFcaUi.PANEL,TornFcaUi.PANEL2,TornFcaUi.BORDER,18));

        nav.addView(navItem("Home",this::renderHome),navParams());
        nav.addView(navItem("Faction",this::renderFaction),navParams());
        nav.addView(navItem("War",this::renderWar),navParams());
        nav.addView(navItem("Training",this::renderTraining),navParams());
        if(identity!=null&&identity.leader)nav.addView(navItem("Leadership",this::renderLeadership),navParams());
        nav.addView(navItem("More",this::renderMore),navParams());

        int baseBottom=TornFcaUi.dp(this,6);
        nav.setOnApplyWindowInsetsListener((v,i)->{
            v.setPadding(TornFcaUi.dp(this,5),TornFcaUi.dp(this,7),TornFcaUi.dp(this,5),baseBottom+i.getSystemWindowInsetBottom());
            return i;
        });
        return nav;
    }

    private LinearLayout.LayoutParams navParams(){
        return new LinearLayout.LayoutParams(0,TornFcaUi.dp(this,58),1f);
    }

    private TextView navItem(String label,Runnable action){
        TextView item=TornFcaUi.text(this,label,9.4f,TornFcaUi.MUTED,true);
        item.setGravity(Gravity.CENTER);
        item.setMaxLines(1);
        item.setTag("current-nav:"+label);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(v->action.run());
        return item;
    }

    private void selectNav(String label){
        if(bottomNav==null)return;
        for(int i=0;i<bottomNav.getChildCount();i++){
            View v=bottomNav.getChildAt(i);
            if(!(v instanceof TextView))continue;
            TextView t=(TextView)v;
            boolean selected=("current-nav:"+label).equals(t.getTag());
            t.setTextColor(selected?TornFcaUi.GOLD:TornFcaUi.MUTED);
            t.setBackground(selected?TornFcaUi.rounded(this,TornFcaUi.PANEL2,TornFcaUi.GOLD,13):null);
        }
    }

    private void renderTop(String requested){
        String target=requested==null||requested.isBlank()?"Home":requested;
        switch(target){
            case "Faction": renderFaction(); break;
            case "War": renderWar(); break;
            case "Training": renderTraining(); break;
            case "Leadership": if(identity!=null&&identity.leader)renderLeadership(); else renderHome(); break;
            case "More": renderMore(); break;
            default: renderHome();
        }
    }

    private void beginTop(String section,String eye,String title,String subtitle){
        if(pageHost==null)return;
        currentSection=section;
        submenuBack=null;
        pageHost.removeAllViews();
        selectNav(section);
        addPageHeading(eye,title,subtitle);
    }

    private void beginSubmenu(String section,String parentLabel,String eye,String title,String subtitle,Runnable back){
        if(pageHost==null)return;
        currentSection=section;
        submenuBack=back;
        pageHost.removeAllViews();
        selectNav(section);
        Button backButton=TornFcaUi.button(this,"← "+parentLabel,TornFcaUi.BORDER);
        backButton.setOnClickListener(v->{submenuBack=null;back.run();});
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(TornFcaUi.dp(this,150),TornFcaUi.dp(this,42));
        bp.bottomMargin=TornFcaUi.dp(this,14);
        pageHost.addView(backButton,bp);
        addPageHeading(eye,title,subtitle);
    }

    private void addPageHeading(String eye,String title,String subtitle){
        TextView e=TornFcaUi.text(this,eye,10,TornFcaUi.GOLD,true);
        e.setLetterSpacing(.12f);
        pageHost.addView(e);

        TextView h=TornFcaUi.text(this,title,25,TornFcaUi.TEXT,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin=TornFcaUi.dp(this,4);
        pageHost.addView(h,hp);

        TextView sub=TornFcaUi.text(this,subtitle,12.8f,TornFcaUi.MUTED,false);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin=TornFcaUi.dp(this,4);
        sp.bottomMargin=TornFcaUi.dp(this,15);
        pageHost.addView(sub,sp);
    }

    private void renderHome(){
        beginTop("Home","HOME","Your faction companion","A simple starting point. Open My Day for the full personal summary, or jump straight to the one thing you need.");
        addMenu("TODAY","My Day","Bars, cooldowns, OC, chain and warfare readiness in one personal view.",TornFcaUi.GREEN,"Open My Day",()->openActivity(MemberDailyActivity.class));
        addMenu("READY","My War Prep","Use your personal warfare checklist before Ranked War or territory activity.",TornFcaUi.RED,"Open War Prep",()->openActivity(WarPrepActivity.class));
        addMenu("ANNOUNCEMENTS","Faction Announcements","Read current leadership announcements and important faction messages.",TornFcaUi.GOLD,"Open announcements",this::openAnnouncements);
        addMenu("ALERTS","Notification Inbox","Review saved TornFCA alerts after Android clears them.",TornFcaUi.GOLD,"Open alerts",()->openActivity(NotificationInboxActivity.class));
        if(identity!=null&&identity.leader){
            addMenu("LEADERSHIP","Needs Attention","Review the faction members and exceptions that may need action.",TornFcaUi.GOLD,"Review attention",()->openActivity(LeadershipAttentionActivity.class));
        }
        addFooter();
    }

    private void renderFaction(){
        beginTop("Faction","FACTION","Faction","Shared faction information stays here. Extra status and community tools are grouped one level deeper.");
        addMenu("ANNOUNCEMENTS","Faction Announcements","Current leadership announcements stay here until they are deleted.",TornFcaUi.GOLD,"Open announcements",this::openAnnouncements);
        addMenu("OVERVIEW","Faction Overview","Current faction information available to members.",TornFcaUi.GOLD,"Open overview",()->openActivity(MemberFactionActivity.class));
        addMenu("DIRECTORY","Faction Directory","Search the roster and open basic member status cards.",TornFcaUi.BLUE,"Open directory",()->openActivity(MemberDirectoryActivity.class));
        addMenu("RESOURCES","Faction Resources","Onboarding, rules, guides and useful faction shortcuts.",TornFcaUi.GOLD,"Open resources",()->openActivity(FactionResourcesActivity.class));
        addMenu("TOOLS","Faction Tools & Community","OC, chain, strength intel and faction chat.",TornFcaUi.GREEN,"Open faction tools",this::renderFactionTools);
        addFooter();
    }

    private void renderFactionTools(){
        beginSubmenu("Faction","Faction","FACTION TOOLS","Tools & community","Only the less-frequent faction tools live here, keeping the main Faction tab easy to scan.",this::renderFaction);
        addMenu("OC","My Organized Crime","Your assignment and readiness; leadership detail remains permission-gated.",TornFcaUi.PURPLE,"Open My OC",()->openFeature(FeatureRouterActivity.TARGET_OC));
        addMenu("CHAIN","Chain Status","Current chain context and your participation.",TornFcaUi.GREEN,"View chain",()->openFeature(FeatureRouterActivity.TARGET_CHAIN));
        addMenu("INTEL","Faction Strength Intel","Optional FFScouter estimates with consent and freshness controls.",TornFcaUi.BLUE,"View intelligence",()->openFeature(FeatureRouterActivity.TARGET_STRENGTH));
        addMenu("COMMUNITY","Faction Chat",CommunityBackendClient.isConfigured()?"Verified faction chat inside TornFCA.":"Community backend is not configured in this build yet.",TornFcaUi.BLUE,"Open faction chat",()->openActivity(FactionChatActivity.class));
        addFooter();
    }

    private void renderWar(){
        beginTop("War","WAR","War","Ranked War and Territories stay separate so you only open the warfare mode your faction is using.");
        addMenu("RANKED WAR","Ranked War","Current matchup, score, participation, opponent intel and completed war history.",TornFcaUi.RED,"Open Ranked War",()->openScopedActivity(WarCenterActivity.class));
        addMenu("TERRITORIES","Territories","Owned blocks, live assaults, wall status, history and personal territory contribution.",TornFcaUi.GOLD,"Open Territories",()->openScopedActivity(TerritoryWarActivity.class));
        addMenu("READINESS","My War Prep","Personal bars, cooldowns, travel, refills, OC and your warfare checklist.",TornFcaUi.GREEN,"Open War Prep",()->openActivity(WarPrepActivity.class));
        addFooter();
    }

    private void renderTraining(){
        beginTop("Training","TRAINING","Training","Training gets its own home instead of competing for space on the main dashboard.");
        addMenu("GUIDANCE","Training Center","Starter guidance plus faction-published training resources.",TornFcaUi.PURPLE,"Open Training Center",()->openActivity(TrainingCenterActivity.class));
        addMenu("PROGRESS","My Training Progress","Track your own battle-stat and Xanax progress from a device-local baseline.",TornFcaUi.GREEN,"Open My Progress",()->openActivity(TrainingProgressActivity.class));
        if(identity!=null&&identity.leader){
            addMenu("LEADERSHIP","Guide & Training Management","Publish faction-scoped guides and training expectations.",TornFcaUi.GOLD,"Manage guides & training",()->openActivity(TrainingAdminActivity.class));
        }
        addFooter();
    }

    private void renderLeadership(){
        if(identity==null||!identity.leader){renderHome();return;}
        beginTop("Leadership","LEADERSHIP","Leadership","Operational tools are grouped by job instead of presenting every leadership feature at once.");
        addMenu("PRIORITY","Needs Attention","Review inactivity, war gaps, OC gaps and availability exceptions.",TornFcaUi.GOLD,"Review attention",()->openActivity(LeadershipAttentionActivity.class));
        addMenu("PEOPLE","People & Activity","Activity Tracker, Faction Pulse and Member Dossier.",TornFcaUi.BLUE,"Open people tools",this::renderLeadershipPeople);
        addMenu("WAR & MONEY","War & Finance","Ranked-war payouts and faction banking.",TornFcaUi.RED,"Open war & finance",this::renderLeadershipWarMoney);
        addMenu("OPERATIONS","Faction Operations","Armory auditing plus guide and training publishing.",TornFcaUi.GREEN,"Open operations",this::renderLeadershipOperations);
        addFooter();
    }

    private void renderLeadershipPeople(){
        if(identity==null||!identity.leader){renderHome();return;}
        beginSubmenu("Leadership","Leadership","PEOPLE & ACTIVITY","People & activity","Leadership member review and faction participation tools.",this::renderLeadership);
        addMenu("ACTIVITY","Activity Tracker","Faction-wide participation and activity scan.",TornFcaUi.GOLD,"Open Activity Tracker",()->openFeature(FeatureRouterActivity.TARGET_ACTIVITY));
        addMenu("PULSE","Faction Pulse","Member health, inactivity and availability at a glance.",TornFcaUi.GREEN,"Open Faction Pulse",()->openFeature(FeatureRouterActivity.TARGET_PULSE));
        addMenu("MEMBERS","Member Dossier","Leadership member lookup with Torn status and opted-in intelligence.",TornFcaUi.BLUE,"Open Member Dossier",()->openFeature(FeatureRouterActivity.TARGET_LOOKUP));
        addFooter();
    }

    private void renderLeadershipWarMoney(){
        if(identity==null||!identity.leader){renderHome();return;}
        beginSubmenu("Leadership","Leadership","WAR & FINANCE","War & finance","Keep payout and banking work together.",this::renderLeadership);
        addMenu("WAR PAYOUT","Payout Calculator","Review completed Ranked War participation and calculate payouts.",TornFcaUi.RED,"Open payout calculator",()->openFeature(FeatureRouterActivity.TARGET_WAR_PAYOUT));
        addMenu("BANKING","Banking","Faction payout requests and queue review where permissions allow.",TornFcaUi.BLUE,"Open banking",()->openFeature(FeatureRouterActivity.TARGET_BANKING));
        addFooter();
    }

    private void renderLeadershipOperations(){
        if(identity==null||!identity.leader){renderHome();return;}
        beginSubmenu("Leadership","Leadership","FACTION OPERATIONS","Faction operations","Less-frequent administration stays off the main leadership page.",this::renderLeadership);
        addMenu("ARMORY","Armory Auditor","Audit armory items, member totals, deposits and restocks.",TornFcaUi.GREEN,"Open Armory Auditor",this::openArmory);
        addMenu("TRAINING ADMIN","Guide & Training Management","Publish faction-scoped guides and training expectations.",TornFcaUi.PURPLE,"Manage guides & training",()->openActivity(TrainingAdminActivity.class));
        addFooter();
    }

    private void renderMore(){
        beginTop("More","MORE","More","Settings and app information live here; everyday faction tools stay in their own tabs.");
        addMenu("SETTINGS","Settings","Notifications, API-key storage, optional services, privacy controls and account actions.",TornFcaUi.BLUE,"Open Settings",()->openActivity(SettingsActivity.class));
        addMenu("LEGAL","Legal & Privacy","Privacy Policy, Terms & Conditions, EULA and acknowledgement status.",TornFcaUi.PURPLE,"Review legal documents",()->openActivity(LegalActivity.class));
        addMenu("ABOUT","About TornFCA","Version, privacy approach, free/core philosophy and third-party services.",TornFcaUi.BORDER,"About TornFCA",()->openActivity(AboutActivity.class));
        addMenu("PREMIUM","TornFCA Premium","Optional extra history, analytics, automation and convenience.",TornFcaUi.GOLD,"View Premium",()->openActivity(PremiumPreviewActivity.class));
        addFooter();
    }

    private void addMenu(String eye,String title,String body,int accent,String action,Runnable open){
        LinearLayout card=TornFcaUi.card(this,eye,title,body,accent);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v->open.run());

        TextView hint=TornFcaUi.text(this,action+"  →",11.5f,accent,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin=TornFcaUi.dp(this,9);
        card.addView(hint,hp);
        TornFcaUi.add(this,pageHost,card);
    }

    private void addFooter(){
        TextView footer=TornFcaUi.footer(this,"TornFCA v"+TornFcaBrand.VERSION+" • "+(identity==null?"Faction":identity.factionName));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.topMargin=TornFcaUi.dp(this,5);
        fp.bottomMargin=TornFcaUi.dp(this,7);
        pageHost.addView(footer,fp);
    }

    private void openAnnouncements(){
        if(identity==null)identity=resolveIdentity();
        Intent i=new Intent(this,WarNoticeActivity.class);
        i.putExtra(WarNoticeActivity.EXTRA_FACTION_ID,identity.factionId);
        i.putExtra(WarNoticeActivity.EXTRA_FACTION_NAME,identity.factionName);
        i.putExtra(WarNoticeActivity.EXTRA_CAN_PUBLISH,identity.leader);
        startActivity(i);
    }

    private void openActivity(Class<?> target){
        startActivity(new Intent(this,target));
    }

    private void openScopedActivity(Class<?> target){
        Intent i=new Intent(this,target);
        putScope(i);
        startActivity(i);
    }

    private void openFeature(String target){
        Intent i=new Intent(this,FeatureRouterActivity.class);
        i.putExtra(FeatureRouterActivity.EXTRA_TARGET,target);
        startActivity(i);
    }

    private void openArmory(){
        Intent i=new Intent(this,ToolHostActivity.class);
        i.putExtra(ToolHostActivity.EXTRA_TOOL,"ARMORY");
        startActivity(i);
    }

    private void putScope(Intent i){
        if(identity==null)identity=resolveIdentity();
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_ID,identity.factionId);
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_NAME,identity.factionName);
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_API,identity.factionApi);
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_ID,identity.factionId);
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_NAME,identity.factionName);
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_API,identity.factionApi);
        i.putExtra(DeveloperConsoleActivity.EXTRA_POSITION,identity.position);
    }

    private void forceCurrentVersion(View view){
        if(view instanceof TextView){
            TextView t=(TextView)view;
            CharSequence raw=t.getText();
            if(raw!=null){
                String s=raw.toString().replaceAll("v0\\.9\\.\\d+","v"+TornFcaBrand.VERSION);
                if(!s.equals(raw.toString()))t.setText(s);
            }
        }
        if(view instanceof ViewGroup){
            ViewGroup g=(ViewGroup)view;
            for(int i=0;i<g.getChildCount();i++)forceCurrentVersion(g.getChildAt(i));
        }
    }

    private boolean containsText(View view,String needle){
        if(view instanceof TextView){
            CharSequence raw=((TextView)view).getText();
            if(raw!=null&&raw.toString().toLowerCase().contains(needle.toLowerCase()))return true;
        }
        if(view instanceof ViewGroup){
            ViewGroup g=(ViewGroup)view;
            for(int i=0;i<g.getChildCount();i++)if(containsText(g.getChildAt(i),needle))return true;
        }
        return false;
    }

    private static final class Identity{
        final int playerId,factionId;
        final String playerName,factionName,position;
        final boolean factionApi,leader;

        Identity(int playerId,String playerName,int factionId,String factionName,String position,boolean factionApi,boolean leader){
            this.playerId=playerId;
            this.playerName=playerName;
            this.factionId=factionId;
            this.factionName=factionName;
            this.position=position;
            this.factionApi=factionApi;
            this.leader=leader;
        }

        boolean sameAs(Identity other){
            return other!=null
                    &&playerId==other.playerId
                    &&factionId==other.factionId
                    &&leader==other.leader
                    &&safe(playerName).equals(safe(other.playerName))
                    &&safe(factionName).equals(safe(other.factionName))
                    &&safe(position).equals(safe(other.position));
        }

        private static String safe(String value){return value==null?"":value;}
    }
}

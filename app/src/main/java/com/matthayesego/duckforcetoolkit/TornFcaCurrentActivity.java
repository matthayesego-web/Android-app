package com.matthayesego.duckforcetoolkit;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Current TornFCA player shell.
 *
 * The legacy companion classes still provide the proven authentication/session bootstrap underneath,
 * but once the authenticated dashboard exists this activity replaces the old versioned presentation
 * with one consolidated member-first navigation surface. Feature implementations stay in their
 * dedicated activities; this class is only the current front door.
 */
public class TornFcaCurrentActivity extends TornFcaActivity {
    private static final String FRAME_TAG="tornfca-current-frame";
    private LinearLayout pageHost;
    private LinearLayout bottomNav;
    private Identity identity;

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);}

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
        else if(isCurrentShell(root))refreshIdentityAndHomeIfNeeded();
    }

    private boolean isCurrentShell(ViewGroup root){
        return root.getChildCount()>0&&FRAME_TAG.equals(root.getChildAt(0).getTag());
    }

    private boolean isLegacyAuthenticatedHome(View root){
        if(root==null||isCurrentShell((ViewGroup)root))return false;
        return containsText(root,"Welcome back,")&&containsText(root,"War");
    }

    private void installCurrentShell(ViewGroup host){
        identity=resolveIdentity();
        host.removeAllViews();

        LinearLayout frame=new LinearLayout(this);frame.setTag(FRAME_TAG);frame.setOrientation(LinearLayout.VERTICAL);frame.setBackgroundColor(TornFcaUi.BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setBackgroundColor(TornFcaUi.BG);
        int left=TornFcaUi.dp(this,16),top=TornFcaUi.dp(this,16),right=TornFcaUi.dp(this,16),bottom=TornFcaUi.dp(this,22);
        scroll.setPadding(left,top,right,bottom);
        scroll.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(left+i.getSystemWindowInsetLeft(),top+i.getSystemWindowInsetTop(),right+i.getSystemWindowInsetRight(),bottom);return i;});
        pageHost=new LinearLayout(this);pageHost.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(pageHost,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        bottomNav=buildBottomNav();
        frame.addView(bottomNav,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        host.addView(frame,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        scroll.requestApplyInsets();bottomNav.requestApplyInsets();
        renderHome();
    }

    private void refreshIdentityAndHomeIfNeeded(){
        Identity now=resolveIdentity();
        if(identity==null||!identity.sameAs(now)){identity=now;renderHome();}
    }

    private Identity resolveIdentity(){
        String key=new SecureApiKeyStore(this).load();
        String player="Member",faction="Faction",position="Member";int playerId=0,factionId=0;boolean factionApi=false;
        if(key!=null&&!key.isBlank()){
            AuthSession hot=TornApiClient.cachedSession(key);
            if(hot!=null){player=clean(hot.playerName,"Member");faction=clean(hot.factionName,"Faction");position=clean(hot.position,"Member");playerId=hot.playerId;factionId=hot.factionId;factionApi=hot.factionApiAccess;}
            else{
                FactionScopeCache.Scope scope=FactionScopeCache.load(this,key);
                if(scope!=null){player=clean(scope.playerName,"Member");faction=clean(scope.factionName,"Faction");position=clean(scope.position,"Member");playerId=scope.playerId;factionId=scope.factionId;factionApi=scope.factionApiAccess;}
            }
        }
        boolean preview=DeveloperPreviewStore.isMemberPreview(this);
        if(preview)position="Member Preview";
        boolean leader=!preview&&AccessPolicy.isLeaderPosition(position);
        return new Identity(playerId,player,factionId,faction,position,factionApi,leader);
    }

    private String clean(String value,String fallback){return value==null||value.trim().isEmpty()?fallback:value.trim();}

    private LinearLayout buildBottomNav(){
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setGravity(Gravity.CENTER);nav.setPadding(TornFcaUi.dp(this,6),TornFcaUi.dp(this,7),TornFcaUi.dp(this,6),TornFcaUi.dp(this,6));nav.setBackground(TornFcaUi.gradient(this,TornFcaUi.PANEL,TornFcaUi.PANEL2,TornFcaUi.BORDER,18));
        nav.addView(navItem("Home",this::renderHome),navParams());
        nav.addView(navItem(identity!=null&&identity.leader?"Faction":"Member",identity!=null&&identity.leader?this::renderFaction:()->openActivity(MemberCenterActivity.class)),navParams());
        nav.addView(navItem("War",()->openFeature(FeatureRouterActivity.TARGET_WAR)),navParams());
        nav.addView(navItem(identity!=null&&identity.leader?"Leadership":"Faction",identity!=null&&identity.leader?this::renderLeadership:this::renderFaction),navParams());
        nav.addView(navItem("More",()->openActivity(MoreActivity.class)),navParams());
        int baseBottom=TornFcaUi.dp(this,6);nav.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(TornFcaUi.dp(this,6),TornFcaUi.dp(this,7),TornFcaUi.dp(this,6),baseBottom+i.getSystemWindowInsetBottom());return i;});
        return nav;
    }

    private LinearLayout.LayoutParams navParams(){return new LinearLayout.LayoutParams(0,TornFcaUi.dp(this,58),1f);}

    private TextView navItem(String label,Runnable action){
        TextView item=TornFcaUi.text(this,label,10.5f,TornFcaUi.MUTED,true);item.setGravity(Gravity.CENTER);item.setTag("current-nav:"+label);item.setClickable(true);item.setFocusable(true);item.setOnClickListener(v->action.run());return item;
    }

    private void selectNav(String label){
        if(bottomNav==null)return;
        for(int i=0;i<bottomNav.getChildCount();i++){
            View v=bottomNav.getChildAt(i);if(!(v instanceof TextView))continue;TextView t=(TextView)v;boolean selected=("current-nav:"+label).equals(t.getTag());t.setTextColor(selected?TornFcaUi.GOLD:TornFcaUi.MUTED);t.setBackground(selected?TornFcaUi.rounded(this,TornFcaUi.PANEL2,TornFcaUi.GOLD,13):null);
        }
    }

    private void renderHome(){
        if(pageHost==null)return;identity=resolveIdentity();pageHost.removeAllViews();selectNav("Home");
        addDashboardHeader("HOME","Your faction companion","Everything current starts here. Core member tools stay free; leadership tools appear only when your verified faction position allows them.");

        LinearLayout status=TornFcaUi.card(this,"CURRENT BUILD","TornFCA v"+TornFcaBrand.VERSION,"Consolidated dashboard • Ranked War and Territories are separated • Member Center, training, community and leadership routes are all surfaced from the current shell.",TornFcaUi.GREEN);TornFcaUi.add(this,pageHost,status);

        TornFcaUi.addSection(this,pageHost,"Today & warfare");
        addPair(card("TODAY","My Day","Bars, cooldowns, OC, chain and war readiness.",TornFcaUi.GREEN,()->openActivity(MemberDailyActivity.class)),card("WAR CENTER","Ranked + Territories","Choose the warfare mode your faction is using.",TornFcaUi.RED,()->openFeature(FeatureRouterActivity.TARGET_WAR)));
        addPair(card("WAR PREP","My War Prep","Personal readiness checklist before faction warfare.",TornFcaUi.GOLD,()->openActivity(WarPrepActivity.class)),card("ORGANIZED CRIME","My OC","Your assignment and readiness; leadership scope stays gated.",TornFcaUi.PURPLE,()->openFeature(FeatureRouterActivity.TARGET_OC)));

        TornFcaUi.addSection(this,pageHost,"Faction life");
        addPair(card("CHAIN","Chain Status","Current faction chain and your participation.",TornFcaUi.BLUE,()->openFeature(FeatureRouterActivity.TARGET_CHAIN)),card("MEMBER CENTER","All member tools","Training, resources, directory, chat, alerts and more.",TornFcaUi.GREEN,()->openActivity(MemberCenterActivity.class)));
        addPair(card("TRAINING","Training Center","Starter guidance plus faction-published training resources.",TornFcaUi.PURPLE,()->openActivity(TrainingCenterActivity.class)),card("PROGRESS","My Training Progress","Device-local battle-stat and Xanax progress tracking.",TornFcaUi.GREEN,()->openActivity(TrainingProgressActivity.class)));

        TornFcaUi.addSection(this,pageHost,"Community & alerts");
        String chatBody=CommunityBackendClient.isConfigured()?"Verified faction chat inside TornFCA.":"Community backend is not configured in this build; the screen explains what is unavailable.";
        addPair(card("COMMUNITY","Faction Chat",chatBody,TornFcaUi.BLUE,()->openActivity(FactionChatActivity.class)),card("ALERTS","Notification Inbox","Review saved TornFCA alerts after Android clears them.",TornFcaUi.GOLD,()->openActivity(NotificationInboxActivity.class)));

        if(identity.leader){
            TornFcaUi.addSection(this,pageHost,"Leadership");
            LinearLayout lead=TornFcaUi.card(this,"LEADERSHIP","Leadership Center","Activity Tracker, Faction Pulse, Member Dossier, war payout, banking, armory and faction publishing are grouped together.",TornFcaUi.GOLD);lead.setClickable(true);lead.setFocusable(true);lead.setOnClickListener(v->renderLeadership());TornFcaUi.add(this,pageHost,lead);
        }
        addFooter();
    }

    private void renderFaction(){
        if(pageHost==null)return;identity=resolveIdentity();pageHost.removeAllViews();selectNav("Faction");
        addDashboardHeader("FACTION","Faction hub","Shared faction information and member tools, organized without duplicating the full Member Center.");
        addPair(card("OVERVIEW","Faction Overview","Current faction information available to members.",TornFcaUi.GOLD,()->openActivity(MemberFactionActivity.class)),card("DIRECTORY","Faction Directory","Search the roster and open basic member status cards.",TornFcaUi.BLUE,()->openActivity(MemberDirectoryActivity.class)));
        addPair(card("RESOURCES","Faction Resources","Onboarding, rules, guides and useful faction shortcuts.",TornFcaUi.GOLD,()->openActivity(FactionResourcesActivity.class)),card("COMMUNITY","Faction Chat",CommunityBackendClient.isConfigured()?"Verified members of your current faction.":"Unavailable until the community backend is configured.",TornFcaUi.BLUE,()->openActivity(FactionChatActivity.class)));
        addPair(card("INTEL","Faction Strength Intel","Optional FFScouter estimates with consent and freshness controls.",TornFcaUi.BLUE,()->openFeature(FeatureRouterActivity.TARGET_STRENGTH)),card("OC","Organized Crime","Your OC plus leadership detail only when permission allows.",TornFcaUi.PURPLE,()->openFeature(FeatureRouterActivity.TARGET_OC)));
        addPair(card("CHAIN","Chain Status","Current chain context and participation.",TornFcaUi.GREEN,()->openFeature(FeatureRouterActivity.TARGET_CHAIN)),card("TRAINING","Training Center","Faction training guidance and member resources.",TornFcaUi.PURPLE,()->openActivity(TrainingCenterActivity.class)));
        if(identity.leader){LinearLayout manage=TornFcaUi.card(this,"LEADERSHIP","Manage Guides & Training","Publish faction-scoped guides and training expectations through the community backend.",TornFcaUi.GOLD);manage.setClickable(true);manage.setOnClickListener(v->openActivity(TrainingAdminActivity.class));TornFcaUi.add(this,pageHost,manage);}
        addFooter();
    }

    private void renderLeadership(){
        if(pageHost==null)return;identity=resolveIdentity();if(!identity.leader){renderHome();return;}pageHost.removeAllViews();selectNav("Leadership");
        addDashboardHeader("LEADERSHIP","Leadership center","Exception-first operational tools. Member Preview keeps the same boundary as a normal member.");
        LinearLayout attention=TornFcaUi.card(this,"PRIORITY","Leadership Attention","Review inactivity, war gaps, OC gaps and availability exceptions in one place.",TornFcaUi.GOLD);attention.setClickable(true);attention.setOnClickListener(v->openActivity(LeadershipAttentionActivity.class));TornFcaUi.add(this,pageHost,attention);
        TornFcaUi.addSection(this,pageHost,"Participation & readiness");
        addPair(card("ACTIVITY","Activity Tracker","Faction-wide participation and activity scan.",TornFcaUi.GOLD,()->openFeature(FeatureRouterActivity.TARGET_ACTIVITY)),card("PULSE","Faction Pulse","Member health, inactivity and availability at a glance.",TornFcaUi.GREEN,()->openFeature(FeatureRouterActivity.TARGET_PULSE)));
        TornFcaUi.addSection(this,pageHost,"Member & warfare operations");
        addPair(card("MEMBERS","Member Dossier","Leadership member lookup with Torn status and opted-in intelligence.",TornFcaUi.BLUE,()->openFeature(FeatureRouterActivity.TARGET_LOOKUP)),card("WAR PAYOUT","Payout Calculator","Review completed ranked-war participation and calculate payouts.",TornFcaUi.RED,()->openFeature(FeatureRouterActivity.TARGET_WAR_PAYOUT)));
        addPair(card("BANKING","Banking","Faction payout requests and queue review where permissions allow.",TornFcaUi.BLUE,()->openFeature(FeatureRouterActivity.TARGET_BANKING)),card("ARMORY","Armory Auditor","Audit armory items, member totals, deposits and restocks.",TornFcaUi.GREEN,this::openArmory));
        TornFcaUi.addSection(this,pageHost,"Faction publishing");
        LinearLayout publish=TornFcaUi.card(this,"TRAINING ADMIN","Guide & Training Management","Publish faction-scoped guides and training expectations. Server-side writes re-verify leadership.",TornFcaUi.PURPLE);publish.setClickable(true);publish.setOnClickListener(v->openActivity(TrainingAdminActivity.class));TornFcaUi.add(this,pageHost,publish);
        addFooter();
    }

    private void addDashboardHeader(String eye,String title,String subtitle){
        TextView brand=TornFcaUi.text(this,"TORNFCA • v"+TornFcaBrand.VERSION,10,TornFcaUi.GOLD,true);brand.setLetterSpacing(.11f);pageHost.addView(brand);
        TextView welcome=TornFcaUi.text(this,identity.playerName,30,TornFcaUi.TEXT,true);LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);wp.topMargin=TornFcaUi.dp(this,5);pageHost.addView(welcome,wp);
        TextView meta=TornFcaUi.text(this,identity.factionName+" • "+identity.position,12.5f,TornFcaUi.MUTED,false);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);mp.topMargin=TornFcaUi.dp(this,2);mp.bottomMargin=TornFcaUi.dp(this,14);pageHost.addView(meta,mp);
        TextView section=TornFcaUi.text(this,eye,10,TornFcaUi.GOLD,true);section.setLetterSpacing(.12f);pageHost.addView(section);
        TextView heading=TornFcaUi.text(this,title,24,TornFcaUi.TEXT,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=TornFcaUi.dp(this,3);pageHost.addView(heading,hp);
        TextView sub=TornFcaUi.text(this,subtitle,12.8f,TornFcaUi.MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=TornFcaUi.dp(this,4);sp.bottomMargin=TornFcaUi.dp(this,16);pageHost.addView(sub,sp);
    }

    private LinearLayout card(String eye,String title,String body,int accent,Runnable action){
        LinearLayout c=TornFcaUi.card(this,eye,title,body,accent);c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->action.run());return c;
    }

    private void addPair(LinearLayout first,LinearLayout second){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setBaselineAligned(false);
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f);LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f);b.leftMargin=TornFcaUi.dp(this,9);row.addView(first,a);row.addView(second,b);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rp.bottomMargin=TornFcaUi.dp(this,10);pageHost.addView(row,rp);
    }

    private void addFooter(){
        TextView footer=TornFcaUi.footer(this,"TornFCA v"+TornFcaBrand.VERSION+" • current consolidated shell • faction-scoped access");LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=TornFcaUi.dp(this,8);fp.bottomMargin=TornFcaUi.dp(this,8);pageHost.addView(footer,fp);
    }

    private void openActivity(Class<?> target){startActivity(new Intent(this,target));}
    private void openFeature(String target){Intent i=new Intent(this,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,target);startActivity(i);}
    private void openArmory(){Intent i=new Intent(this,ToolHostActivity.class);i.putExtra(ToolHostActivity.EXTRA_TOOL,"ARMORY");startActivity(i);}

    private void forceCurrentVersion(View view){
        if(view instanceof TextView){TextView t=(TextView)view;CharSequence raw=t.getText();if(raw!=null){String s=raw.toString().replaceAll("v0\\.9\\.\\d+","v"+TornFcaBrand.VERSION);if(!s.equals(raw.toString()))t.setText(s);}}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)forceCurrentVersion(g.getChildAt(i));}
    }

    private boolean containsText(View view,String needle){
        if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&raw.toString().toLowerCase().contains(needle.toLowerCase()))return true;}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)if(containsText(g.getChildAt(i),needle))return true;}
        return false;
    }

    private static final class Identity{
        final int playerId,factionId;final String playerName,factionName,position;final boolean factionApi,leader;
        Identity(int playerId,String playerName,int factionId,String factionName,String position,boolean factionApi,boolean leader){this.playerId=playerId;this.playerName=playerName;this.factionId=factionId;this.factionName=factionName;this.position=position;this.factionApi=factionApi;this.leader=leader;}
        boolean sameAs(Identity o){return o!=null&&playerId==o.playerId&&factionId==o.factionId&&leader==o.leader&&safe(playerName).equals(safe(o.playerName))&&safe(factionName).equals(safe(o.factionName))&&safe(position).equals(safe(o.position));}
        private static String safe(String s){return s==null?"":s;}
    }
}

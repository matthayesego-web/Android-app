package com.matthayesego.duckforcetoolkit;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * Premium faction-OS shell.
 * The working legacy feature handlers remain underneath, but the visible app is rebuilt around
 * Home / Faction / War / Leadership / More with no duplicate top tabs.
 */
public class V095CompanionActivity extends V090CompanionActivity {
    private static final String VERSION = "0.9.15";
    private static final int BG=Color.rgb(5,8,12), SURFACE=Color.rgb(12,18,26), SURFACE_2=Color.rgb(8,13,20), BORDER=Color.rgb(36,47,61);
    private static final int TEXT=Color.rgb(246,248,251), MUTED=Color.rgb(145,155,169), GOLD=Color.rgb(241,190,86), GOLD_DARK=Color.rgb(122,84,27);
    private static final int BLUE=Color.rgb(82,153,235), GREEN=Color.rgb(76,190,102), RED=Color.rgb(239,88,82);

    private View legacyRoot;
    private LinearLayout pageHost;
    private LinearLayout bottomNav;
    private ImageView avatarView;
    private boolean leadershipAvailable;
    private String playerName="Member", factionName="Duck Force", position="Member";
    private int factionId=0;
    private TextView warEyebrow,warTitle,warMeta,warStatus;
    private int footerTapCount=0;
    private long lastFooterTap=0L;

    @Override public void setContentView(View view){
        boolean home=containsIgnoreCase(view,"Welcome back,");
        super.setContentView(view);
        stampVersion(view);
        if(!home)return;

        ViewGroup host=findViewById(android.R.id.content);
        if(host==null||host.getChildCount()==0)return;
        View current=host.getChildAt(0);
        if(current!=null&&"premium-v097-frame".equals(current.getTag()))return;
        legacyRoot=current;
        inferIdentity(legacyRoot);
        leadershipAvailable=!DeveloperPreviewStore.isMemberPreview(this)
                &&(containsExact(legacyRoot,"Leadership")||containsIgnoreCase(legacyRoot,"LEADERSHIP CONTROLS")||containsIgnoreCase(legacyRoot,"ARMORY AUDITOR"));
        buildPremiumShell(host);
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int a,int b,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable oval(int fill,int stroke){GradientDrawable d=new GradientDrawable();d.setShape(GradientDrawable.OVAL);d.setColor(fill);if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return t;}
    private TextView eyebrow(String value,int color){TextView t=text(value,9.5f,color,true);t.setLetterSpacing(.12f);return t;}

    private void inferIdentity(View root){
        TextView welcome=findContainingIgnoreCase(root,"Welcome back,");
        if(welcome!=null){String raw=welcome.getText().toString();int comma=raw.indexOf(',');if(comma>=0&&comma+1<raw.length())playerName=raw.substring(comma+1).trim();}
        TextView meta=findContainingIgnoreCase(root,"Duck Force •");
        if(meta!=null){String[] bits=meta.getText().toString().split(" • ");if(bits.length>0&&!bits[0].trim().isEmpty())factionName=bits[0].trim();if(bits.length>1&&!bits[1].trim().isEmpty())position=bits[1].trim();}
        if(DeveloperPreviewStore.isMemberPreview(this))position="Member Preview";
    }

    private void buildPremiumShell(ViewGroup host){
        host.removeAllViews();
        LinearLayout frame=new LinearLayout(this);frame.setTag("premium-v097-frame");frame.setOrientation(LinearLayout.VERTICAL);frame.setBackgroundColor(BG);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setBackgroundColor(BG);
        int l=dp(18),t=dp(12),r=dp(18),b=dp(22);scroll.setPadding(l,t,r,b);
        scroll.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b);return i;});
        pageHost=new LinearLayout(this);pageHost.setOrientation(LinearLayout.VERTICAL);scroll.addView(pageHost,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        bottomNav=buildBottomNav();frame.addView(bottomNav,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        host.addView(frame,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        scroll.requestApplyInsets();bottomNav.requestApplyInsets();
        renderHome();
        loadAvatar();
    }

    private LinearLayout buildBottomNav(){
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(6),dp(7),dp(6),dp(6));nav.setElevation(dp(12));
        nav.setBackground(gradient(Color.rgb(15,22,31),Color.rgb(7,11,17),BORDER,22));
        nav.addView(navItem(R.drawable.ic_nav_home,"Home",()->renderHome()),navParams());
        nav.addView(navItem(R.drawable.ic_nav_faction,"Faction",()->renderFaction()),navParams());
        nav.addView(navItem(R.drawable.ic_nav_war,"War",()->openFeature(FeatureRouterActivity.TARGET_WAR)),navParams());
        if(leadershipAvailable)nav.addView(navItem(R.drawable.ic_nav_leadership,"Leadership",()->renderLeadership()),navParams());
        nav.addView(navItem(R.drawable.ic_nav_more,"More",()->startActivity(new Intent(this,MoreActivity.class))),navParams());
        int baseBottom=dp(6);nav.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(dp(6),dp(7),dp(6),baseBottom+i.getSystemWindowInsetBottom());return i;});
        return nav;
    }

    private LinearLayout navItem(int iconRes,String label,Runnable action){
        LinearLayout item=new LinearLayout(this);item.setTag("nav:"+label);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setPadding(dp(3),dp(3),dp(3),dp(3));item.setClickable(true);item.setFocusable(true);
        ImageView icon=new ImageView(this);icon.setImageResource(iconRes);icon.setColorFilter(MUTED);icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);item.addView(icon,new LinearLayout.LayoutParams(dp(26),dp(26)));
        TextView name=text(label,9.5f,MUTED,true);name.setGravity(Gravity.CENTER);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(22));np.topMargin=dp(2);item.addView(name,np);
        item.setOnClickListener(v->action.run());return item;
    }
    private LinearLayout.LayoutParams navParams(){return new LinearLayout.LayoutParams(0,dp(58),1f);}
    private void selectNav(String label){if(bottomNav==null)return;for(int i=0;i<bottomNav.getChildCount();i++){View v=bottomNav.getChildAt(i);if(!(v instanceof LinearLayout))continue;LinearLayout item=(LinearLayout)v;boolean on=("nav:"+label).equals(item.getTag());int color=on?GOLD:MUTED;item.setBackground(on?gradient(Color.rgb(45,34,17),Color.rgb(18,20,22),GOLD_DARK,16):Color.TRANSPARENT==0?null:null);if(item.getChildCount()>0&&item.getChildAt(0)instanceof ImageView)((ImageView)item.getChildAt(0)).setColorFilter(color);if(item.getChildCount()>1&&item.getChildAt(1)instanceof TextView)((TextView)item.getChildAt(1)).setTextColor(color);}}

    private void renderHome(){
        if(pageHost==null)return;pageHost.removeAllViews();selectNav("Home");
        addHomeHeader(pageHost);
        addBrandDivider(pageHost);
        addWarHero(pageHost);

        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout chain=compactCard("CHAIN STATUS","Current chain readiness",GREEN,()->openFeature(FeatureRouterActivity.TARGET_CHAIN));
        LinearLayout obligations=compactCard("MY OBLIGATIONS","War, OC and personal faction tasks",GOLD,()->openMember(MemberFactionActivity.MODE_OVERVIEW));
        row.addView(chain,weighted(false));row.addView(obligations,weighted(true));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(144));rp.bottomMargin=dp(12);pageHost.addView(row,rp);

        if(leadershipAvailable){
            LinearLayout attention=premiumCard("LEADERSHIP ATTENTION","Who needs attention right now","Review war participation, OC gaps, inactivity and availability exceptions.",GOLD,()->startActivity(new Intent(this,LeadershipAttentionActivity.class)));
            LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(122));ap.bottomMargin=dp(12);pageHost.addView(attention,ap);
        }

        LinearLayout digest=premiumCard("WHILE YOU WERE AWAY","Faction digest","See your latest faction status and anything that needs your action without hunting through separate tools.",BLUE,()->openMember(MemberFactionActivity.MODE_OVERVIEW));
        LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(112));dpv.bottomMargin=dp(14);pageHost.addView(digest,dpv);
        addDeveloperFooter(pageHost);
        loadWarStatus();
    }

    private void addHomeHeader(LinearLayout root){
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(2),dp(10),dp(2),dp(12));
        avatarView=new ImageView(this);avatarView.setTag("tornfca-profile-avatar");avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);avatarView.setImageResource(R.drawable.duckforce_noir_art);avatarView.setBackground(oval(Color.rgb(17,23,31),GOLD_DARK));avatarView.setClipToOutline(true);avatarView.setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View view,Outline outline){outline.setOval(0,0,view.getWidth(),view.getHeight());}});
        header.addView(avatarView,new LinearLayout.LayoutParams(dp(78),dp(78)));
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);TextView welcome=text("Welcome back,",13,MUTED,false);copy.addView(welcome);TextView name=text(playerName,28,TEXT,true);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);np.topMargin=dp(1);copy.addView(name,np);TextView role=text(factionName+"  •  "+position,12,DeveloperPreviewStore.isMemberPreview(this)?BLUE:GOLD,false);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rp.topMargin=dp(4);copy.addView(role,rp);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);cp.leftMargin=dp(14);header.addView(copy,cp);root.addView(header,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addBrandDivider(LinearLayout root){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);TextView brand=eyebrow("DUCK FORCE COMPANION",MUTED);row.addView(brand);View line=new View(this);line.setBackgroundColor(Color.rgb(29,38,49));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(1),1f);lp.leftMargin=dp(10);lp.rightMargin=dp(10);row.addView(line,lp);TextView version=text("v"+VERSION+" preview",10,MUTED,false);row.addView(version);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(30));p.bottomMargin=dp(8);root.addView(row,p);}

    private void addWarHero(LinearLayout root){
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(20),dp(18),dp(20),dp(18));hero.setBackground(gradient(Color.rgb(22,28,38),Color.rgb(9,14,21),BORDER,23));hero.setElevation(dp(2));
        warEyebrow=eyebrow("CURRENT WAR",GOLD);hero.addView(warEyebrow);warTitle=text("Checking war status…",26,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(7);hero.addView(warTitle,tp);warMeta=text("Loading Torn ranked-war data",13,MUTED,false);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);mp.topMargin=dp(5);hero.addView(warMeta,mp);warStatus=text("",13,GOLD,true);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(12);hero.addView(warStatus,sp);
        TextView open=text("OPEN WAR CENTER   →",12,GOLD,true);open.setGravity(Gravity.CENTER);open.setBackground(gradient(Color.rgb(42,32,16),Color.rgb(24,20,15),GOLD_DARK,12));open.setClickable(true);open.setFocusable(true);open.setOnClickListener(v->openFeature(FeatureRouterActivity.TARGET_WAR));LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));op.topMargin=dp(16);hero.addView(open,op);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(220));hp.bottomMargin=dp(12);root.addView(hero,hp);
    }

    private LinearLayout compactCard(String title,String body,int accent,Runnable action){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(gradient(SURFACE,SURFACE_2,BORDER,20));c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->action.run());TextView e=eyebrow(title,accent);c.addView(e);TextView b=text(body,13,TEXT,true);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(9);c.addView(b,bp);TextView hint=text("Tap to open  →",10,MUTED,false);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(10);c.addView(hint,hp);return c;}
    private LinearLayout.LayoutParams weighted(boolean margin){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f);if(margin)p.leftMargin=dp(10);return p;}

    private LinearLayout premiumCard(String eye,String title,String body,int accent,Runnable action){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(18),dp(14),dp(18),dp(14));c.setBackground(gradient(SURFACE,SURFACE_2,BORDER,20));c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->action.run());c.addView(eyebrow(eye,accent));TextView h=text(title,20,TEXT,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(5);c.addView(h,hp);TextView b=text(body,12.5f,MUTED,false);b.setMaxLines(2);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(4);c.addView(b,bp);return c;}

    private void renderFaction(){
        pageHost.removeAllViews();selectNav("Faction");addPageTitle(pageHost,"FACTION","Your faction hub","Personal faction status, shared tools and strength intelligence without dashboard clutter.");
        LinearLayout strength=premiumCard("FFSCOUTER INTEL","Faction Strength Intel","Estimated battle stats, Fair Fight, source and freshness using your own opted-in Torn key.",BLUE,()->openFeature(FeatureRouterActivity.TARGET_STRENGTH));addFull(pageHost,strength,128);
        addSection(pageHost,"MY FACTION STATUS");
        addFull(pageHost,premiumCard("PERSONAL","My Status","A single place for your OC, war participation, chain context and current obligations.",GOLD,()->openMember(MemberFactionActivity.MODE_OVERVIEW)),112);
        LinearLayout pair=new LinearLayout(this);pair.setOrientation(LinearLayout.HORIZONTAL);pair.addView(compactCard("MY OC","Assignment and readiness",BLUE,()->openMember(MemberFactionActivity.MODE_OC)),weighted(false));pair.addView(compactCard("PARTICIPATION","My ranked-war activity",GREEN,()->openMember(MemberFactionActivity.MODE_PARTICIPATION)),weighted(true));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(140));pp.bottomMargin=dp(14);pageHost.addView(pair,pp);
        if(containsIgnoreCase(legacyRoot,"BANKING")){addSection(pageHost,"SHARED TOOLS");addFull(pageHost,premiumCard("FACTION OPERATIONS","Banking","Request faction payouts and review the shared request queue when your permissions allow it.",BLUE,()->invokeLegacy("BANKING")),116);}
        addDeveloperFooter(pageHost);
    }

    private void renderLeadership(){
        if(!leadershipAvailable){renderHome();return;}pageHost.removeAllViews();selectNav("Leadership");addPageTitle(pageHost,"LEADERSHIP","Command center","Exceptions first: who needs attention, why they need it, and the tools required to act.");
        addFull(pageHost,premiumCard("PRIORITY","Leadership Attention","War-hit gaps, missing OC assignments, inactivity and availability exceptions in one view.",GOLD,()->startActivity(new Intent(this,LeadershipAttentionActivity.class))),124);
        addSection(pageHost,"MEMBERS");
        addFull(pageHost,premiumCard("MEMBER INTELLIGENCE","Member Dossier","Leadership-focused member lookup with current Torn status and opted-in battle intelligence.",BLUE,()->openFeature(FeatureRouterActivity.TARGET_LOOKUP)),120);
        addSection(pageHost,"WAR & OC");
        LinearLayout pair=new LinearLayout(this);pair.setOrientation(LinearLayout.HORIZONTAL);pair.addView(compactCard("WAR","Participation command",RED,()->openFeature(FeatureRouterActivity.TARGET_WAR)),weighted(false));pair.addView(compactCard("OC","OC management",BLUE,()->openFeature(FeatureRouterActivity.TARGET_OC)),weighted(true));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(140));pp.bottomMargin=dp(14);pageHost.addView(pair,pp);
        addSection(pageHost,"OPERATIONS");
        if(containsIgnoreCase(legacyRoot,"BANKING"))addFull(pageHost,premiumCard("OPERATIONS","Banking","Faction payout requests, queue review and reconciliation tools.",BLUE,()->invokeLegacy("BANKING")),112);
        if(containsIgnoreCase(legacyRoot,"ARMORY AUDITOR"))addFull(pageHost,premiumCard("OPERATIONS","Armory Auditor","Audit faction armory items, member totals, deposits, restocks and detailed activity.",GREEN,()->invokeLegacy("ARMORY AUDITOR")),118);
        if(containsIgnoreCase(legacyRoot,"LEADERSHIP CONTROLS"))addFull(pageHost,premiumCard("ADMINISTRATION","Leadership Controls","Faction permissions, listener guidance and administration.",GOLD,()->invokeLegacy("LEADERSHIP CONTROLS")),112);
        addDeveloperFooter(pageHost);
    }

    private void addPageTitle(LinearLayout root,String eye,String title,String body){LinearLayout h=new LinearLayout(this);h.setOrientation(LinearLayout.VERTICAL);h.setPadding(dp(2),dp(18),dp(2),dp(18));h.addView(eyebrow(eye,GOLD));TextView t=text(title,30,TEXT,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);tp.topMargin=dp(5);h.addView(t,tp);TextView b=text(body,13,MUTED,false);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(6);h.addView(b,bp);root.addView(h);}
    private void addSection(LinearLayout root,String value){TextView s=eyebrow(value,MUTED);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(5);p.bottomMargin=dp(8);root.addView(s,p);}
    private void addFull(LinearLayout root,View card,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(height));p.bottomMargin=dp(12);root.addView(card,p);}

    private void addDeveloperFooter(LinearLayout root){TextView footer=text("Duck Force Companion v"+VERSION+" preview",10.5f,MUTED,false);footer.setGravity(Gravity.CENTER);footer.setPadding(dp(6),dp(15),dp(6),dp(18));footer.setClickable(true);footer.setOnClickListener(v->{long now=System.currentTimeMillis();if(now-lastFooterTap>1500L)footerTapCount=0;lastFooterTap=now;if(++footerTapCount>=3){footerTapCount=0;startActivity(new Intent(this,DeveloperGateActivity.class));}});root.addView(footer,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));}

    private void loadWarStatus(){String key=new SecureApiKeyStore(this).load();if(key==null)return;new Thread(()->{try{int id=factionId;AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null&&hot.factionId>0)id=hot.factionId;if(id<=0){FactionScopeCache.Scope scope=FactionScopeCache.load(this,key);if(scope!=null)id=scope.factionId;}if(id<=0){JSONObject factionRoot=TornApiClient.getJson("/user/faction",key);JSONObject faction=factionRoot.optJSONObject("faction");id=faction==null?0:faction.optInt("id",0);}if(id>0)factionId=id;final int resolvedId=id;WarStatus status=WarStatus.from(TornApiClient.getJson("/faction/wars",key),resolvedId);long now=System.currentTimeMillis()/1000L;runOnUiThread(()->applyWarStatus(status,now));}catch(Exception e){runOnUiThread(()->{if(warTitle!=null){warEyebrow.setText("WAR STATUS");warEyebrow.setTextColor(MUTED);warTitle.setText("War status unavailable");warMeta.setText("Open War Center to retry live Torn data.");warStatus.setText("");}});}}).start();}
    private void applyWarStatus(WarStatus status,long now){if(warTitle==null)return;if(!status.present){warEyebrow.setText("WAR STATUS");warEyebrow.setTextColor(GREEN);warTitle.setText("No Ranked War Scheduled");warMeta.setText("Duck Force has no current or upcoming ranked war.");warStatus.setText("Ready for the next operation");warStatus.setTextColor(GREEN);}else if(status.isUpcoming(now)){warEyebrow.setText("UPCOMING WAR");warEyebrow.setTextColor(GOLD);warTitle.setText("War Starts Soon");warMeta.setText("vs "+status.opponent+(status.target>0?"  •  target "+status.target:""));warStatus.setText("Starts in "+WarStatus.duration(status.start-now));warStatus.setTextColor(GOLD);}else if(status.isLive(now)){warEyebrow.setText("CURRENT WAR");warEyebrow.setTextColor(RED);warTitle.setText("War In Progress");warMeta.setText("vs "+status.opponent+"  •  "+status.ourScore+" – "+status.opponentScore);warStatus.setText(WarStatus.duration(now-status.start)+" elapsed");warStatus.setTextColor(RED);}else{warEyebrow.setText("LATEST WAR");warEyebrow.setTextColor(MUTED);warTitle.setText("Latest Ranked War Ended");warMeta.setText(status.opponent+"  •  final "+status.ourScore+" – "+status.opponentScore);warStatus.setText("Open War Center for participation details");warStatus.setTextColor(MUTED);}}

    /** Public TornFCA restores the profile image after this shell is installed. */
    private void loadAvatar(){/* TornFcaActivity owns the async cached avatar loader. */}

    private void openFeature(String target){Intent i=new Intent(this,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,target);startActivity(i);}
    private void openMember(String mode){Intent i=new Intent(this,MemberFactionActivity.class);i.putExtra(MemberFactionActivity.EXTRA_MODE,mode);startActivity(i);}
    private void invokeLegacy(String title){if(legacyRoot==null||!performClickableAncestor(findContainingIgnoreCase(legacyRoot,title))){Toast.makeText(this,title+" is unavailable for this account.",Toast.LENGTH_SHORT).show();}}
    private boolean performClickableAncestor(View start){View v=start;while(v!=null){if(v.isClickable()&&v.performClick())return true;if(!(v.getParent() instanceof View))break;v=(View)v.getParent();}return false;}

    private void stampVersion(View view){if(view instanceof TextView){TextView t=(TextView)view;CharSequence raw=t.getText();if(raw!=null){String v=raw.toString().replace("v0.8.0","v"+VERSION).replace("v0.9.0","v"+VERSION).replace("v0.9.1","v"+VERSION).replace("v0.9.2","v"+VERSION).replace("v0.9.3","v"+VERSION).replace("v0.9.4","v"+VERSION).replace("v0.9.5","v"+VERSION).replace("v0.9.6","v"+VERSION).replace("v0.9.7","v"+VERSION).replace("v0.9.8","v"+VERSION).replace("v0.9.9","v"+VERSION).replace("v0.9.10","v"+VERSION).replace("v0.9.11","v"+VERSION).replace("v0.9.12","v"+VERSION).replace("v0.9.13","v"+VERSION).replace("v0.9.14","v"+VERSION);if(!v.equals(raw.toString()))t.setText(v);}}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)stampVersion(g.getChildAt(i));}}
    private TextView findExact(View view,String exact){if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&exact.equals(raw.toString()))return(TextView)view;}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView f=findExact(g.getChildAt(i),exact);if(f!=null)return f;}}return null;}
    private boolean containsExact(View view,String exact){return findExact(view,exact)!=null;}
    private TextView findContainingIgnoreCase(View view,String needle){if(view==null)return null;String n=needle.toLowerCase();if(view instanceof TextView){CharSequence raw=((TextView)view).getText();if(raw!=null&&raw.toString().toLowerCase().contains(n))return(TextView)view;}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView f=findContainingIgnoreCase(g.getChildAt(i),needle);if(f!=null)return f;}}return null;}
    private boolean containsIgnoreCase(View view,String needle){return findContainingIgnoreCase(view,needle)!=null;}
}

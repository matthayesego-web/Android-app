package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

/** Thin wrappers keep proven feature logic intact while applying TornFCA branding and faction theming. */
public final class TornFcaScreens {
    private TornFcaScreens(){}
    private static void brand(Activity a){ViewGroup root=a.findViewById(android.R.id.content);if(root!=null)TornFcaBrand.apply(a,root);}
    private static void brand(Activity a,View v){brand(a);}
    private static Intent route(Activity a,Intent i){return TornFcaBrand.retarget(a,i);}

    public static class More extends MoreActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class About extends AboutActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class FeatureRouter extends FeatureRouterActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class WarCenter extends WarCenterActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class WarPayout extends WarPayoutActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class BankingCompanion extends BankingCompanionActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class DeveloperGate extends DeveloperGateActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class DeveloperPanel extends DeveloperPanelActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class PremiumAdmin extends PremiumAdminActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class LeadershipAttention extends LeadershipAttentionActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class FactionStrength extends FactionStrengthActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class MemberFaction extends MemberFactionActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class WarNotice extends WarNoticeActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class FactionOps extends FactionOpsActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class OcTracker extends OcTrackerActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class QuickIntel extends QuickIntelActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class PremiumPreview extends PremiumPreviewActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
    public static class DeveloperConsole extends DeveloperConsoleActivity { @Override public void setContentView(View v){super.setContentView(v);brand(this,v);} @Override public void startActivity(Intent i){super.startActivity(route(this,i));} }
}

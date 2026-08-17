package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Simple member-facing split between ranked warfare and territory warfare. */
public class WarHubActivity extends Activity {
    private int factionId;
    private String factionName,position;
    private boolean factionApi;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        factionId=getIntent().getIntExtra(FactionOpsActivity.EXTRA_FACTION_ID,0);
        factionName=getIntent().getStringExtra(FactionOpsActivity.EXTRA_FACTION_NAME);
        position=getIntent().getStringExtra(DeveloperConsoleActivity.EXTRA_POSITION);
        factionApi=getIntent().getBooleanExtra(FactionOpsActivity.EXTRA_FACTION_API,false);
        if(factionName==null||factionName.isBlank())factionName="Your faction";
        if(position==null||position.isBlank())position="Member";
        render();
    }

    private void render(){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"TornFCA","War Center",factionName+" • choose the kind of faction warfare you want to follow.");

        TornFcaUi.addSection(this,r,"War modes");
        addLaunch(r,"RANKED WAR","Ranked War","Current matchup, score, participation, opponent intelligence and completed ranked-war history.",TornFcaUi.RED,WarCenterActivity.class,"Open Ranked War");
        addLaunch(r,"TERRITORIES","Territories","Owned territory blocks, live assaults, wall status, territory-war history and your personal joins / clears / score when available.",TornFcaUi.GOLD,TerritoryWarActivity.class,"Open Territories");

        TornFcaUi.addSection(this,r,"Get ready");
        addLaunch(r,"READINESS","My War Prep","Check your personal energy, cooldowns, refills, travel, OC and war-prep checklist before faction warfare.",TornFcaUi.GREEN,WarPrepActivity.class,"Open War Prep");

        LinearLayout info=TornFcaUi.card(this,"TERRITORY BASICS","What is territory warfare?","Factions can own blocks of the Torn city map. Held territories generate daily respect and may contain rackets. Territory assaults are fought over a wall with limited slots, scores and a target. TornFCA keeps this separate from Ranked War so members can focus on the mode their faction is actually using.",TornFcaUi.BORDER);
        TornFcaUi.add(this,r,info);

        setContentView(s);s.requestApplyInsets();
    }

    private void addLaunch(LinearLayout r,String eye,String title,String body,int accent,Class<?> target,String action){
        LinearLayout c=TornFcaUi.card(this,eye,title,body,accent);c.setClickable(true);c.setFocusable(true);
        Runnable open=()->{Intent i=new Intent(this,target);putScope(i);startActivity(i);};
        c.setOnClickListener(v->open.run());Button b=TornFcaUi.button(this,action,accent);b.setOnClickListener(v->open.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));p.topMargin=TornFcaUi.dp(this,10);c.addView(b,p);TornFcaUi.add(this,r,c);
    }

    private void putScope(Intent i){
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_ID,factionId);
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_NAME,factionName);
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_API,factionApi);
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_ID,factionId);
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_NAME,factionName);
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_API,factionApi);
        i.putExtra(DeveloperConsoleActivity.EXTRA_POSITION,position);
    }
}

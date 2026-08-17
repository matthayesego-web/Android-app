package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Player-facing About page. Detailed architecture notes live in the repository, not in the app UI. */
public class AboutActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);render();}
    private void render(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"More","About TornFCA","Torn Faction Companion App • v"+TornFcaBrand.VERSION);TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION COMPANION","Built for faction life","TornFCA brings your personal faction day, war participation, OC, chain, faction tools and leadership workflows into one focused Android companion. It complements Torn rather than replacing it.",TornFcaUi.GOLD));TornFcaUi.add(this,r,TornFcaUi.card(this,"PRIVACY","Your Torn API key","A Limited Access Torn API key is recommended. The key stays session-only unless you explicitly choose encrypted 7, 30 or 90 day device retention. TornFCA does not store it in plaintext.",TornFcaUi.GREEN));TornFcaUi.add(this,r,TornFcaUi.card(this,"INTEGRATIONS","Third-party intelligence","FFScouter and TornStats are optional separate services. TornFCA contacts them only after you explicitly opt in after reviewing their terms/data policy.",TornFcaUi.BLUE));Button ff=TornFcaUi.button(this,"FFScouter",TornFcaUi.BLUE);ff.setOnClickListener(v->open(FFScouterClient.HOMEPAGE));r.addView(ff,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46)));LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));gap.topMargin=TornFcaUi.dp(this,8);Button ts=TornFcaUi.button(this,"TornStats Terms",TornFcaUi.BLUE);ts.setOnClickListener(v->open(TornStatsClient.TERMS_URL));r.addView(ts,gap);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=TornFcaUi.dp(this,16);r.addView(TornFcaUi.footer(this,"Independent community project • Torn, FFScouter and TornStats are separate services."),fp);setContentView(s);s.requestApplyInsets();}
    private void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}}
}

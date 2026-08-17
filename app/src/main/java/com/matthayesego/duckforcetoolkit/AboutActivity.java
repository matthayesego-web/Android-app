package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Player-facing About page. Detailed architecture and release notes remain in the repository. */
public class AboutActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);render();}
    @Override protected void onResume(){super.onResume();render();}

    private void render(){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"More","About TornFCA","Torn Faction Companion App • v"+TornFcaBrand.VERSION);

        TornFcaUi.addSection(this,r,"What TornFCA is");
        TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION COMPANION","Built around everyday faction life","TornFCA brings your daily status, training, faction resources, war prep, OC, chains, community tools and faction information into one companion. Leadership tools appear only when your verified faction access allows them. TornFCA complements Torn rather than replacing it.",TornFcaUi.GOLD));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"MEMBER FIRST","Core faction tools stay free","My Day, war readiness, OC and chain status, training resources, faction guides, directory and other core member tools are not locked behind Premium. Premium is reserved for optional extra history, analytics, automation and convenience.",TornFcaUi.GREEN));

        TornFcaUi.addSection(this,r,"Privacy & legal");
        TornFcaUi.add(this,r,TornFcaUi.card(this,"API KEY","Minimum access by design","A Limited Access Torn API key is recommended; Full Access is not required. Your key stays session-only unless you choose encrypted 7, 30 or 90 day storage on this device. Shared/cloud features use verified faction scope only where the selected feature needs it.",TornFcaUi.GREEN));

        String legalStatus=LegalAcceptanceStore.hasAcceptedCurrent(this)?"The current Privacy Policy/data-use notice, Terms & Conditions and EULA have been acknowledged on this device.":"The current legal version has not yet been acknowledged on this device.";
        LinearLayout legal=TornFcaUi.card(this,"LEGAL & PRIVACY","Review current documents",legalStatus,TornFcaUi.PURPLE);
        Button legalButton=TornFcaUi.button(this,"Privacy, Terms & EULA",TornFcaUi.PURPLE);legalButton.setOnClickListener(v->startActivity(new Intent(this,LegalActivity.class)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));lp.topMargin=TornFcaUi.dp(this,10);legal.addView(legalButton,lp);TornFcaUi.add(this,r,legal);

        TornFcaUi.addSection(this,r,"Optional services");
        TornFcaUi.add(this,r,TornFcaUi.card(this,"PLAYER INTELLIGENCE","FFScouter & TornStats","These are separate optional services. TornFCA contacts them only after you choose to enable the relevant integration and review its provider information. They operate under their own terms and privacy practices.",TornFcaUi.BLUE));
        Button ff=TornFcaUi.button(this,"FFScouter",TornFcaUi.BLUE);ff.setOnClickListener(v->open(FFScouterClient.HOMEPAGE));r.addView(ff,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46)));
        LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));gap.topMargin=TornFcaUi.dp(this,8);
        Button ts=TornFcaUi.button(this,"TornStats Terms",TornFcaUi.BLUE);ts.setOnClickListener(v->open(TornStatsClient.TERMS_URL));r.addView(ts,gap);

        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=TornFcaUi.dp(this,16);
        r.addView(TornFcaUi.footer(this,"Independent community project • Not affiliated with Torn • Torn, Google/Firebase, FFScouter and TornStats are separate services."),fp);
        setContentView(s);s.requestApplyInsets();
    }

    private void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}}
}

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
    @Override protected void onResume(){super.onResume();render();}

    private void render(){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"More","About TornFCA","Torn Faction Companion App • v"+TornFcaBrand.VERSION);
        TornFcaUi.add(this,r,TornFcaUi.card(this,"FACTION COMPANION","Built for faction life","TornFCA puts your daily faction status, training, resources, wars, OC, chains and community tools in one place. Leadership tools appear only when your faction access allows them. TornFCA complements Torn rather than replacing it.",TornFcaUi.GOLD));
        TornFcaUi.add(this,r,TornFcaUi.card(this,"PRIVACY","Your Torn API key","A Limited Access Torn API key is recommended; Full Access is not required. Your key stays session-only unless you choose encrypted 7, 30 or 90 day storage on this device.",TornFcaUi.GREEN));

        String legalStatus=LegalAcceptanceStore.hasAcceptedCurrent(this)?"The current Privacy Policy, Terms & Conditions and EULA have been acknowledged on this device.":"The current legal documents have not yet been acknowledged on this device.";
        LinearLayout legal=TornFcaUi.card(this,"LEGAL & PRIVACY","Review current documents",legalStatus,TornFcaUi.PURPLE);
        Button legalButton=TornFcaUi.button(this,"Privacy, Terms & EULA",TornFcaUi.PURPLE);legalButton.setOnClickListener(v->startActivity(new Intent(this,LegalActivity.class)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));lp.topMargin=TornFcaUi.dp(this,10);legal.addView(legalButton,lp);TornFcaUi.add(this,r,legal);

        TornFcaUi.add(this,r,TornFcaUi.card(this,"OPTIONAL SERVICES","FFScouter & TornStats","These are separate optional player-intelligence services. TornFCA contacts them only after you choose to enable them and review their terms/data policy.",TornFcaUi.BLUE));
        Button ff=TornFcaUi.button(this,"FFScouter",TornFcaUi.BLUE);ff.setOnClickListener(v->open(FFScouterClient.HOMEPAGE));r.addView(ff,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46)));
        LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));gap.topMargin=TornFcaUi.dp(this,8);Button ts=TornFcaUi.button(this,"TornStats Terms",TornFcaUi.BLUE);ts.setOnClickListener(v->open(TornStatsClient.TERMS_URL));r.addView(ts,gap);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=TornFcaUi.dp(this,16);r.addView(TornFcaUi.footer(this,"Independent community project • Torn, FFScouter and TornStats are separate services."),fp);
        setContentView(s);s.requestApplyInsets();
    }
    private void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}}
}

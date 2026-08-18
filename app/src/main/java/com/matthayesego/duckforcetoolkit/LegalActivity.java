package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

/** Central legal hub and first-run acknowledgement gate. */
public class LegalActivity extends Activity {
    public static final String EXTRA_REQUIRE_ACCEPTANCE="require_acceptance";
    private boolean requireAcceptance;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        requireAcceptance=getIntent().getBooleanExtra(EXTRA_REQUIRE_ACCEPTANCE,false);
        render();
    }

    private void render(){
        ScrollView s=TornFcaUi.shell(this);
        LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(
                this,
                r,
                requireAcceptance?"Exit":"More",
                requireAcceptance?"Before you continue":"Legal & Privacy",
                requireAcceptance
                        ?"A short privacy acknowledgement and agreement to the Terms & Conditions and EULA are required before connecting a Torn account. You can open every document below before deciding."
                        :"Review TornFCA's current legal documents and acknowledgement status."
        );

        if(requireAcceptance){
            TornFcaUi.add(this,r,TornFcaUi.card(
                    this,
                    "DATA USE BEFORE SIGN-IN",
                    "What happens when you connect",
                    "TornFCA uses the Torn API key you provide to request the Torn data needed for features you choose. A Limited Access key is recommended; Full Access is not required. If cloud/community features are configured, TornFCA may process your verified Torn player/faction identifiers and a Firebase push token for faction-scoped services and notifications. FFScouter and TornStats remain separate optional services and require their own opt-in. Read the Privacy Policy below for storage, retention, deletion and Firebase details.",
                    TornFcaUi.GREEN
            ));
        }else{
            String status;
            if(LegalAcceptanceStore.hasAcceptedCurrent(this)){
                long at=LegalAcceptanceStore.acceptedAt(this);
                status="Current legal version acknowledged.";
                if(at>0){
                    status="Current legal version acknowledged on "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(at))+".";
                }
            }else{
                status="The current legal version has not been acknowledged on this device.";
            }
            TornFcaUi.add(this,r,TornFcaUi.card(this,"STATUS","Legal acknowledgement",status,TornFcaUi.GREEN));
        }

        addDocument(r,"PRIVACY","Privacy Policy","What Torn data the app accesses, local/cloud storage, notifications, optional providers, retention, deletion and security.",LegalDocumentActivity.DOC_PRIVACY,TornFcaUi.GREEN);
        addDocument(r,"TERMS","Terms & Conditions","Rules for lawful use, faction content, community conduct, service accuracy, optional providers and service changes.",LegalDocumentActivity.DOC_TERMS,TornFcaUi.GOLD);
        addDocument(r,"LICENSE","End User License Agreement","The personal app license, ownership, restrictions, updates, third-party components and termination.",LegalDocumentActivity.DOC_EULA,TornFcaUi.BLUE);

        if(requireAcceptance){
            LinearLayout accept=TornFcaUi.card(this,"ACKNOWLEDGEMENT","Accept current legal version","These choices are kept on this device with the legal version and acknowledgement time. Material legal changes can require acknowledgement again.",TornFcaUi.PURPLE);

            CheckBox privacyBox=new CheckBox(this);
            privacyBox.setText("I have reviewed the Privacy Policy and understand the data-use notice above.");
            privacyBox.setTextColor(TornFcaUi.TEXT);
            privacyBox.setTextSize(13f);
            LinearLayout.LayoutParams privacyParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            privacyParams.topMargin=TornFcaUi.dp(this,10);
            accept.addView(privacyBox,privacyParams);

            CheckBox agreementBox=new CheckBox(this);
            agreementBox.setText("I agree to the Terms & Conditions and the End User License Agreement.");
            agreementBox.setTextColor(TornFcaUi.TEXT);
            agreementBox.setTextSize(13f);
            LinearLayout.LayoutParams agreementParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            agreementParams.topMargin=TornFcaUi.dp(this,6);
            accept.addView(agreementBox,agreementParams);

            Button continueButton=TornFcaUi.button(this,"Accept & Continue",TornFcaUi.GREEN);
            continueButton.setEnabled(false);
            Runnable updateEnabled=()->continueButton.setEnabled(privacyBox.isChecked()&&agreementBox.isChecked());
            privacyBox.setOnCheckedChangeListener((buttonView,isChecked)->updateEnabled.run());
            agreementBox.setOnCheckedChangeListener((buttonView,isChecked)->updateEnabled.run());
            continueButton.setOnClickListener(v->{
                if(!privacyBox.isChecked()||!agreementBox.isChecked()){
                    Toast.makeText(this,"Please complete both acknowledgement choices first.",Toast.LENGTH_SHORT).show();
                    return;
                }
                LegalAcceptanceStore.acceptCurrent(this);
                PushNotifications.initialize(this);
                Intent i=TornFcaCommandRuntime.homeIntent(this,"Home");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            });
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,50));
            cp.topMargin=TornFcaUi.dp(this,12);
            accept.addView(continueButton,cp);
            TornFcaUi.add(this,r,accept);
        }

        TextView note=TornFcaUi.footer(this,"TornFCA is an independent community project. Torn and optional integrated services are separate services. Legal documents can be reviewed again from More, Settings or About TornFCA.");
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        np.topMargin=TornFcaUi.dp(this,8);
        r.addView(note,np);

        setContentView(s);
        s.requestApplyInsets();
    }

    private void addDocument(LinearLayout r,String eye,String title,String body,String doc,int accent){
        LinearLayout card=TornFcaUi.card(this,eye,title,body,accent);
        Runnable open=()->{
            Intent i=new Intent(this,LegalDocumentActivity.class);
            i.putExtra(LegalDocumentActivity.EXTRA_DOCUMENT,doc);
            startActivity(i);
        };
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v->open.run());

        Button b=TornFcaUi.button(this,"Read "+title,accent);
        b.setOnClickListener(v->open.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));
        p.topMargin=TornFcaUi.dp(this,10);
        card.addView(b,p);
        TornFcaUi.add(this,r,card);
    }
}

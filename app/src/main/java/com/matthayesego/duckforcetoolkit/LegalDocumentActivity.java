package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Plain-language legal documents kept inside the app for easy review. */
public class LegalDocumentActivity extends Activity {
    public static final String EXTRA_DOCUMENT="document";
    public static final String DOC_PRIVACY="privacy";
    public static final String DOC_TERMS="terms";
    public static final String DOC_EULA="eula";

    @Override protected void onCreate(Bundle b){super.onCreate(b);render();}

    private void render(){
        String doc=getIntent().getStringExtra(EXTRA_DOCUMENT);if(doc==null)doc=DOC_PRIVACY;
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        if(DOC_TERMS.equals(doc))renderTerms(r);else if(DOC_EULA.equals(doc))renderEula(r);else renderPrivacy(r);
        setContentView(s);s.requestApplyInsets();
    }

    private void renderPrivacy(LinearLayout r){
        TornFcaUi.header(this,r,"Legal","Privacy Policy","Effective August 17, 2026 • Legal version "+LegalAcceptanceStore.LEGAL_VERSION);
        section(r,"Overview","TornFCA is an independent community companion for Torn faction members. This policy explains what data the app accesses, what may stay on your device, what may be sent to TornFCA services, and when optional third-party services receive data.");
        section(r,"Torn data we access","Features may request your Torn identity, faction membership and position, faction/member information, bars and cooldowns, organized-crime information, chain and ranked-war information, battle stats, personal statistics and other Torn API data needed for the feature you choose. Access is limited by your Torn API key and Torn permissions.");
        section(r,"Your API key","TornFCA recommends a Limited Access Torn API key. By default the key can remain session-only. If you choose 7, 30 or 90 day retention, it is encrypted on the device using Android security APIs. TornFCA may send the key over HTTPS to Torn's official API and, for specific shared faction features, to the TornFCA backend for identity or permission verification. The TornFCA backend is designed not to retain the API key.");
        section(r,"Your choices","You choose whether the Torn API key is stored beyond the current app session, whether optional FFScouter or TornStats integrations are enabled, and which TornFCA notification categories you want. Cloud push remains disabled until the current legal version is acknowledged. Android notification permission remains under the device user's control.");
        section(r,"Data stored on your device","Depending on the features you use, TornFCA may store preferences, notification history, provider-consent choices, cached faction identity, onboarding/checklist state, war-prep state, personal training baselines, entitlement state and the legal version/timestamp acknowledged on that device. Personal training baselines are device-local and are scoped to the player and faction.");
        section(r,"Faction community data","When shared faction features are enabled, TornFCA services may store verified player and faction identifiers, push-notification device tokens, faction chat messages, faction guides, training rules, notices and other content intentionally submitted to shared faction features. Shared faction content is separated by verified faction identity and may be restricted by faction role or permission.");
        section(r,"Notifications & Firebase","When cloud push is configured, TornFCA uses Firebase Cloud Messaging (FCM). TornFCA delays FCM Messaging initialization until you acknowledge the current legal version. FCM and its Firebase Installations component may process app-version and Firebase user-agent information and create a per-installation Firebase installation ID (FID); the user-agent information can include device/OS/model/form-factor, install-source and Firebase SDK/version details. TornFCA may then register an FCM token together with your verified Torn player/faction scope so relevant notifications can reach your device. Firebase Analytics collection is explicitly disabled in the current Android build, and TornFCA does not enable FCM BigQuery delivery-metrics export.");
        section(r,"Third-party services","Torn's official API is required for core Torn data. FFScouter and TornStats are optional intelligence providers and are contacted only after separate explicit opt-in. Those providers operate under their own terms and privacy practices. Google Play and Google/Firebase services may process data required for app distribution, billing or push delivery under their own policies.");
        section(r,"Retention and deletion","Local data remains until it expires, is reset by a feature, is removed by account/logout controls where applicable, or Android app data is cleared/uninstalled. Shared TornFCA-hosted content may remain while needed for the feature, moderation, security or reasonable operational records. To request deletion of TornFCA-hosted personal data, use the developer/privacy contact shown on TornFCA's distribution page or Google Play listing. This does not delete data held independently by Torn or optional third-party providers.");
        section(r,"Security","TornFCA uses HTTPS for supported network connections, does not permit cleartext traffic in the Android app, and offers encrypted on-device API-key retention. No system can guarantee absolute security, so users should revoke a Torn API key if they believe it has been exposed.");
        section(r,"Changes and contact","Material changes to these disclosures use a new legal version and can require renewed in-app acknowledgement. Privacy questions or deletion requests can be sent through the developer/privacy contact published with TornFCA on its distribution page or Google Play listing.");
    }

    private void renderTerms(LinearLayout r){
        TornFcaUi.header(this,r,"Legal","Terms & Conditions","Effective August 17, 2026 • Legal version "+LegalAcceptanceStore.LEGAL_VERSION);
        section(r,"Using TornFCA","By using TornFCA you agree to use it lawfully, in accordance with these terms, and in a way that does not abuse Torn, Torn's API, TornFCA services, other players, or third-party providers. TornFCA is an independent community project and is not Torn itself.");
        section(r,"Your responsibility","You are responsible for the Torn API key you provide, the permissions attached to it, actions you take based on app information, and content you submit to faction/community features. Use the minimum Torn API access needed and revoke a key if you believe it has been exposed.");
        section(r,"Faction content and permissions","Faction leaders may publish faction-specific rules, guides, notices and expectations. Those are created by the faction, not universal TornFCA rules. Access to faction-local content depends on current verified faction membership and may change when you change factions or roles.");
        section(r,"Community conduct","Community features must not be used for threats, harassment, bullying, hateful or sexually exploitative content, unlawful content, malicious links or code, credential sharing, impersonation, evasion of faction/access controls, or deliberate interference with another user's access. Do not post private or sensitive information about another person without permission. Content or access may be restricted or removed when reasonably necessary for moderation, safety, security, policy compliance or service operation.");
        section(r,"Reports, blocks and moderation","Where TornFCA provides shared user-generated content such as faction chat, users are expected to use available report and block controls for objectionable content or users. Reports may be reviewed and acted on according to the scope of the community feature. Blocking is a personal visibility/safety control and does not by itself determine whether reported content violates these terms.");
        section(r,"Accuracy and availability","TornFCA depends on Torn API data, network availability, third-party providers and faction-entered information. Information may be delayed, incomplete or wrong. War readiness, payout estimates, training guidance, intelligence estimates and similar features are decision aids rather than guarantees.");
        section(r,"Optional providers and paid features","Optional providers such as FFScouter and TornStats have separate terms. If TornFCA offers paid features through Google Play, pricing, renewal, cancellation and refunds will also be subject to the purchase disclosures and Google Play rules shown at the time of purchase. Core access described as free in the app is not converted into a paid entitlement merely by accepting these terms.");
        section(r,"Service changes","Features may be changed, suspended or retired for security, API compatibility, legal, policy or operational reasons. Material legal changes can require renewed acknowledgement in the app.");
        section(r,"No misuse of TornFCA","You may not use TornFCA or its shared services to bypass faction authorization, obtain data you are not permitted to access, distribute secrets or credentials, or intentionally overload TornFCA, Torn or integrated providers.");
        section(r,"Disclaimer","TornFCA is provided on an as-available basis. To the extent permitted by applicable law, no warranty is made that it will always be available, error-free, secure, or suitable for a particular faction strategy or outcome.");
        section(r,"Contact","Questions about these terms can be sent through the developer contact published with TornFCA on its distribution page or Google Play listing.");
    }

    private void renderEula(LinearLayout r){
        TornFcaUi.header(this,r,"Legal","End User License Agreement","Effective August 17, 2026 • Legal version "+LegalAcceptanceStore.LEGAL_VERSION);
        section(r,"License","Subject to these terms, TornFCA grants you a limited, personal, revocable, non-exclusive and non-transferable license to install and use the app for lawful personal/faction companion purposes on devices you control.");
        section(r,"Ownership","TornFCA's original app code, branding and service components remain the property of their respective owner(s). Torn, Torn data, third-party names and third-party services remain the property of their respective owners. No ownership rights are transferred to you by installing the app.");
        section(r,"Restrictions","You may not use the app to defeat authentication or permission controls, steal credentials, introduce malicious code, intentionally disrupt services, or redistribute a modified build in a way that falsely represents it as an official TornFCA release. Rights granted by applicable law are not waived where they cannot legally be restricted.");
        section(r,"Updates","Updates may add, remove or alter features and may be required for compatibility, security or policy compliance. A materially changed EULA or Terms version may require renewed acknowledgement.");
        section(r,"Third-party components","The app relies on Android/Google services and may integrate with Torn, Firebase, FFScouter, TornStats and other services. Their software, data and services are governed by their own licenses and policies where applicable.");
        section(r,"Termination","This license ends if you materially violate the applicable terms or if the app/service is discontinued. You may stop using TornFCA at any time by removing the app and revoking any Torn API key you no longer wish to use with it.");
        section(r,"Warranty and liability","TornFCA is provided as available and is not a guarantee of game outcomes, payouts, intelligence accuracy, faction performance or uninterrupted service. Any limitation of liability applies only to the extent allowed by applicable law.");
        section(r,"Contact","Questions about this EULA can be sent through the developer contact published with TornFCA on its distribution page or Google Play listing.");
    }

    private void section(LinearLayout r,String title,String body){TornFcaUi.add(this,r,TornFcaUi.card(this,"",title,body,TornFcaUi.BORDER));}
}

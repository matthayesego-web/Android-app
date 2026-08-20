package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Synthetic Google Play review environment.
 *
 * No Torn API key, Torn API call, Apps Script backend call, Firebase registration or production write is made
 * from this activity. It exists only to let store reviewers inspect representative Member/Leader workflows.
 */
public class PlayReviewActivity extends Activity {
    public static final String REVIEW_CODE="TORNFCA-PLAY-REVIEW";
    private PlayReviewStore.Persona persona=PlayReviewStore.Persona.MEMBER;
    private boolean featureOpen=false;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(TornFcaUi.BG);
        getWindow().setNavigationBarColor(TornFcaUi.BG);
        if(PlayReviewStore.isActive(this)){
            persona=PlayReviewStore.persona(this);
            showDashboard();
        }else showAccessGate();
    }

    private void showAccessGate(){
        ScrollView scroll=TornFcaUi.shell(this);LinearLayout root=TornFcaUi.root(this,scroll);
        TornFcaUi.header(this,root,"Back","Google Play Review Access","Testing-only access using synthetic faction data. No live Torn account is required.");
        LinearLayout info=TornFcaUi.card(this,"REVIEW SANDBOX","Safe store-review environment","This mode never creates a Torn session, never registers a Firebase device and never writes to TornFCA production backends.",TornFcaUi.PURPLE);
        TornFcaUi.add(this,root,info);

        EditText code=new EditText(this);code.setHint("Reviewer access code");code.setSingleLine(true);code.setTextColor(TornFcaUi.TEXT);code.setHintTextColor(TornFcaUi.MUTED);code.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);code.setPadding(dp(14),0,dp(14),0);code.setBackground(TornFcaUi.rounded(this,TornFcaUi.PANEL2,TornFcaUi.BORDER,12));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54));cp.bottomMargin=dp(10);root.addView(code,cp);
        TextView status=TornFcaUi.text(this,"Use the reusable code supplied in Play Console App Access instructions.",11.5f,TornFcaUi.MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.bottomMargin=dp(12);root.addView(status,sp);

        Button enter=TornFcaUi.button(this,"Enter Review Sandbox",TornFcaUi.PURPLE);enter.setOnClickListener(v->{
            String value=code.getText()==null?"":code.getText().toString().trim();
            if(!REVIEW_CODE.equals(value)){
                status.setText("Review code not recognized.");status.setTextColor(TornFcaUi.RED);return;
            }
            persona=PlayReviewStore.Persona.MEMBER;PlayReviewStore.enter(this,persona);showDashboard();
        });
        root.addView(enter,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50)));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=dp(18);root.addView(TornFcaUi.footer(this,"Synthetic review data only • v"+TornFcaBrand.VERSION),fp);
        setContentView(scroll);scroll.requestApplyInsets();
    }

    private void showDashboard(){
        featureOpen=false;
        if(!PlayReviewStore.isActive(this)){showAccessGate();return;}
        persona=PlayReviewStore.persona(this);
        ScrollView scroll=TornFcaUi.shell(this);LinearLayout root=TornFcaUi.root(this,scroll);

        TextView banner=TornFcaUi.text(this,"PLAY REVIEW SANDBOX • SYNTHETIC DATA • NO LIVE WRITES",10,TornFcaUi.PURPLE,true);banner.setGravity(Gravity.CENTER);banner.setPadding(dp(10),dp(10),dp(10),dp(10));banner.setBackground(TornFcaUi.rounded(this,Color.rgb(24,15,38),TornFcaUi.PURPLE,12));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bp.bottomMargin=dp(14);root.addView(banner,bp);

        TextView title=TornFcaUi.text(this,persona==PlayReviewStore.Persona.LEADER?"Review Leader":"Review Member",29,TornFcaUi.TEXT,true);root.addView(title);
        TextView meta=TornFcaUi.text(this,"Harbor Ducks [990001] • "+(persona==PlayReviewStore.Persona.LEADER?"Leader":"Member")+" • synthetic review persona",12.5f,TornFcaUi.MUTED,false);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);mp.topMargin=dp(3);mp.bottomMargin=dp(12);root.addView(meta,mp);

        LinearLayout personaRow=new LinearLayout(this);personaRow.setOrientation(LinearLayout.HORIZONTAL);
        Button member=TornFcaUi.button(this,"Member",persona==PlayReviewStore.Persona.MEMBER?TornFcaUi.PURPLE:TornFcaUi.BORDER);member.setOnClickListener(v->setPersona(PlayReviewStore.Persona.MEMBER));
        Button leader=TornFcaUi.button(this,"Faction Leader",persona==PlayReviewStore.Persona.LEADER?TornFcaUi.PURPLE:TornFcaUi.BORDER);leader.setOnClickListener(v->setPersona(PlayReviewStore.Persona.LEADER));
        personaRow.addView(member,new LinearLayout.LayoutParams(0,dp(46),1f));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1f);lp.leftMargin=dp(8);personaRow.addView(leader,lp);LinearLayout.LayoutParams prp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));prp.bottomMargin=dp(14);root.addView(personaRow,prp);

        TornFcaUi.addSection(this,root,"My day");
        addFeature(root,"TODAY","My Day","2 faction items need attention • OC ready in 1h 42m • war prep 4/5",TornFcaUi.GOLD,"my_day");
        addFeature(root,"WAR","Ranked War","Harbor Ducks vs Review Rangers • 1,284 – 1,166 • chain 63",TornFcaUi.RED,"war");
        addFeature(root,"OC","Organized Crime","Review Member • Planning • required item ready",TornFcaUi.BLUE,"oc");

        TornFcaUi.addSection(this,root,"Faction");
        addFeature(root,"DIRECTORY","Member Directory","40 members • sample status, last action and faction role data",TornFcaUi.BLUE,"directory");
        addFeature(root,"TRAINING","Training Resources","Current training target, guides and personal progress",TornFcaUi.GREEN,"training");
        addFeature(root,"ANNOUNCEMENTS","Faction Announcements","2 unread synthetic faction notices",TornFcaUi.GOLD,"announcements");

        TornFcaUi.addSection(this,root,"Community & requests");
        addFeature(root,"CHAT","Faction Chat","General, war and OC channels with sample report/block controls",TornFcaUi.PURPLE,"chat");
        addFeature(root,"BANKING","Banking Request","Request $25,000,000 Torn cash • synthetic queue only",TornFcaUi.GREEN,"banking");
        addFeature(root,"NOTIFICATIONS","Notification Center","Announcement, banking, chat and war preferences",TornFcaUi.BLUE,"notifications");

        if(persona==PlayReviewStore.Persona.LEADER){
            TornFcaUi.addSection(this,root,"Leadership");
            addFeature(root,"ATTENTION","Leadership Attention","3 members need review • 2 war-prep gaps • 1 inactive exception",TornFcaUi.GOLD,"leadership_attention");
            addFeature(root,"ACTIVITY","Faction Activity","30-day synthetic participation and action rollup",TornFcaUi.BLUE,"activity");
            addFeature(root,"WAR PREP","War Prep Leadership","34 ready • 4 partial • 2 not started",TornFcaUi.RED,"war_prep_leader");
            addFeature(root,"BANKING","Banking Management","3 pending requests • approve/deny actions remain local",TornFcaUi.GREEN,"banking_manage");
            addFeature(root,"PUBLISH","Announcement Controls","Publish and delete sample notices without live push",TornFcaUi.GOLD,"announcement_manage");
            addFeature(root,"PAYOUT","War Payout Workflow","Synthetic hit ledger, rates and payout receipt preview",TornFcaUi.PURPLE,"war_payout");
            addFeature(root,"MODERATION","Community Moderation","2 sample reports • dismiss/remove actions remain local",TornFcaUi.RED,"moderation");
        }

        TornFcaUi.addSection(this,root,"Review controls");
        Button exit=TornFcaUi.button(this,"Exit Review Sandbox",TornFcaUi.RED);exit.setOnClickListener(v->exitReview());root.addView(exit,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50)));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=dp(18);root.addView(TornFcaUi.footer(this,"No Torn API • No Apps Script • No Firebase registration • v"+TornFcaBrand.VERSION),fp);
        setContentView(scroll);scroll.requestApplyInsets();
    }

    private void addFeature(LinearLayout root,String eyebrow,String title,String body,int accent,String key){
        LinearLayout card=TornFcaUi.card(this,eyebrow,title,body,accent);card.setClickable(true);card.setFocusable(true);card.setOnClickListener(v->showFeature(key,title));TornFcaUi.add(this,root,card);
    }

    private void showFeature(String key,String title){
        featureOpen=true;
        ScrollView scroll=TornFcaUi.shell(this);LinearLayout root=TornFcaUi.root(this,scroll);
        Button back=TornFcaUi.button(this,"← Review dashboard",TornFcaUi.BORDER);back.setOnClickListener(v->showDashboard());root.addView(back,new LinearLayout.LayoutParams(dp(190),dp(44)));
        TextView sandbox=TornFcaUi.text(this,"PLAY REVIEW SANDBOX",10,TornFcaUi.PURPLE,true);sandbox.setLetterSpacing(.11f);LinearLayout.LayoutParams sb=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sb.topMargin=dp(16);root.addView(sandbox,sb);
        root.addView(TornFcaUi.text(this,title,29,TornFcaUi.TEXT,true));
        TextView sub=TornFcaUi.text(this,"Representative synthetic workflow. Nothing on this screen can affect a live Torn account or TornFCA production data.",12.5f,TornFcaUi.MUTED,false);LinearLayout.LayoutParams sup=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sup.topMargin=dp(5);sup.bottomMargin=dp(15);root.addView(sub,sup);

        String[] rows=sampleRows(key);
        for(String row:rows){
            String[] parts=row.split("\\|",2);String h=parts.length>0?parts[0]:"Review item",b=parts.length>1?parts[1]:"Synthetic data";
            TornFcaUi.add(this,root,TornFcaUi.card(this,"SAMPLE",h,b,TornFcaUi.BLUE));
        }
        if(isActionFeature(key)){
            Button action=TornFcaUi.button(this,actionLabel(key),TornFcaUi.PURPLE);action.setOnClickListener(v->Toast.makeText(this,"Review sandbox — no live changes were made.",Toast.LENGTH_SHORT).show());LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));ap.topMargin=dp(4);root.addView(action,ap);
        }
        setContentView(scroll);scroll.requestApplyInsets();
    }

    private String[] sampleRows(String key){
        switch(key){
            case "my_day": return new String[]{"War participation|12 hits logged • target 10 • complete","OC readiness|Planning • CPR ready • required item ready","War Prep|4 of 5 checklist items complete"};
            case "war": return new String[]{"Current ranked war|Harbor Ducks 1,284 • Review Rangers 1,166","Chain|63 hits • 4:37 remaining","My participation|12 hits • 1 assist • target complete"};
            case "oc": return new String[]{"Assignment|Review Member • Slot 3","Status|Planning • starts in 1h 42m","Readiness|CPR ready • item ready"};
            case "directory": return new String[]{"Review Member|Online • Member • 12 war hits","Quack Reviewer|Idle 18m • Recruit • 6 war hits","Sample Leader|Online • Leader • leadership access"};
            case "training": return new String[]{"Faction target|Train daily during the current training block","Xanax target|3 per day where safe/available","Guide|War preparation and faction expectations"};
            case "announcements": return new String[]{"Training block|Focus on battle stats until Friday","Next war|Stack energy before the declared start"};
            case "chat": return new String[]{"General|Review Member: Morning all!","War|Sample Leader: Stack to 1,000E before start","Safety tools|Report message • block member • unblock member"};
            case "banking": return new String[]{"Available balance|$84,500,000 Torn cash","Request|$25,000,000 • pending","History|$10,000,000 paid • synthetic receipt"};
            case "notifications": return new String[]{"Announcements|Enabled","Banking|Enabled","Chat|Muted","War / chain|Enabled"};
            case "leadership_attention": return new String[]{"War target|2 members below hit target","War Prep|2 members not started","Inactivity|1 member exceeds review threshold"};
            case "activity": return new String[]{"Review Member|146 actions / 30 days","Quack Reviewer|119 actions / 30 days","Sample Recruit|41 actions / 30 days"};
            case "war_prep_leader": return new String[]{"Ready|34 members","Partial|4 members","Not started|2 members"};
            case "banking_manage": return new String[]{"Review Member|$25,000,000 • pending","Quack Reviewer|Full balance • pending","Sample Recruit|$5,000,000 • pending"};
            case "announcement_manage": return new String[]{"Draft|War starts Saturday at 18:00 TCT","Published|Training block active through Friday"};
            case "war_payout": return new String[]{"Eligible hits|368","Current rate|$1,250,000 per qualifying hit","Projected payout|$460,000,000 synthetic total"};
            case "moderation": return new String[]{"Report 1|Spam/repeated posting • open","Report 2|Harassment sample • open","Available actions|Dismiss • remove sample message"};
            default:return new String[]{"Review data|Representative TornFCA sample content"};
        }
    }

    private boolean isActionFeature(String key){
        return "chat".equals(key)||"banking".equals(key)||"banking_manage".equals(key)||"announcement_manage".equals(key)||"war_prep_leader".equals(key)||"moderation".equals(key)||"war_payout".equals(key);
    }

    private String actionLabel(String key){
        if("chat".equals(key))return"Simulate report / block";
        if("banking".equals(key))return"Submit synthetic banking request";
        if("banking_manage".equals(key))return"Approve synthetic request";
        if("announcement_manage".equals(key))return"Publish synthetic announcement";
        if("war_prep_leader".equals(key))return"Update synthetic checklist";
        if("moderation".equals(key))return"Resolve synthetic report";
        if("war_payout".equals(key))return"Generate synthetic payout receipt";
        return"Simulate action";
    }

    private void setPersona(PlayReviewStore.Persona next){persona=next;PlayReviewStore.setPersona(this,next);showDashboard();}

    private void exitReview(){
        PlayReviewStore.clear(this);
        Intent i=new Intent(this,AccessGateActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();
    }

    private int dp(int value){return TornFcaUi.dp(this,value);}

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed(){
        if(PlayReviewStore.isActive(this)&&featureOpen)showDashboard();else if(PlayReviewStore.isActive(this))moveTaskToBack(true);else super.onBackPressed();
    }
}

package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/** Beta feedback surface backed by the authenticated TornFCA Feedback service. */
public class FeedbackActivity extends Activity {
    private static final String[] CATEGORIES={"Bug","Feature Request","UI/UX","Performance","Access/Permissions","Other"};

    private Spinner category;
    private EditText title;
    private EditText message;
    private Button submit;
    private TextView status;
    private LinearLayout history;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        render();
        loadHistory();
    }

    private void render(){
        ScrollView shell=TornFcaUi.shell(this);
        LinearLayout root=TornFcaUi.root(this,shell);
        TornFcaUi.header(this,root,"More","Feedback & Requests","Send a bug report, feature request or usability note directly to the TornFCA Beta feedback queue. Your app/build details and verified Torn identity are attached safely by the service.");

        TornFcaUi.addSection(this,root,"Send feedback");
        LinearLayout card=TornFcaUi.card(this,"BETA","Tell us what happened","Do not include API keys, passwords, authenticator secrets or other credentials. Those are never needed for a feedback report.",TornFcaUi.PURPLE);

        category=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,CATEGORIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category.setAdapter(adapter);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,50));cp.topMargin=TornFcaUi.dp(this,12);card.addView(category,cp);

        title=input("Short title",true,1);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,52));tp.topMargin=TornFcaUi.dp(this,10);card.addView(title,tp);

        message=input("Describe the issue, request or suggestion",false,6);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,150));mp.topMargin=TornFcaUi.dp(this,10);card.addView(message,mp);

        submit=TornFcaUi.button(this,"Submit Feedback",TornFcaUi.PURPLE);
        submit.setOnClickListener(v->submitFeedback());
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));sp.topMargin=TornFcaUi.dp(this,11);card.addView(submit,sp);

        status=TornFcaUi.text(this,FeedbackBackendClient.isConfigured()?"Feedback backend connected for this build.":"Feedback backend is not configured in this build.",11.5f,TornFcaUi.MUTED,false);
        LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);stp.topMargin=TornFcaUi.dp(this,9);card.addView(status,stp);
        TornFcaUi.add(this,root,card);

        TornFcaUi.addSection(this,root,"My recent submissions");
        history=new LinearLayout(this);history.setOrientation(LinearLayout.VERTICAL);
        TornFcaUi.add(this,root,history);

        TextView footer=TornFcaUi.footer(this,"Feedback is tied to your verified Torn account so duplicate reports can be followed up safely. Developer-only notes and credentials are never exposed here.\n\nTornFCA v"+TornFcaBrand.VERSION);
        root.addView(footer,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(shell);shell.requestApplyInsets();
    }

    private EditText input(String hint,boolean singleLine,int minLines){
        EditText e=new EditText(this);
        e.setHint(hint);e.setHintTextColor(TornFcaUi.MUTED);e.setTextColor(TornFcaUi.TEXT);e.setTextSize(13.5f);
        e.setPadding(TornFcaUi.dp(this,13),TornFcaUi.dp(this,10),TornFcaUi.dp(this,13),TornFcaUi.dp(this,10));
        e.setBackground(TornFcaUi.rounded(this,TornFcaUi.PANEL2,TornFcaUi.BORDER,12));
        if(singleLine){e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}
        else{e.setSingleLine(false);e.setMinLines(minLines);e.setGravity(Gravity.TOP|Gravity.START);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}
        return e;
    }

    private void submitFeedback(){
        if(!FeedbackBackendClient.isConfigured()){status.setText("Feedback backend is not configured in this build.");return;}
        String apiKey=new SecureApiKeyStore(this).load();
        if(apiKey==null||apiKey.trim().isEmpty()){status.setText("Your signed-in Torn API key is required to verify this feedback submission.");return;}
        String selected=String.valueOf(category.getSelectedItem());
        String t=title.getText().toString().trim(),m=message.getText().toString().trim();
        if(t.isEmpty()){status.setText("Add a short title first.");return;}
        if(m.isEmpty()){status.setText("Add a description first.");return;}
        submit.setEnabled(false);status.setText("Submitting feedback…");
        String source=getIntent()==null?"Feedback":getIntent().getStringExtra("source_screen");if(source==null||source.trim().isEmpty())source="Feedback";
        final String screen=source;
        new Thread(()->{
            try{
                JSONObject saved=FeedbackBackendClient.submit(apiKey,selected,t,m,screen);
                String id=saved.optString("id","");
                runOnUiThread(()->{title.setText("");message.setText("");submit.setEnabled(true);status.setText(id.isEmpty()?"Feedback submitted.":"Feedback submitted • "+id.substring(0,Math.min(8,id.length())));loadHistory();});
            }catch(Exception ex){
                String error=ex.getMessage()==null?"Unable to submit feedback.":ex.getMessage();
                runOnUiThread(()->{submit.setEnabled(true);status.setText(error);});
            }
        }).start();
    }

    private void loadHistory(){
        if(history==null)return;
        history.removeAllViews();
        history.addView(TornFcaUi.text(this,"Loading recent submissions…",11.5f,TornFcaUi.MUTED,false));
        if(!FeedbackBackendClient.isConfigured()){showHistoryMessage("Feedback history will appear after the backend is configured.");return;}
        String apiKey=new SecureApiKeyStore(this).load();
        if(apiKey==null||apiKey.trim().isEmpty()){showHistoryMessage("Sign in with your Torn API key to view your submissions.");return;}
        new Thread(()->{
            try{JSONArray rows=FeedbackBackendClient.mine(apiKey);runOnUiThread(()->renderHistory(rows));}
            catch(Exception ex){String error=ex.getMessage()==null?"Unable to load feedback history.":ex.getMessage();runOnUiThread(()->showHistoryMessage(error));}
        }).start();
    }

    private void renderHistory(JSONArray rows){
        history.removeAllViews();
        if(rows==null||rows.length()==0){showHistoryMessage("No feedback submitted from this Torn account yet.");return;}
        int limit=Math.min(rows.length(),12);
        for(int i=0;i<limit;i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            String state=row.optString("status","NEW");
            String priority=row.optString("priority","UNSET");
            String fixed=row.optString("fixed_in_version","");
            String body=row.optString("category","Other")+" • "+state+("UNSET".equals(priority)?"":" • "+priority)+(fixed.isEmpty()?"":" • fixed in "+fixed);
            LinearLayout card=TornFcaUi.card(this,"FEEDBACK",row.optString("title","Untitled"),body,"FIXED".equals(state)?TornFcaUi.GREEN:TornFcaUi.BORDER);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=TornFcaUi.dp(this,8);history.addView(card,p);
        }
    }

    private void showHistoryMessage(String value){
        history.removeAllViews();history.addView(TornFcaUi.text(this,value,11.5f,TornFcaUi.MUTED,false));
    }
}

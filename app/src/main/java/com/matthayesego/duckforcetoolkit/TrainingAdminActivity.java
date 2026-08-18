package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

/** Leader/Co-leader editor for faction-scoped training expectations and shared faction guide library. */
public class TrainingAdminActivity extends Activity {
    private SecureApiKeyStore keyStore;
    private AuthSession session;
    private JSONObject library=new JSONObject();

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);keyStore=new SecureApiKeyStore(this);showLoading();load();}

    private void showLoading(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Faction Resources","Guide & Training Management","Verifying faction leadership and loading current rules…");TornFcaUi.add(this,r,TornFcaUi.card(this,"LEADERSHIP","Loading faction publishing","All writes are re-verified by the faction-scoped community backend.",TornFcaUi.GOLD));setContentView(s);s.requestApplyInsets();}

    private void load(){if(MemberPresentationPolicy.memberPreview(this)){renderError("Training management is hidden in Standard Member Preview, matching normal member access.");return;}String key=keyStore.load();if(key==null||key.isBlank()){renderError("Reconnect your Torn API key first.");return;}if(!CommunityBackendClient.isConfigured()){renderError("The TornFCA community backend is not configured in this build.");return;}new Thread(()->{try{AuthSession verified=TornApiClient.authenticateFreshFaction(key);if(!AccessPolicy.isLeaderPosition(verified.position))throw new Exception("Faction guide and training management is currently restricted to the faction Leader and Co-leader.");session=verified;library=CommunityBackendClient.trainingLibrary(key);runOnUiThread(this::render);}catch(Exception e){renderError(e.getMessage()==null?"Unable to load faction publishing.":e.getMessage());}},"TornFCA-TrainingAdmin").start();}

    private EditText field(String hint,boolean multi){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(TornFcaUi.MUTED);e.setTextColor(TornFcaUi.TEXT);e.setPadding(TornFcaUi.dp(this,13),TornFcaUi.dp(this,8),TornFcaUi.dp(this,13),TornFcaUi.dp(this,8));e.setBackground(TornFcaUi.rounded(this,TornFcaUi.PANEL2,TornFcaUi.BORDER,12));e.setSingleLine(!multi);e.setInputType(InputType.TYPE_CLASS_TEXT|(multi?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));if(multi)e.setMinLines(4);return e;}
    private void addField(LinearLayout parent,EditText e,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,height<=0?ViewGroup.LayoutParams.WRAP_CONTENT:TornFcaUi.dp(this,height));p.topMargin=TornFcaUi.dp(this,8);parent.addView(e,p);}

    private void render(){ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Faction Resources","Guide & Training Management",session.factionName+" • Leader/Co-leader publishing");
        JSONObject rules=library.optJSONObject("trainingRules");
        TornFcaUi.addSection(this,r,"FACTION TRAINING EXPECTATIONS");LinearLayout rc=TornFcaUi.card(this,"RULES","Set current training expectations","Use plain-language targets so every member can understand what the faction expects. Examples: “5% total battle-stat gain per month” or “Aim for 2–3 Xanax per day when not stacking for war.”",TornFcaUi.GOLD);
        EditText gain=field("Stat gain target",false),xanax=field("Regular Xanax target",false),notes=field("Training notes / exceptions",true);if(rules!=null){gain.setText(rules.optString("stat_gain_target",""));xanax.setText(rules.optString("xanax_target",""));notes.setText(rules.optString("notes",""));}addField(rc,gain,48);addField(rc,xanax,48);addField(rc,notes,0);Button save=TornFcaUi.button(this,"Save faction training rules",TornFcaUi.GOLD);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));sp.topMargin=TornFcaUi.dp(this,10);rc.addView(save,sp);save.setOnClickListener(v->saveRules(gain.getText().toString(),xanax.getText().toString(),notes.getText().toString()));TornFcaUi.add(this,r,rc);

        TornFcaUi.addSection(this,r,"PUBLISH A FACTION GUIDE");LinearLayout gc=TornFcaUi.card(this,"FACTION LIBRARY","Add a faction-specific member guide","Guides belong to this faction ID and can cover onboarding, training, trading, war prep, community rules or any other local resource. They do not follow a player who leaves the faction.",TornFcaUi.BLUE);EditText title=field("Guide title",false),category=field("Category (New Player, Training, War Prep, Trading…)",false),body=field("Guide content",true);addField(gc,title,48);addField(gc,category,48);addField(gc,body,0);Button publish=TornFcaUi.button(this,"Publish faction guide",TornFcaUi.BLUE);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46));pp.topMargin=TornFcaUi.dp(this,10);gc.addView(publish,pp);publish.setOnClickListener(v->publishGuide(title.getText().toString(),category.getText().toString(),body.getText().toString()));TornFcaUi.add(this,r,gc);

        TornFcaUi.addSection(this,r,"CURRENT FACTION GUIDES");JSONArray guides=library.optJSONArray("guides");if(guides==null||guides.length()==0)TornFcaUi.add(this,r,TornFcaUi.card(this,"LIBRARY","No guides yet","Publish the first faction-specific member guide above.",TornFcaUi.BORDER));else for(int i=0;i<guides.length();i++){JSONObject g=guides.optJSONObject(i);if(g!=null)addExistingGuide(r,g);}
        r.addView(TornFcaUi.footer(this,"Server-side faction identity is rechecked for every faction guide/training write."));setContentView(s);s.requestApplyInsets();}

    private void addExistingGuide(LinearLayout r,JSONObject g){String id=g.optString("id",""),title=g.optString("title","Guide"),category=g.optString("category","Guide"),body=g.optString("body","");LinearLayout c=TornFcaUi.card(this,category,title,body,TornFcaUi.BORDER);Button archive=TornFcaUi.button(this,"Archive guide",TornFcaUi.RED);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,42));p.topMargin=TornFcaUi.dp(this,10);c.addView(archive,p);archive.setOnClickListener(v->archiveGuide(id));TornFcaUi.add(this,r,c);}

    private void saveRules(String gain,String xanax,String notes){String key=keyStore.load();new Thread(()->{try{CommunityBackendClient.saveTrainingRules(key,gain,xanax,notes);library=CommunityBackendClient.trainingLibrary(key);runOnUiThread(()->{Toast.makeText(this,"Training rules saved.",Toast.LENGTH_SHORT).show();render();});}catch(Exception e){toast(e);}},"TornFCA-SaveTrainingRules").start();}
    private void publishGuide(String title,String category,String body){if(title.trim().isEmpty()||body.trim().isEmpty()){Toast.makeText(this,"Guide title and content are required.",Toast.LENGTH_SHORT).show();return;}String key=keyStore.load();new Thread(()->{try{CommunityBackendClient.saveTrainingGuide(key,"",title,category,body);library=CommunityBackendClient.trainingLibrary(key);runOnUiThread(()->{Toast.makeText(this,"Faction guide published.",Toast.LENGTH_SHORT).show();render();});}catch(Exception e){toast(e);}},"TornFCA-PublishFactionGuide").start();}
    private void archiveGuide(String id){String key=keyStore.load();new Thread(()->{try{CommunityBackendClient.archiveTrainingGuide(key,id);library=CommunityBackendClient.trainingLibrary(key);runOnUiThread(()->{Toast.makeText(this,"Guide archived.",Toast.LENGTH_SHORT).show();render();});}catch(Exception e){toast(e);}},"TornFCA-ArchiveFactionGuide").start();}
    private void toast(Exception e){runOnUiThread(()->Toast.makeText(this,e.getMessage()==null?"Faction resource update failed.":e.getMessage(),Toast.LENGTH_LONG).show());}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);TornFcaUi.header(this,r,"Faction Resources","Guide & Training Management","Access unavailable");TornFcaUi.add(this,r,TornFcaUi.card(this,"ACCESS","Unable to continue",message,TornFcaUi.RED));setContentView(s);s.requestApplyInsets();});}
}

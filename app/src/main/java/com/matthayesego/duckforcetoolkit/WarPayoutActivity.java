package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Leadership-only ranked-war payout calculator and Torn payment handoff. */
public class WarPayoutActivity extends Activity {
    private static final int BG=Color.rgb(5,8,12),PANEL=Color.rgb(12,18,26),PANEL2=Color.rgb(8,13,20),BORDER=Color.rgb(36,47,61),TEXT=Color.rgb(246,248,251),MUTED=Color.rgb(145,155,169),GOLD=Color.rgb(241,190,86),BLUE=Color.rgb(82,153,235),GREEN=Color.rgb(76,190,102),RED=Color.rgb(239,88,82);
    private SecureApiKeyStore keyStore;
    private int factionId;
    private String factionName,position;
    private JSONArray history=new JSONArray();
    private JSONObject selectedReport;
    private final List<MemberMetric> metrics=new ArrayList<>();
    private boolean detailedLogsLoaded=false;
    private String detailedLogsError=null;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        keyStore=new SecureApiKeyStore(this);
        factionId=getIntent().getIntExtra(FactionOpsActivity.EXTRA_FACTION_ID,0);
        factionName=getIntent().getStringExtra(FactionOpsActivity.EXTRA_FACTION_NAME);
        position=getIntent().getStringExtra(DeveloperConsoleActivity.EXTRA_POSITION);
        if(factionName==null||factionName.isBlank())factionName="Faction";
        if(position==null)position="Member";
        if(!AccessPolicy.isLeaderPosition(position)){renderError("WarPay is currently restricted to faction leadership.");return;}
        showLoading("Loading completed ranked wars…");loadHistory();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int a,int b,int stroke,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0f,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView eyebrow(String value,int color){TextView t=text(value,9.5f,color,true);t.setLetterSpacing(.12f);return t;}
    private Button button(String value,int accent){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(TEXT);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(PANEL2,accent,13));return b;}
    private EditText moneyField(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setTextSize(14);e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);e.setPadding(dp(13),0,dp(13),0);e.setBackground(rounded(PANEL2,BORDER,11));return e;}
    private EditText noteField(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setTextSize(13);e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);e.setPadding(dp(13),0,dp(13),0);e.setBackground(rounded(PANEL2,BORDER,11));return e;}
    @SuppressWarnings("deprecation") private ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setBackgroundColor(BG);int l=dp(18),t=dp(14),r=dp(18),b=dp(28);s.setPadding(l,t,r,b);s.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private LinearLayout card(int accent){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(gradient(PANEL,PANEL2,accent==Color.TRANSPARENT?BORDER:accent,20));return c;}
    private void add(LinearLayout root,LinearLayout c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(12);root.addView(c,p);}

    private void header(LinearLayout r){
        Button back=button("← War Center",BORDER);back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(dp(140),dp(44)));
        TextView e=eyebrow("LEADERSHIP • WARPAY",GOLD);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ep.topMargin=dp(18);r.addView(e,ep);
        r.addView(text("WarPay",31,TEXT,true));
        TextView sub=text("Weight ranked-war hits, outside hits and respect earned. Add member-specific cash penalties, then hand each payment to Torn for final confirmation.",13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(5);sp.bottomMargin=dp(18);r.addView(sub,sp);
    }
    private void showLoading(String msg){ScrollView s=shell();LinearLayout r=root(s);header(r);LinearLayout c=card(BORDER);c.addView(eyebrow("LOADING",GOLD));c.addView(text(msg,19,TEXT,true));add(r,c);setContentView(s);s.requestApplyInsets();}

    private void loadHistory(){
        String key=keyStore.load();if(key==null){renderError("Reconnect your Torn API key first.");return;}
        new Thread(()->{try{
            if(factionId<=0){JSONObject f=TornApiClient.getJson("/user/faction",key).optJSONObject("faction");if(f!=null)factionId=f.optInt("id",0);}
            JSONArray h=TornApiClient.getJson("/faction/"+factionId+"/rankedwars?limit=8",key).optJSONArray("rankedwars");history=h==null?new JSONArray():h;
            runOnUiThread(this::renderWarPicker);
        }catch(Exception e){renderError(e.getMessage()==null?"Unable to load ranked-war history.":e.getMessage());}}).start();
    }

    private void renderWarPicker(){
        ScrollView s=shell();LinearLayout r=root(s);header(r);
        LinearLayout note=card(GOLD);note.addView(eyebrow("STEP 1",GOLD));note.addView(text("Choose a completed war",20,TEXT,true));note.addView(text("The ranked-war report provides official war hits. TornFCA then reads the same war window from your faction attack log to derive outside hits and respect earned.",13,MUTED,false));add(r,note);
        if(history.length()==0){LinearLayout none=card(BORDER);none.addView(text("No completed ranked wars returned.",17,TEXT,true));add(r,none);}
        for(int i=0;i<history.length();i++){
            JSONObject w=history.optJSONObject(i);if(w==null)continue;int id=w.optInt("id",0);JSONArray fs=w.optJSONArray("factions");String opponent="Opponent";int our=0,their=0,winner=w.isNull("winner")?0:w.optInt("winner",0);
            for(int j=0;fs!=null&&j<fs.length();j++){JSONObject f=fs.optJSONObject(j);if(f==null)continue;if(f.optInt("id",0)==factionId)our=f.optInt("score",0);else{opponent=f.optString("name",opponent);their=f.optInt("score",0);}}
            String result=winner==factionId?"WIN":winner==0?"DRAW":"LOSS";int accent="WIN".equals(result)?GREEN:"LOSS".equals(result)?RED:MUTED;
            LinearLayout c=card(Color.TRANSPARENT);c.addView(eyebrow(result+" • WAR #"+id,accent));c.addView(text("vs "+opponent,19,TEXT,true));c.addView(text("Final "+our+" – "+their,13,MUTED,false));Button use=button("Use This War",accent);LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));up.topMargin=dp(10);c.addView(use,up);use.setOnClickListener(v->loadReport(id));
            if(WarPayoutReceiptStore.has(this,id)){Button receipt=button("View Saved Receipt",GREEN);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));rp.topMargin=dp(7);c.addView(receipt,rp);receipt.setOnClickListener(v->openReceipt(id));}
            add(r,c);
        }
        setContentView(s);s.requestApplyInsets();
    }

    private void loadReport(int warId){
        showLoading("Loading official war #"+warId+" report…");String key=keyStore.load();
        new Thread(()->{try{
            JSONObject root=TornApiClient.getJson("/faction/"+warId+"/rankedwarreport",key);JSONObject report=root.optJSONObject("rankedwarreport");if(report==null)throw new Exception("Ranked-war report was empty.");selectedReport=report;
            loadDetailedMetrics(key);
            runOnUiThread(this::renderCalculator);
        }catch(Exception e){renderError(e.getMessage()==null?"Unable to load war report.":e.getMessage());}}).start();
    }

    private void loadDetailedMetrics(String key){
        metrics.clear();detailedLogsLoaded=false;detailedLogsError=null;
        JSONObject ours=findOurFaction(selectedReport);if(ours==null)return;
        JSONArray members=ours.optJSONArray("members");
        for(int i=0;members!=null&&i<members.length();i++){
            JSONObject m=members.optJSONObject(i);if(m==null)continue;
            metrics.add(new MemberMetric(m.optInt("id",0),m.optString("name","Member"),m.optInt("attacks",0),m.optDouble("score",0)));
        }
        long start=selectedReport.optLong("start",0),end=selectedReport.optLong("end",0);
        if(start<=0||end<=start){detailedLogsError="The war report did not include a usable start/end window.";return;}
        try{
            String path="/faction/attacks?filters=outgoing&from="+start+"&to="+end+"&sort=ASC&limit=100";
            JSONArray attacks=TornApiClient.getPagedArray(path,key,"attacks",120);
            for(int i=0;i<attacks.length();i++){
                JSONObject a=attacks.optJSONObject(i);if(a==null)continue;JSONObject attacker=a.optJSONObject("attacker");if(attacker==null)continue;JSONObject af=attacker.optJSONObject("faction");if(af==null||af.optInt("id",0)!=factionId)continue;
                MemberMetric row=findMetric(attacker.optInt("id",0));if(row==null)continue;
                double respect=Math.max(0d,a.optDouble("respect_gain",0d));boolean ranked=a.optBoolean("is_ranked_war",false);
                if(!ranked&&respect>0d)row.outsideHits++;
                if(respect>0d)row.respectEarned+=respect;
            }
            detailedLogsLoaded=true;
        }catch(Exception e){detailedLogsError=e.getMessage()==null?"Detailed faction attack data was unavailable.":e.getMessage();}
    }

    private MemberMetric findMetric(int playerId){for(MemberMetric m:metrics)if(m.id==playerId)return m;return null;}

    private void renderCalculator(){
        ScrollView s=shell();LinearLayout r=root(s);header(r);JSONObject ours=findOurFaction(selectedReport);if(ours==null){renderError("Your faction was not found in this report.");return;}
        JSONArray memberArray=ours.optJSONArray("members");int participantCount=memberArray==null?0:memberArray.length();
        LinearLayout summary=card(GREEN);summary.addView(eyebrow("WAR #"+selectedReport.optInt("id",0)+" • PARTICIPATION LOADED",GREEN));summary.addView(text(ours.optInt("attacks",0)+" ranked-war hits • "+participantCount+" participating members",20,TEXT,true));summary.addView(text(detailedLogsLoaded?"Outside-hit and respect metrics loaded from the faction attack log.":"Ranked-war hits loaded. Detailed attack-log metrics are unavailable.",12.5f,detailedLogsLoaded?MUTED:RED,false));add(r,summary);
        if(!detailedLogsLoaded){LinearLayout warning=card(RED);warning.addView(eyebrow("LIMITED DATA",RED));warning.addView(text("Outside hits and respect earned need a Limited Torn key with faction API access. Those weights default to zero for this calculation.",14,TEXT,true));if(detailedLogsError!=null)warning.addView(text(detailedLogsError,12,MUTED,false));add(r,warning);}

        LinearLayout settings=card(GOLD);settings.addView(eyebrow("PAYOUT POOL",GOLD));EditText pool=moneyField("Total payout pool (e.g. 500000000)");LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52));fp.topMargin=dp(10);settings.addView(pool,fp);settings.addView(text("The three sliders are relative importance. TornFCA normalizes them automatically, so they do not need to total 100.",11.5f,MUTED,false));
        TextView mix=text("",12,GOLD,true);LinearLayout.LayoutParams mixp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);mixp.topMargin=dp(10);settings.addView(mix,mixp);
        SeekBar hitsBar=weightControl(settings,"Ranked-war hits",100);
        SeekBar outsideBar=weightControl(settings,"Outside hits",detailedLogsLoaded?100:0);
        SeekBar respectBar=weightControl(settings,"Respect earned",detailedLogsLoaded?100:0);
        updateMix(mix,hitsBar,outsideBar,respectBar);
        SeekBar.OnSeekBarChangeListener listener=new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean f){updateMix(mix,hitsBar,outsideBar,respectBar);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}};
        hitsBar.setOnSeekBarChangeListener(listener);outsideBar.setOnSeekBarChangeListener(listener);respectBar.setOnSeekBarChangeListener(listener);
        add(r,settings);

        LinearLayout penaltyIntro=card(BLUE);penaltyIntro.addView(eyebrow("CUSTOM PENALTIES",BLUE));penaltyIntro.addView(text("Enter any cash deduction beside a member who broke faction rules. Penalties reduce that member's payout and stay with the faction; they are not redistributed to other members.",13,TEXT,false));add(r,penaltyIntro);
        for(MemberMetric m:metrics){
            LinearLayout c=card(Color.TRANSPARENT);c.addView(text(m.name,17,TEXT,true));c.addView(text(m.warHits+" war hits • "+m.outsideHits+" outside hits • "+String.format(Locale.US,"%.2f",m.respectEarned)+" respect",12.5f,MUTED,false));
            m.penaltyField=moneyField("Penalty amount (optional)");LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));pp.topMargin=dp(9);c.addView(m.penaltyField,pp);
            m.reasonField=noteField("Penalty reason (optional)");LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));rp.topMargin=dp(7);c.addView(m.reasonField,rp);add(r,c);
        }

        Button calc=button("Calculate WarPay",GOLD);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52));cp.bottomMargin=dp(14);r.addView(calc,cp);
        LinearLayout resultHost=new LinearLayout(this);resultHost.setOrientation(LinearLayout.VERTICAL);r.addView(resultHost,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        calc.setOnClickListener(v->calculate(pool,hitsBar,outsideBar,respectBar,resultHost));
        TextView foot=text("TornFCA calculates and queues payments; Torn still requires the authorized faction user to confirm each transfer.",10.5f,MUTED,false);foot.setGravity(Gravity.CENTER);r.addView(foot);
        setContentView(s);s.requestApplyInsets();
    }

    private SeekBar weightControl(LinearLayout parent,String label,int value){
        TextView l=text(label+" importance",13,TEXT,true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.topMargin=dp(10);parent.addView(l,lp);
        SeekBar b=new SeekBar(this);b.setMax(100);b.setProgress(value);parent.addView(b,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(38)));return b;
    }

    private void updateMix(TextView label,SeekBar hits,SeekBar outside,SeekBar respect){
        int h=hits.getProgress(),o=outside.getProgress(),r=respect.getProgress(),sum=h+o+r;
        if(sum<=0){label.setText("Effective mix: choose at least one metric");return;}
        label.setText(String.format(Locale.US,"Effective mix: Hits %.0f%% • Outside %.0f%% • Respect %.0f%%",100d*h/sum,100d*o/sum,100d*r/sum));
    }

    private JSONObject findOurFaction(JSONObject report){JSONArray fs=report==null?null:report.optJSONArray("factions");for(int i=0;fs!=null&&i<fs.length();i++){JSONObject f=fs.optJSONObject(i);if(f!=null&&f.optInt("id",0)==factionId)return f;}return null;}

    private void calculate(EditText poolField,SeekBar hitsBar,SeekBar outsideBar,SeekBar respectBar,LinearLayout host){
        host.removeAllViews();long pool;
        try{pool=parseMoney(poolField.getText().toString());if(pool<=0)throw new Exception();}catch(Exception e){Toast.makeText(this,"Enter a valid payout pool.",Toast.LENGTH_SHORT).show();return;}
        double totalHits=0,totalOutside=0,totalRespect=0;for(MemberMetric m:metrics){totalHits+=Math.max(0,m.warHits);totalOutside+=Math.max(0,m.outsideHits);totalRespect+=Math.max(0d,m.respectEarned);}
        double wh=hitsBar.getProgress(),wo=outsideBar.getProgress(),wr=respectBar.getProgress();
        if(totalHits<=0)wh=0;if(totalOutside<=0)wo=0;if(totalRespect<=0)wr=0;double activeWeight=wh+wo+wr;
        if(activeWeight<=0){Toast.makeText(this,"Choose at least one metric with available data.",Toast.LENGTH_SHORT).show();return;}
        List<PayoutRow> rows=new ArrayList<>();double totalComposite=0;
        for(MemberMetric m:metrics){double composite=0;if(wh>0)composite+=wh*(m.warHits/totalHits);if(wo>0)composite+=wo*(m.outsideHits/totalOutside);if(wr>0)composite+=wr*(m.respectEarned/totalRespect);if(composite>0){PayoutRow row=new PayoutRow(m,composite);rows.add(row);totalComposite+=composite;}}
        if(rows.isEmpty()||totalComposite<=0){Toast.makeText(this,"No members have payable metrics for these weights.",Toast.LENGTH_SHORT).show();return;}
        long assignedGross=0;
        for(int i=0;i<rows.size();i++){PayoutRow row=rows.get(i);if(i==rows.size()-1)row.gross=pool-assignedGross;else{row.gross=(long)Math.floor(pool*(row.composite/totalComposite));assignedGross+=row.gross;}long requested=parseMoneySafe(row.member.penaltyField==null?"":row.member.penaltyField.getText().toString());row.penalty=Math.min(Math.max(0,requested),row.gross);row.net=row.gross-row.penalty;row.reason=row.member.reasonField==null?"":row.member.reasonField.getText().toString().trim();}
        Collections.sort(rows,(a,b)->Long.compare(b.net,a.net));
        long totalPaid=0,totalPenalty=0;for(PayoutRow row:rows){totalPaid+=row.net;totalPenalty+=row.penalty;}
        JSONObject receipt=buildReceipt(pool,totalPaid,totalPenalty,rows);if(receipt!=null)WarPayoutReceiptStore.save(this,receipt);
        LinearLayout summary=card(GREEN);summary.addView(eyebrow("WARPAY CALCULATED",GREEN));summary.addView(text(money(totalPaid)+" queued to members",20,TEXT,true));summary.addView(text("Pool "+money(pool)+" • penalties retained "+money(totalPenalty)+" • "+rows.size()+" weighted members",12.5f,MUTED,false));
        Button copy=button("Copy Full Receipt",GREEN);LinearLayout.LayoutParams cop=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));cop.topMargin=dp(10);summary.addView(copy,cop);
        if(receipt!=null){Button view=button("View Saved Receipt",BLUE);LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));vp.topMargin=dp(7);summary.addView(view,vp);int warId=selectedReport.optInt("id",0);view.setOnClickListener(v->openReceipt(warId));copy.setOnClickListener(v->copy(WarPayoutReceiptStore.text(receipt)));}
        host.addView(summary,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        StringBuilder paste=new StringBuilder("War #").append(selectedReport.optInt("id",0)).append(" WarPay — ").append(money(totalPaid)).append(" paid from ").append(money(pool)).append(" pool\n");
        List<PayoutRow> paymentQueue=new ArrayList<>();int rank=1;
        for(PayoutRow row:rows){LinearLayout c=card(Color.TRANSPARENT);c.addView(text(rank+". "+row.member.name,17,TEXT,true));String detail=money(row.net)+" net • gross "+money(row.gross)+" • "+row.member.warHits+" hits • "+row.member.outsideHits+" outside • "+String.format(Locale.US,"%.2f",row.member.respectEarned)+" respect";if(row.penalty>0)detail+="\nPenalty: -"+money(row.penalty)+(row.reason.isEmpty()?"":" • "+row.reason);c.addView(text(detail,12.5f,row.penalty>0?GOLD:MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(10);host.addView(c,p);paste.append(rank).append(". ").append(row.member.name).append(" [").append(row.member.id).append("] — ").append(money(row.net));if(row.penalty>0)paste.append(" (penalty -").append(money(row.penalty)).append(row.reason.isEmpty()?"":", "+row.reason).append(")");paste.append("\n");if(row.net>0)paymentQueue.add(row);rank++;}
        if(receipt==null){String out=paste.toString().trim();copy.setOnClickListener(v->copy(out));}
        if(!paymentQueue.isEmpty()){
            LinearLayout pay=card(BLUE);pay.addView(eyebrow("TORN PAYMENT HANDOFF",BLUE));pay.addView(text("Open each calculated transfer directly in Torn's faction Give to User screen with the player and amount prefilled. Confirm the payment in Torn, return here, then open the next one.",13,TEXT,false));
            Button next=button("Open Payment 1 of "+paymentQueue.size()+" in Torn",BLUE);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));np.topMargin=dp(10);pay.addView(next,np);final int[] index={0};next.setOnClickListener(v->{if(index[0]>=paymentQueue.size()){next.setText("Payment Queue Opened");next.setEnabled(false);return;}PayoutRow row=paymentQueue.get(index[0]);openTornPayment(row.member.id,row.net);index[0]++;if(index[0]<paymentQueue.size())next.setText("Open Payment "+(index[0]+1)+" of "+paymentQueue.size()+" in Torn");else next.setText("All Payments Opened — tap to finish");});
            Button reset=button("Reset Payment Queue",BORDER);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));rp.topMargin=dp(8);pay.addView(reset,rp);reset.setOnClickListener(v->{index[0]=0;next.setEnabled(true);next.setText("Open Payment 1 of "+paymentQueue.size()+" in Torn");});LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);pp.topMargin=dp(12);host.addView(pay,pp);
        }
    }

    private JSONObject buildReceipt(long pool,long totalPaid,long totalPenalty,List<PayoutRow> rows){
        try{JSONObject receipt=new JSONObject();receipt.put("war_id",selectedReport==null?0:selectedReport.optInt("id",0));receipt.put("created_at",System.currentTimeMillis());receipt.put("pool",pool);receipt.put("total_paid",totalPaid);receipt.put("total_penalty",totalPenalty);receipt.put("member_count",rows.size());JSONArray items=new JSONArray();for(PayoutRow row:rows){JSONObject item=new JSONObject();item.put("player_id",row.member.id);item.put("name",row.member.name);item.put("gross",row.gross);item.put("penalty",row.penalty);item.put("net",row.net);item.put("reason",row.reason);item.put("war_hits",row.member.warHits);item.put("outside_hits",row.member.outsideHits);item.put("respect",row.member.respectEarned);items.put(item);}receipt.put("rows",items);return receipt;}catch(Exception e){Toast.makeText(this,"Payout calculated, but the local receipt could not be saved.",Toast.LENGTH_SHORT).show();return null;}
    }

    private void openReceipt(int warId){Intent i=new Intent(this,WarPayoutReceiptActivity.class);i.putExtra(WarPayoutReceiptActivity.EXTRA_WAR_ID,warId);i.putExtra(WarPayoutReceiptActivity.EXTRA_POSITION,position);startActivity(i);}
    private void openTornPayment(int playerId,long amount){
        String url="https://www.torn.com/factions.php?step=your#/tab=controls&option=give-to-user&giveMoneyTo="+playerId+"&money="+amount;
        try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));}catch(Exception e){Toast.makeText(this,"Unable to open Torn payment page.",Toast.LENGTH_SHORT).show();}
    }
    private static long parseMoney(String raw){return (long)Double.parseDouble(raw.replace(",","").replace("$","").trim());}
    private static long parseMoneySafe(String raw){try{return parseMoney(raw);}catch(Exception e){return 0L;}}
    private static String money(long value){return "$"+NumberFormat.getIntegerInstance(Locale.US).format(value);}
    private void copy(String value){ClipboardManager c=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);if(c!=null){c.setPrimaryClip(ClipData.newPlainText("WarPay",value));Toast.makeText(this,"WarPay receipt copied.",Toast.LENGTH_SHORT).show();}}
    private void renderError(String message){runOnUiThread(()->{ScrollView s=shell();LinearLayout r=root(s);header(r);LinearLayout c=card(RED);c.addView(eyebrow("WARPAY UNAVAILABLE",RED));c.addView(text(message,14,TEXT,false));Button retry=button("Back to War List",GOLD);retry.setOnClickListener(v->loadHistory());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));p.topMargin=dp(12);c.addView(retry,p);add(r,c);setContentView(s);s.requestApplyInsets();});}

    private static final class MemberMetric{
        final int id;final String name;final int warHits;final double warScore;int outsideHits=0;double respectEarned=0;EditText penaltyField,reasonField;
        MemberMetric(int id,String name,int warHits,double warScore){this.id=id;this.name=name;this.warHits=warHits;this.warScore=warScore;}
    }
    private static final class PayoutRow{
        final MemberMetric member;final double composite;long gross,penalty,net;String reason="";
        PayoutRow(MemberMetric member,double composite){this.member=member;this.composite=composite;}
    }
}

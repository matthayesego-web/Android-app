package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

public class NotificationInboxActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);render();}
    @Override protected void onResume(){super.onResume();render();}
    private void render(){
        ScrollView s=TornFcaUi.shell(this);LinearLayout r=TornFcaUi.root(this,s);
        TornFcaUi.header(this,r,"More","Notification Inbox","Faction, war, OC, chat and personal alerts that reached this device.");
        JSONArray rows=NotificationInboxStore.all(this);
        Button clear=TornFcaUi.button(this,rows.length()==0?"Inbox Empty":"Clear Inbox",TornFcaUi.RED);
        clear.setEnabled(rows.length()>0);clear.setAlpha(rows.length()>0?1f:.45f);
        clear.setOnClickListener(v->confirmClear(rows.length()));
        r.addView(clear,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,46)));
        LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(1,TornFcaUi.dp(this,12));r.addView(new android.view.View(this),gap);
        if(rows.length()==0){
            TornFcaUi.add(this,r,TornFcaUi.card(this,"ALL CLEAR","No saved notifications","Alerts that reach TornFCA will remain here until you clear them.",TornFcaUi.GREEN));
        }else for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            String type=row.optString("type","personal").toUpperCase(java.util.Locale.US);String title=row.optString("title","TornFCA");String body=row.optString("body","");long created=row.optLong("created_at",0L);
            String when=created>0?DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(created)):"";
            TornFcaUi.add(this,r,TornFcaUi.card(this,type,title,body+(when.isEmpty()?"":"\n"+when),accent(type)));
        }
        TextView foot=TornFcaUi.footer(this,"Stored only on this device • "+rows.length()+" saved");LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);fp.topMargin=TornFcaUi.dp(this,6);r.addView(foot,fp);
        setContentView(s);s.requestApplyInsets();
    }
    private void confirmClear(int count){
        if(count<=0)return;
        new AlertDialog.Builder(this)
                .setTitle("Clear notification inbox?")
                .setMessage("This will remove all "+count+" saved notifications from this device.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Clear",(dialog,which)->{NotificationInboxStore.clear(this);render();})
                .show();
    }
    private int accent(String type){if("WAR".equals(type))return TornFcaUi.RED;if("OC".equals(type)||"CHAIN".equals(type))return TornFcaUi.PURPLE;if("FACTION".equals(type)||"ANNOUNCEMENT".equals(type))return TornFcaUi.GOLD;if("CHAT".equals(type))return TornFcaUi.BLUE;return TornFcaUi.GREEN;}
}

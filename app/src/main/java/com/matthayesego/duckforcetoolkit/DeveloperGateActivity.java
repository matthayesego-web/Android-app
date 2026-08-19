package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.UUID;

/** Hidden developer entry. Beta access intentionally uses one developer password only. */
public class DeveloperGateActivity extends Activity {
    private DeveloperSessionStore sessions;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        sessions=new DeveloperSessionStore(this);
        checkExistingSession();
    }

    private boolean passwordSession(DeveloperSessionStore.Session s){return s!=null&&s.token!=null&&s.token.startsWith("password-owner:");}
    private boolean localBetaSession(DeveloperSessionStore.Session s){return s!=null&&s.token!=null&&s.token.startsWith("local-beta-owner:");}
    private boolean localBetaFallbackAvailable(){return TornFcaCommandRuntime.isBetaBuild()&&!DeveloperBackendClient.isConfigured();}

    private void checkExistingSession(){
        DeveloperSessionStore.Session s=sessions.load();
        if(s==null){renderLogin(null);return;}
        if(passwordSession(s)){openPanel();return;}
        if(localBetaSession(s)&&localBetaFallbackAvailable()){openPanel();return;}
        if(!DeveloperBackendClient.isConfigured()){sessions.clear();renderLogin(null);return;}
        new Thread(()->{
            try{DeveloperBackendClient.developerSession(s.token);runOnUiThread(this::openPanel);}
            catch(Exception e){sessions.clear();runOnUiThread(()->renderLogin("Developer session expired. Enter the password again."));}
        },"TornFCA-DevSessionCheck").start();
    }

    private void renderLogin(String error){
        ScrollView shell=TornFcaUi.shell(this);
        LinearLayout root=TornFcaUi.root(this,shell);
        TornFcaUi.header(this,root,"More","Developer Console","Hidden entry • developer password only");

        LinearLayout card=TornFcaUi.card(this,"DEVELOPER","Developer sign in","This Beta gate is intentionally simple for now. Enter the developer password to open the hidden console.",TornFcaUi.GOLD);
        EditText password=new EditText(this);
        password.setHint("Developer password");
        password.setHintTextColor(TornFcaUi.MUTED);
        password.setTextColor(TornFcaUi.TEXT);
        password.setSingleLine(true);
        password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setPadding(TornFcaUi.dp(this,14),0,TornFcaUi.dp(this,14),0);
        password.setBackground(TornFcaUi.rounded(this,TornFcaUi.PANEL2,TornFcaUi.BORDER,12));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,56));pp.topMargin=TornFcaUi.dp(this,13);card.addView(password,pp);

        Button unlock=TornFcaUi.button(this,"Open Developer Panel",TornFcaUi.GOLD);
        LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,50));up.topMargin=TornFcaUi.dp(this,12);card.addView(unlock,up);
        TextView status=TornFcaUi.text(this,error==null?(DeveloperBackendClient.isConfigured()?"Password verification uses the Developer Backend. No username or authenticator code is required.":"Developer Backend is not configured in this build."):error,12,error==null?TornFcaUi.MUTED:TornFcaUi.RED,false);
        status.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=TornFcaUi.dp(this,9);card.addView(status,sp);
        TornFcaUi.add(this,root,card);

        unlock.setEnabled(DeveloperBackendClient.isConfigured());
        unlock.setOnClickListener(v->{
            String entered=password.getText().toString();
            if(entered.isEmpty()){status.setText("Enter the developer password.");status.setTextColor(TornFcaUi.RED);return;}
            String apiKey=new SecureApiKeyStore(this).load();
            if(apiKey==null||apiKey.trim().isEmpty()){status.setText("Reconnect your Torn API key first, then try the developer password again.");status.setTextColor(TornFcaUi.RED);return;}
            unlock.setEnabled(false);status.setText("Checking developer password…");status.setTextColor(TornFcaUi.MUTED);
            new Thread(()->{
                try{
                    DeveloperBackendClient.verifyOwnerPassword(apiKey,entered);
                    long expires=System.currentTimeMillis()/1000L+12L*60L*60L;
                    sessions.save("password-owner:"+UUID.randomUUID(),"Root Admin","root",expires);
                    runOnUiThread(this::openPanel);
                }catch(Exception e){
                    String m=e.getMessage()==null?"Developer password was not accepted.":e.getMessage();
                    runOnUiThread(()->{unlock.setEnabled(true);status.setText(m);status.setTextColor(TornFcaUi.RED);password.setText("");password.requestFocus();});
                }
            },"TornFCA-DeveloperPasswordLogin").start();
        });

        if(localBetaFallbackAvailable()){
            LinearLayout fallback=TornFcaUi.card(this,"BETA FALLBACK","Backend unavailable","If the Developer Backend is temporarily unavailable, the Beta owner can still open local-only diagnostics after a fresh Torn owner verification.",TornFcaUi.BORDER);
            Button local=TornFcaUi.button(this,"Open Local Test Console",TornFcaUi.BORDER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,TornFcaUi.dp(this,48));lp.topMargin=TornFcaUi.dp(this,10);fallback.addView(local,lp);
            TextView localStatus=TornFcaUi.text(this,"This fallback cannot change remote developer policy.",11,TornFcaUi.MUTED,false);LinearLayout.LayoutParams lsp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lsp.topMargin=TornFcaUi.dp(this,8);fallback.addView(localStatus,lsp);
            local.setOnClickListener(v->openLocalBetaTestSession(local,localStatus));TornFcaUi.add(this,root,fallback);
        }

        TextView footer=TornFcaUi.footer(this,"The hidden entry remains the first layer. Stronger multi-factor developer access can be restored later if TornFCA grows into higher-risk administration.\n\nTornFCA v"+TornFcaBrand.VERSION);
        root.addView(footer,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(shell);shell.requestApplyInsets();
    }

    private void openLocalBetaTestSession(Button button,TextView status){
        if(!localBetaFallbackAvailable())return;
        String key=new SecureApiKeyStore(this).load();
        if(key==null||key.isBlank()){status.setText("Reconnect your Torn API key first.");status.setTextColor(TornFcaUi.RED);return;}
        button.setEnabled(false);status.setText("Verifying current Torn owner identity…");status.setTextColor(TornFcaUi.MUTED);
        new Thread(()->{
            try{
                AuthSession verified=TornApiClient.authenticateFreshFaction(key);
                if(verified.playerId!=BuildConfig.DEVELOPER_PLAYER_ID)throw new Exception("This temporary Beta fallback is restricted to the TornFCA owner account.");
                long expires=System.currentTimeMillis()/1000L+2L*60L*60L;
                sessions.save("local-beta-owner:"+UUID.randomUUID(),verified.playerName,"root",expires);
                runOnUiThread(this::openPanel);
            }catch(Exception e){
                String m=e.getMessage()==null?"Owner verification failed.":e.getMessage();
                runOnUiThread(()->{button.setEnabled(true);status.setText(m);status.setTextColor(TornFcaUi.RED);});
            }
        },"TornFCA-BetaOwnerDevAccess").start();
    }

    private void openPanel(){
        DeveloperSessionStore.Session session=sessions.load();
        if(session==null){renderLogin("Developer session unavailable.");return;}
        Intent i=new Intent(this,DeveloperPanelActivity.class);
        String key=new SecureApiKeyStore(this).load();
        FactionScopeCache.Scope scope=key==null?null:FactionScopeCache.load(this,key);
        if(scope!=null){i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_ID,scope.factionId);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_NAME,scope.factionName);i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_API,scope.factionApiAccess);i.putExtra(DeveloperConsoleActivity.EXTRA_POSITION,scope.position);}
        i.putExtra(DeveloperPanelActivity.EXTRA_DEVELOPER_ROLE,session.role);i.putExtra(DeveloperPanelActivity.EXTRA_DEVELOPER_USERNAME,session.username);
        startActivity(i);finish();
    }
}

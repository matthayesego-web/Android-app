package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Experimental foreground-only bridge to Torn's own faction chat UI.
 *
 * Torn owns authentication, chat transport and sending. TornFCA only hosts the actively-viewed
 * Torn page and, when Torn's faction chat DOM is present, expands that real chat box to fill this
 * activity. The WebView is deliberately blanked whenever this activity is not foreground-visible
 * so it cannot become a hidden Torn page/WebSocket collector.
 */
public final class RealTornChatActivity extends Activity {
    private static final String START_URL="https://www.torn.com/factions.php?step=your";
    private WebView webView;
    private TextView status;
    private boolean blankedForBackground=false;
    private String resumeUrl=START_URL;

    private static final String FOCUS_SCRIPT = """
        (function(){
          const marker='tornfca-real-chat-focus-v1';
          const setImportant=(el,name,value)=>{ if(el) el.style.setProperty(name,value,'important'); };
          const hasInput=(el)=>!!(el&&el.querySelector('textarea,.tt-chat-autocomplete'));
          const loginVisible=()=>/login|authenticate/i.test(location.pathname)||!!document.querySelector('input[type="password"]');
          const findFaction=()=>{
            const direct=[...document.querySelectorAll('div[id^="faction-"]')];
            const ready=direct.find(hasInput);
            if(ready) return ready;
            const chatRoot=document.querySelector('#chatRoot');
            if(chatRoot){
              const boxes=[...chatRoot.querySelectorAll('div[class*="chat-box_"],div[class*="chat-box"]')];
              const faction=boxes.find(el=>/faction/i.test(el.id||'')||/faction/i.test(el.getAttribute('data-chat')||''));
              if(faction) return faction;
            }
            return direct[0]||null;
          };
          const tryOpen=(box)=>{
            if(!box||hasInput(box)) return false;
            const title=box.querySelector('div[class*="chat-box-title_"],div[class*="chat-box-title"],[class*="chatHeader"]');
            if(title){ title.click(); return true; }
            return false;
          };
          const focus=()=>{
            const box=findFaction();
            if(!box) return loginVisible()?'LOGIN':'OPEN_FACTION_CHAT';
            if(!hasInput(box)){
              return tryOpen(box)?'OPENING':'OPEN_FACTION_CHAT';
            }
            if(box.dataset.tornfcaFocused==='1') return 'READY';
            box.dataset.tornfcaFocused='1';
            document.documentElement.dataset.tornfcaRealChat='1';
            setImportant(document.documentElement,'overflow','hidden');
            setImportant(document.body,'overflow','hidden');
            for(let p=box.parentElement;p&&p!==document.body;p=p.parentElement){
              setImportant(p,'transform','none');
              setImportant(p,'filter','none');
              setImportant(p,'perspective','none');
              setImportant(p,'overflow','visible');
            }
            setImportant(box,'position','fixed');
            setImportant(box,'inset','0');
            setImportant(box,'left','0');
            setImportant(box,'top','0');
            setImportant(box,'right','0');
            setImportant(box,'bottom','0');
            setImportant(box,'width','100vw');
            setImportant(box,'max-width','100vw');
            setImportant(box,'height','100vh');
            setImportant(box,'max-height','100vh');
            setImportant(box,'margin','0');
            setImportant(box,'border-radius','0');
            setImportant(box,'z-index','2147483000');
            setImportant(box,'background','#111820');
            const title=box.querySelector('div[class*="chat-box-title_"],div[class*="chat-box-title"],[class*="chatHeader"]');
            if(title){ setImportant(title,'width','100%'); setImportant(title,'max-width','100%'); }
            const content=box.querySelector('div[class*="chat-box-content_"],div[class*="chat-box-content"],[class*="chatContent"]');
            if(content){
              setImportant(content,'width','100%');
              setImportant(content,'max-width','100%');
              setImportant(content,'height','calc(100vh - 42px)');
              setImportant(content,'max-height','none');
            }
            const viewport=box.querySelector('div[class*="viewport_"],div[class*="viewport"]');
            if(viewport){
              setImportant(viewport,'height','calc(100vh - 112px)');
              setImportant(viewport,'max-height','none');
              setImportant(viewport,'width','100%');
            }
            const textarea=box.querySelector('textarea,.tt-chat-autocomplete');
            if(textarea){ setImportant(textarea,'width','100%'); setImportant(textarea,'max-width','100%'); }
            return 'READY';
          };
          if(!window.__tornfcaRealChatObserver){
            let timer=0;
            window.__tornfcaRealChatObserver=new MutationObserver(()=>{
              clearTimeout(timer);
              timer=setTimeout(()=>{ try{ focus(); }catch(_){} },120);
            });
            window.__tornfcaRealChatObserver.observe(document.documentElement,{subtree:true,childList:true});
          }
          return focus();
        })();
        """;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(TornFcaUi.BG);
        getWindow().setNavigationBarColor(TornFcaUi.BG);
        build();
        configureWebView();
        webView.loadUrl(START_URL);
    }

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(TornFcaUi.BG);
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(TornFcaUi.dp(this,8),TornFcaUi.dp(this,7),TornFcaUi.dp(this,8),TornFcaUi.dp(this,7));bar.setBackground(TornFcaUi.rounded(this,TornFcaUi.PANEL,TornFcaUi.BORDER,0));
        Button back=TornFcaUi.button(this,"← Back",TornFcaUi.BORDER);back.setOnClickListener(v->goBackOrFinish());bar.addView(back,new LinearLayout.LayoutParams(TornFcaUi.dp(this,82),TornFcaUi.dp(this,42)));
        TextView title=TornFcaUi.text(this,"Real Torn Chat",16,TornFcaUi.TEXT,true);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);tp.leftMargin=TornFcaUi.dp(this,6);tp.rightMargin=TornFcaUi.dp(this,6);bar.addView(title,tp);
        Button focus=TornFcaUi.button(this,"Focus",TornFcaUi.BLUE);focus.setOnClickListener(v->focusFactionChat(true));bar.addView(focus,new LinearLayout.LayoutParams(TornFcaUi.dp(this,76),TornFcaUi.dp(this,42)));
        Button reload=TornFcaUi.button(this,"↻",TornFcaUi.GOLD);reload.setOnClickListener(v->{setStatus("Reloading Torn…",TornFcaUi.GOLD);webView.reload();});LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(TornFcaUi.dp(this,50),TornFcaUi.dp(this,42));rp.leftMargin=TornFcaUi.dp(this,6);bar.addView(reload,rp);
        root.addView(bar,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        status=TornFcaUi.text(this,"Loading Torn. First use may require your normal Torn web login.",11.5f,TornFcaUi.MUTED,false);status.setPadding(TornFcaUi.dp(this,12),TornFcaUi.dp(this,8),TornFcaUi.dp(this,12),TornFcaUi.dp(this,8));root.addView(status,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        webView=new WebView(this);root.addView(webView,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        setContentView(root);
    }

    private void configureWebView(){
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        String ua=s.getUserAgentString();
        if(ua!=null&&!ua.contains("TornFCA"))s.setUserAgentString(ua+" TornFCA-Development/"+TornFcaBrand.VERSION);
        CookieManager cookies=CookieManager.getInstance();cookies.setAcceptCookie(true);cookies.setAcceptThirdPartyCookies(webView,true);
        WebView.setWebContentsDebuggingEnabled(!BuildConfig.PLAY_STORE_BUILD);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
                Uri uri=request.getUrl();
                if(isTorn(uri))return false;
                try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){}
                return true;
            }
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap favicon){
                if(isTorn(Uri.parse(url)))resumeUrl=url;
                setStatus("Loading Torn…",TornFcaUi.MUTED);
            }
            @Override public void onPageFinished(WebView view,String url){
                if(isTorn(Uri.parse(url)))resumeUrl=url;
                setStatus("Open Torn's faction chat if it is not already open. TornFCA will focus the real chat box.",TornFcaUi.MUTED);
                view.postDelayed(()->focusFactionChat(false),500);
                view.postDelayed(()->focusFactionChat(false),1500);
                view.postDelayed(()->focusFactionChat(false),3500);
            }
        });
    }

    private boolean isTorn(Uri uri){
        if(uri==null||!"https".equalsIgnoreCase(uri.getScheme()))return false;
        String host=uri.getHost();if(host==null)return false;host=host.toLowerCase(java.util.Locale.US);
        return host.equals("torn.com")||host.endsWith(".torn.com");
    }

    private void focusFactionChat(boolean userRequested){
        if(webView==null||!isTorn(Uri.parse(webView.getUrl()==null?"":webView.getUrl()))){
            if(userRequested)Toast.makeText(this,"Load the Torn page first.",Toast.LENGTH_SHORT).show();
            return;
        }
        webView.evaluateJavascript(FOCUS_SCRIPT,result->{
            String state=result==null?"":result.replace("\"","").trim();
            switch(state){
                case "READY": setStatus("LIVE • Torn's real faction chat is focused. Messages and Send are handled by Torn.",TornFcaUi.GREEN);break;
                case "LOGIN": setStatus("SIGN IN • Complete your normal Torn web login here. TornFCA does not receive your password.",TornFcaUi.GOLD);break;
                case "OPENING": setStatus("Opening Torn faction chat…",TornFcaUi.BLUE);break;
                default: setStatus("Open the faction chat in Torn, then tap Focus. The native TornFCA chat remains available behind Back.",TornFcaUi.GOLD);break;
            }
        });
    }

    private void setStatus(String value,int color){
        if(status==null)return;status.setText(value);status.setTextColor(color);status.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
    }

    private void goBackOrFinish(){if(webView!=null&&webView.canGoBack())webView.goBack();else finish();}
    @Override public void onBackPressed(){goBackOrFinish();}

    @Override protected void onPause(){
        if(webView!=null&&!isChangingConfigurations()){
            String current=webView.getUrl();if(current!=null&&isTorn(Uri.parse(current)))resumeUrl=current;
            webView.stopLoading();
            webView.loadUrl("about:blank");
            blankedForBackground=true;
        }
        if(webView!=null)webView.onPause();
        super.onPause();
    }

    @Override protected void onResume(){
        super.onResume();
        if(webView!=null)webView.onResume();
        if(blankedForBackground&&webView!=null){blankedForBackground=false;webView.loadUrl(isTorn(Uri.parse(resumeUrl))?resumeUrl:START_URL);}
    }

    @Override protected void onDestroy(){
        if(webView!=null){
            webView.stopLoading();webView.loadUrl("about:blank");webView.clearHistory();webView.removeAllViews();webView.destroy();webView=null;
        }
        super.onDestroy();
    }
}

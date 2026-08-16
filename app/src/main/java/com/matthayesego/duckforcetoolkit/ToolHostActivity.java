package com.matthayesego.duckforcetoolkit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.webkit.WebViewAssetLoader;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolHostActivity extends Activity {
    public static final String EXTRA_TOOL="tool";
    private static final String APP_VERSION="0.9.13";
    private static final String MANAGED_KEY_SENTINEL="TORNFCA_MANAGED_KEY";
    private static final int BG=Color.rgb(8,12,18),PANEL=Color.rgb(20,27,38),TEXT=Color.rgb(245,248,252);
    private WebView webView;private SecureApiKeyStore keyStore;
    private enum Tool{ARMORY("Faction Armory Auditor","armory.duckforce.app","armory_log.html"),TRAIN("Company Train Calculator","train.duckforce.app","train_calculator.html");final String title,domain,asset;Tool(String t,String d,String a){title=t;domain=d;asset=a;}}
    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);keyStore=new SecureApiKeyStore(this);Tool tool;try{tool=Tool.valueOf(getIntent().getStringExtra(EXTRA_TOOL));}catch(Exception e){finish();return;}openTool(tool);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    @SuppressWarnings("deprecation") private void applyInsets(LinearLayout view){int l=view.getPaddingLeft(),t=view.getPaddingTop(),r=view.getPaddingRight(),b=view.getPaddingBottom();view.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});}
    @SuppressLint("SetJavaScriptEnabled") private void openTool(Tool tool){
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(BG);applyInsets(page);
        LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(8),dp(7),dp(12),dp(7));toolbar.setBackgroundColor(PANEL);
        Button back=new Button(this);back.setText("← Companion");back.setAllCaps(false);back.setTextColor(TEXT);back.setBackgroundColor(Color.TRANSPARENT);back.setOnClickListener(v->finish());toolbar.addView(back,new LinearLayout.LayoutParams(dp(118),dp(44)));
        TextView title=new TextView(this);title.setText(tool.title);title.setTextColor(TEXT);title.setTextSize(15);title.setSingleLine(true);title.setPadding(dp(8),0,0,0);toolbar.addView(title,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));page.addView(toolbar);
        webView=new WebView(this);WebSettings settings=webView.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);if(Build.VERSION.SDK_INT>=26)settings.setSafeBrowsingEnabled(true);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);settings.setSupportZoom(true);settings.setUseWideViewPort(true);settings.setUserAgentString(settings.getUserAgentString()+" TornFCA/"+APP_VERSION);CookieManager.getInstance().setAcceptThirdPartyCookies(webView,false);
        WebViewAssetLoader loader=new WebViewAssetLoader.Builder().setDomain(tool.domain).addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        webView.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                Uri uri=request.getUrl();String host=uri.getHost();
                if("api.torn.com".equalsIgnoreCase(host)&&"GET".equalsIgnoreCase(request.getMethod()))return proxyTornApi(uri);
                WebResourceResponse local=loader.shouldInterceptRequest(uri);if(local!=null)return local;
                return blockedResponse(403,"Blocked","Embedded TornFCA tools cannot make arbitrary network requests.");
            }
            @Override public void onPageFinished(WebView view,String url){super.onPageFinished(view,url);if(url!=null&&url.contains(tool.domain))injectManagedKey(view);}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                Uri uri=request.getUrl();String host=uri.getHost();
                if(host!=null&&(host.equalsIgnoreCase(tool.domain)||host.equalsIgnoreCase("api.torn.com")))return false;
                String scheme=uri.getScheme();
                if("http".equalsIgnoreCase(scheme)||"https".equalsIgnoreCase(scheme)){try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,uri));}catch(Exception ignored){}return true;}
                return true;
            }
        });
        page.addView(webView,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));setContentView(page);page.requestApplyInsets();webView.loadUrl("https://"+tool.domain+"/assets/tools/"+tool.asset);
    }
    /** The real Torn key never enters JavaScript/DOM; JS only receives a non-secret sentinel. */
    private void injectManagedKey(WebView view){String key=keyStore.load();if(key==null||key.isEmpty())return;String js="(function(){var e=document.getElementById('key');if(e){e.value='"+MANAGED_KEY_SENTINEL+"';e.readOnly=true;e.placeholder='Using Companion API key';e.title='Key managed securely by TornFCA';}})();";view.evaluateJavascript(js,null);}
    private WebResourceResponse proxyTornApi(Uri uri){
        try{
            String key=keyStore.load();if(key==null||key.isBlank())return blockedResponse(401,"No API key","Reconnect your Torn API key.");
            Uri.Builder safe=uri.buildUpon().clearQuery();
            for(String name:uri.getQueryParameterNames()){
                if("key".equalsIgnoreCase(name))continue;
                List<String> values=uri.getQueryParameters(name);if(values.isEmpty())safe.appendQueryParameter(name,"");else for(String value:values)safe.appendQueryParameter(name,value);
            }
            JSONObject json=TornApiClient.getJsonAbsolute(safe.build().toString(),key);
            return jsonResponse(200,"OK",json.toString());
        }catch(Exception e){return jsonResponse(429,"Torn API unavailable",errorEnvelope(e.getMessage()==null?"Torn API request failed.":e.getMessage()));}
    }
    private String errorEnvelope(String message){try{return new JSONObject().put("error",new JSONObject().put("error",message)).toString();}catch(Exception ignored){return "{\"error\":{\"error\":\"Torn API request failed.\"}}";}}
    private WebResourceResponse jsonResponse(int status,String reason,String body){Map<String,String> headers=new HashMap<>();headers.put("Access-Control-Allow-Origin","*");headers.put("Access-Control-Allow-Methods","GET, OPTIONS");headers.put("Cache-Control","no-store");return new WebResourceResponse("application/json","UTF-8",status,reason,headers,new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));}
    private WebResourceResponse blockedResponse(int status,String reason,String message){try{return jsonResponse(status,reason,new JSONObject().put("error",message).toString());}catch(Exception e){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)));}}
    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(webView!=null){webView.stopLoading();webView.setWebViewClient(null);webView.destroy();webView=null;}super.onDestroy();}
}

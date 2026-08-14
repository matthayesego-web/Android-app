package com.matthayesego.duckforcetoolkit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolHostActivity extends Activity {
    public static final String EXTRA_TOOL="tool";
    private static final String APP_VERSION="0.4.5";
    private static final int BG=Color.rgb(8,12,18),PANEL=Color.rgb(20,27,38),TEXT=Color.rgb(245,248,252);
    private WebView webView;private SecureApiKeyStore keyStore;
    private enum Tool{ARMORY("Faction Armory Auditor","armory.duckforce.app","armory_log.html"),TRAIN("Company Train Calculator","train.duckforce.app","train_calculator.html");final String title,domain,asset;Tool(String t,String d,String a){title=t;domain=d;asset=a;}}
    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);keyStore=new SecureApiKeyStore(this);Tool tool;try{tool=Tool.valueOf(getIntent().getStringExtra(EXTRA_TOOL));}catch(Exception e){finish();return;}openTool(tool);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    @SuppressWarnings("deprecation") private void applyInsets(LinearLayout view){int l=view.getPaddingLeft(),t=view.getPaddingTop(),r=view.getPaddingRight(),b=view.getPaddingBottom();view.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});}
    @SuppressLint("SetJavaScriptEnabled") private void openTool(Tool tool){LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(BG);applyInsets(page);LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(8),dp(7),dp(12),dp(7));toolbar.setBackgroundColor(PANEL);Button back=new Button(this);back.setText("← Companion");back.setAllCaps(false);back.setTextColor(TEXT);back.setBackgroundColor(Color.TRANSPARENT);back.setOnClickListener(v->finish());toolbar.addView(back,new LinearLayout.LayoutParams(dp(118),dp(44)));TextView title=new TextView(this);title.setText(tool.title);title.setTextColor(TEXT);title.setTextSize(15);title.setSingleLine(true);title.setPadding(dp(8),0,0,0);toolbar.addView(title,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));page.addView(toolbar);webView=new WebView(this);WebSettings settings=webView.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);settings.setSupportZoom(true);settings.setUseWideViewPort(true);settings.setUserAgentString(settings.getUserAgentString()+" DuckForceCompanion/"+APP_VERSION);WebViewAssetLoader loader=new WebViewAssetLoader.Builder().setDomain(tool.domain).addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();webView.setWebViewClient(new WebViewClient(){@Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){Uri uri=request.getUrl();if("api.torn.com".equalsIgnoreCase(uri.getHost())&&"GET".equalsIgnoreCase(request.getMethod())){WebResourceResponse response=proxyTornApi(uri);if(response!=null)return response;}return loader.shouldInterceptRequest(uri);}@Override public void onPageFinished(WebView view,String url){super.onPageFinished(view,url);if(url!=null&&url.contains(tool.domain))injectKey(view);}});page.addView(webView,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));setContentView(page);page.requestApplyInsets();webView.loadUrl("https://"+tool.domain+"/assets/tools/"+tool.asset);}
    private void injectKey(WebView view){String key=keyStore.load();if(key==null||key.isEmpty())return;String js="(function(){var e=document.getElementById('key');if(e){e.value="+JSONObject.quote(key)+";e.readOnly=true;e.placeholder='Using Companion API key';}})();";view.evaluateJavascript(js,null);}
    private WebResourceResponse proxyTornApi(Uri uri){HttpURLConnection connection=null;try{connection=(HttpURLConnection)new URL(uri.toString()).openConnection();connection.setRequestMethod("GET");connection.setConnectTimeout(15000);connection.setReadTimeout(30000);connection.setUseCaches(false);connection.setRequestProperty("Accept","application/json, text/plain, */*");connection.setRequestProperty("User-Agent","DuckForceCompanion/"+APP_VERSION+" Android");int status=connection.getResponseCode();InputStream raw=status>=400?connection.getErrorStream():connection.getInputStream();if(raw==null)return null;final HttpURLConnection finalConnection=connection;InputStream stream=new FilterInputStream(raw){@Override public void close()throws IOException{try{super.close();}finally{finalConnection.disconnect();}}};Map<String,String> headers=new HashMap<>();for(Map.Entry<String,List<String>> entry:connection.getHeaderFields().entrySet())if(entry.getKey()!=null&&entry.getValue()!=null&&!entry.getValue().isEmpty())headers.put(entry.getKey(),entry.getValue().get(0));headers.put("Access-Control-Allow-Origin","*");headers.put("Cache-Control","no-store");String reason=connection.getResponseMessage();if(reason==null||reason.isEmpty())reason=status>=400?"Error":"OK";return new WebResourceResponse("application/json","UTF-8",status,reason,headers,stream);}catch(Exception e){if(connection!=null)connection.disconnect();return null;}}
    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(webView!=null){webView.stopLoading();webView.destroy();webView=null;}super.onDestroy();}
}

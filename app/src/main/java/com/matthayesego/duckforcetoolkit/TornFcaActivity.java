package com.matthayesego.duckforcetoolkit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Public TornFCA shell. Existing v0.9 functionality remains underneath this brand/theme layer. */
public class TornFcaActivity extends V098CompanionActivity {
    private static final long AVATAR_REFRESH_MS=6L*60L*60L*1000L;
    private volatile boolean avatarRefreshInFlight=false;

    @Override public void setContentView(View view){
        super.setContentView(view);
        ViewGroup root=findViewById(android.R.id.content);
        if(root!=null){
            TornFcaBrand.apply(this,root);
            addApiRequirementNotice(root);
            retargetBankingCards(root);
            restoreProfileAvatar(root);
        }
        primeProviderConsent();
    }

    @Override protected void onResume(){
        super.onResume();
        ViewGroup root=findViewById(android.R.id.content);
        if(root!=null){
            TornFcaBrand.apply(this,root);
            addApiRequirementNotice(root);
            retargetBankingCards(root);
            restoreProfileAvatar(root);
        }
        primeProviderConsent();
        refreshPremiumEntitlement();
    }

    @Override public void startActivity(Intent intent){
        super.startActivity(TornFcaBrand.retarget(this,intent));
    }

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}

    /** Adds Torn's key-use disclosure directly beside the API-key input. */
    private void addApiRequirementNotice(View root){
        TextView title=findText(root,"Connect your Torn account");
        if(title==null||!(title.getParent() instanceof LinearLayout))return;
        LinearLayout card=(LinearLayout)title.getParent();
        for(int i=0;i<card.getChildCount();i++)if("tornfca-api-requirement".equals(card.getChildAt(i).getTag()))return;

        FactionTheme theme=FactionTheme.forContext(this);
        TextView notice=new TextView(this);
        notice.setTag("tornfca-api-requirement");
        notice.setText("API KEY REQUIREMENT\nUse one 16-character Limited Access Torn API key for TornFCA. Full Access is NOT required. Leadership-only faction data additionally depends on your in-game Faction API Access permission.\n\nStorage: the key is AES-GCM encrypted on this device using Android Keystore. TornFCA sends it to Torn's official API for requested features. Shared notices/banking may temporarily send it over HTTPS to the TornFCA faction backend to verify identity/permissions; that backend does not store the key. FFScouter and TornStats receive the key only after separate explicit opt-in, under their own terms/data policies.");
        notice.setTextSize(12f);
        notice.setTextColor(Color.rgb(224,232,241));
        notice.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        notice.setLineSpacing(0f,1.12f);
        notice.setPadding(dp(13),dp(12),dp(13),dp(12));
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(Color.rgb(8,18,25));
        bg.setCornerRadius(dp(13));
        bg.setStroke(dp(1),theme.accent);
        notice.setBackground(bg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(12);p.bottomMargin=dp(3);
        card.addView(notice,Math.min(2,card.getChildCount()),p);
    }

    private void retargetBankingCards(View root){
        if(root instanceof TextView){
            TextView t=(TextView)root;
            if("Banking".equals(t.getText()==null?"":t.getText().toString())){
                View card=findClickableAncestor(t);
                if(card!=null){card.setOnClickListener(v->{Intent i=new Intent(this,FeatureRouterActivity.class);i.putExtra(FeatureRouterActivity.EXTRA_TARGET,FeatureRouterActivity.TARGET_BANKING);startActivity(i);});}
            }
        }
        if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++)retargetBankingCards(g.getChildAt(i));}
    }

    private View findClickableAncestor(View start){View v=start;while(v!=null){if(v.isClickable())return v;if(!(v.getParent() instanceof View))break;v=(View)v.getParent();}return null;}

    /** Restores the authenticated player's Torn profile image without blocking the shell. */
    private void restoreProfileAvatar(ViewGroup root){
        ImageView avatar=findHeaderAvatar(root);if(avatar==null)return;
        String key=new SecureApiKeyStore(this).load();if(key==null||key.isBlank())return;
        int playerId=0;AuthSession hot=TornApiClient.cachedSession(key);if(hot!=null)playerId=hot.playerId;
        if(playerId<=0){FactionScopeCache.Scope scope=FactionScopeCache.load(this,key);if(scope!=null)playerId=scope.playerId;}
        if(playerId<=0)return;
        File cached=avatarFile(playerId);
        if(cached.exists()){
            Bitmap local=BitmapFactory.decodeFile(cached.getAbsolutePath());
            if(local!=null)applyAvatar(avatar,local);
            if(local!=null&&System.currentTimeMillis()-cached.lastModified()<AVATAR_REFRESH_MS)return;
        }
        if(avatarRefreshInFlight)return;avatarRefreshInFlight=true;final int id=playerId;
        new Thread(()->{
            try{
                // Always use Torn's dedicated v2 profile endpoint. Do not reuse the generic
                // profile/faction response because those selection schemas have changed separately.
                JSONObject rootJson=TornApiClient.getJson("/user/profile",key);
                JSONObject profile=rootJson.optJSONObject("profile");
                if(profile==null)profile=rootJson;
                String image=firstNonBlank(profile.optString("image",""),profile.optString("profile_image",""));
                image=normalizeImageUrl(image);
                if(image.isEmpty())return;
                BitmapDownload result=downloadBitmap(image);if(result==null||result.bitmap==null)return;
                try(FileOutputStream out=new FileOutputStream(avatarFile(id))){out.write(result.bytes);}
                runOnUiThread(()->{ViewGroup current=findViewById(android.R.id.content);ImageView currentAvatar=current==null?null:findHeaderAvatar(current);if(currentAvatar!=null)applyAvatar(currentAvatar,result.bitmap);});
            }catch(Exception ignored){}finally{avatarRefreshInFlight=false;}
        },"TornFCA-ProfileAvatar").start();
    }

    private void applyAvatar(ImageView target,Bitmap bitmap){target.setPadding(0,0,0,0);target.setScaleType(ImageView.ScaleType.CENTER_CROP);target.setImageBitmap(bitmap);}
    private String firstNonBlank(String a,String b){if(a!=null&&!a.trim().isEmpty()&&!"null".equalsIgnoreCase(a.trim()))return a.trim();if(b!=null&&!b.trim().isEmpty()&&!"null".equalsIgnoreCase(b.trim()))return b.trim();return"";}
    private String normalizeImageUrl(String raw){if(raw==null)return"";String value=raw.trim();if(value.isEmpty()||"null".equalsIgnoreCase(value))return"";if(value.startsWith("//"))value="https:"+value;else if(value.startsWith("/"))value="https://www.torn.com"+value;else if(value.startsWith("http://profileimages.torn.com/"))value="https://"+value.substring("http://".length());try{URL url=new URL(value);return"https".equalsIgnoreCase(url.getProtocol())?value:"";}catch(Exception e){return"";}}

    private File avatarFile(int playerId){return new File(getCacheDir(),"torn-profile-"+playerId+".img");}

    private BitmapDownload downloadBitmap(String value)throws Exception{
        URL url=new URL(value);if(!"https".equalsIgnoreCase(url.getProtocol()))return null;
        HttpURLConnection c=(HttpURLConnection)url.openConnection();
        try{
            c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setUseCaches(true);c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent","Mozilla/5.0 (Android) TornFCA/0.9.15");
            c.setRequestProperty("Accept","image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
            c.setRequestProperty("Referer","https://www.torn.com/");
            int code=c.getResponseCode();if(code<200||code>=300)return null;
            String contentType=c.getContentType();if(contentType!=null&&!contentType.toLowerCase().startsWith("image/"))return null;
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int n,total=0;while((n=in.read(buffer))!=-1){total+=n;if(total>4*1024*1024)return null;out.write(buffer,0,n);}byte[] bytes=out.toByteArray();Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);return bitmap==null?null:new BitmapDownload(bitmap,bytes);
            }
        }finally{c.disconnect();}
    }

    private ImageView findHeaderAvatar(View view){
        if(view instanceof ImageView){
            if("tornfca-profile-avatar".equals(view.getTag()))return(ImageView)view;
            ViewGroup.LayoutParams p=view.getLayoutParams();if(p!=null&&Math.abs(p.width-dp(78))<=dp(2)&&Math.abs(p.height-dp(78))<=dp(2))return(ImageView)view;
        }
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){ImageView found=findHeaderAvatar(g.getChildAt(i));if(found!=null)return found;}}
        return null;
    }

    /** Loads only local consent state so provider clients can enforce opt-in at their network boundary. */
    private void primeProviderConsent(){String key=new SecureApiKeyStore(this).load();if(key!=null&&!key.isBlank())FFScouterClient.hasConsent(this,key);}

    private void refreshPremiumEntitlement(){
        String key=new SecureApiKeyStore(this).load();if(key==null||key.isBlank()||!PremiumBackendClient.isConfigured())return;
        int playerId=0;FactionScopeCache.Scope scope=FactionScopeCache.load(this,key);if(scope!=null)playerId=scope.playerId;AuthSession hot=TornApiClient.cachedSession(key);if(playerId<=0&&hot!=null)playerId=hot.playerId;if(playerId<=0)return;
        PremiumBackendClient.refreshAsync(this,playerId);
    }

    private TextView findText(View view,String needle){
        if(view instanceof TextView){TextView t=(TextView)view;if(needle.equals(t.getText()==null?"":t.getText().toString()))return t;}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView found=findText(g.getChildAt(i),needle);if(found!=null)return found;}}
        return null;
    }

    private static final class BitmapDownload{final Bitmap bitmap;final byte[] bytes;BitmapDownload(Bitmap bitmap,byte[] bytes){this.bitmap=bitmap;this.bytes=bytes;}}
}

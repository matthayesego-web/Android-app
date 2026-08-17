package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Reusable command-console primitives for the Torn FCA Beta shell. */
final class TornFcaCommandUi {
    static final int BG=Color.rgb(3,6,10);
    static final int BG_2=Color.rgb(6,10,16);
    static final int PANEL=Color.rgb(10,16,24);
    static final int PANEL_2=Color.rgb(13,20,31);
    static final int PANEL_3=Color.rgb(18,26,39);
    static final int LINE=Color.rgb(43,55,74);
    static final int LINE_SOFT=Color.rgb(26,35,49);
    static final int TEXT=Color.rgb(246,247,250);
    static final int MUTED=Color.rgb(149,160,179);
    static final int STEEL=Color.rgb(112,128,151);
    static final int GOLD=Color.rgb(238,185,83);
    static final int GOLD_2=Color.rgb(255,214,132);
    static final int PURPLE=Color.rgb(147,89,246);
    static final int PURPLE_2=Color.rgb(180,120,255);
    static final int GREEN=Color.rgb(78,190,129);
    static final int RED=Color.rgb(226,91,100);
    static final int BLUE=Color.rgb(84,151,222);

    private TornFcaCommandUi(){}

    static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}

    static TextView text(Context c,String value,float sp,int color,boolean bold){
        TextView t=new TextView(c);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        t.setLineSpacing(0f,1.08f);
        return t;
    }

    static GradientDrawable gradient(Context c,int[] colors,float radiusDp,int strokeColor,float strokeDp){
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,colors);
        g.setCornerRadius(dp(c,(int)radiusDp));
        if(strokeColor!=Color.TRANSPARENT&&strokeDp>0)g.setStroke(Math.max(1,dp(c,(int)strokeDp)),strokeColor);
        return g;
    }

    static GradientDrawable solid(Context c,int color,float radiusDp,int strokeColor,float strokeDp){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c,(int)radiusDp));
        if(strokeColor!=Color.TRANSPARENT&&strokeDp>0)g.setStroke(Math.max(1,dp(c,(int)strokeDp)),strokeColor);
        return g;
    }

    static Drawable ripple(Context c,int color,float radius){
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(72,Color.red(color),Color.green(color),Color.blue(color))),null,solid(c,Color.WHITE,radius,Color.TRANSPARENT,0));
    }

    static LinearLayout vertical(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
    static LinearLayout horizontal(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.HORIZONTAL);return v;}

    static LinearLayout sectionHeading(Context c,String title,String subtitle,int accent){
        LinearLayout row=horizontal(c);row.setGravity(Gravity.CENTER_VERTICAL);
        View bar=new View(c);bar.setBackground(solid(c,accent,2,Color.TRANSPARENT,0));
        row.addView(bar,new LinearLayout.LayoutParams(dp(c,4),dp(c,38)));
        LinearLayout copy=vertical(c);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);cp.leftMargin=dp(c,10);row.addView(copy,cp);
        TextView h=text(c,title,26,TEXT,true);copy.addView(h);
        if(subtitle!=null&&!subtitle.isBlank()){
            TextView s=text(c,subtitle,12.8f,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(c,4);copy.addView(s,sp);
        }
        return row;
    }

    static LinearLayout quickTile(Activity a,int icon,String title,String subtitle,int accent,Runnable action){
        LinearLayout box=vertical(a);box.setGravity(Gravity.START);box.setPadding(dp(a,13),dp(a,13),dp(a,11),dp(a,11));
        box.setBackground(gradient(a,new int[]{PANEL_2,PANEL},17,Color.argb(175,Color.red(LINE),Color.green(LINE),Color.blue(LINE)),1));
        box.setClickable(true);box.setFocusable(true);box.setForeground(ripple(a,accent,17));box.setElevation(dp(a,2));box.setOnClickListener(v->action.run());
        ImageView iv=new ImageView(a);iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);Drawable d=a.getDrawable(icon);if(d!=null){d=d.mutate();d.setTint(accent);iv.setImageDrawable(d);}box.addView(iv,new LinearLayout.LayoutParams(dp(a,28),dp(a,28)));
        TextView h=text(a,title,13.2f,TEXT,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(a,8);box.addView(h,hp);
        TextView s=text(a,subtitle,10.5f,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(a,3);box.addView(s,sp);
        TextView arrow=text(a,"→",18,accent,false);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);ap.topMargin=dp(a,4);box.addView(arrow,ap);
        return box;
    }

    static LinearLayout actionRow(Activity a,int icon,String title,String subtitle,String value,int accent,Runnable action){
        LinearLayout row=horizontal(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(a,13),dp(a,11),dp(a,12),dp(a,11));
        row.setBackground(solid(a,Color.argb(88,Color.red(PANEL_3),Color.green(PANEL_3),Color.blue(PANEL_3)),14,LINE_SOFT,1));
        row.setClickable(true);row.setFocusable(true);row.setForeground(ripple(a,accent,14));row.setOnClickListener(v->action.run());
        ImageView iv=new ImageView(a);Drawable d=a.getDrawable(icon);if(d!=null){d=d.mutate();d.setTint(accent);iv.setImageDrawable(d);}row.addView(iv,new LinearLayout.LayoutParams(dp(a,25),dp(a,25)));
        LinearLayout copy=vertical(a);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);cp.leftMargin=dp(a,11);row.addView(copy,cp);
        copy.addView(text(a,title,13.5f,TEXT,true));TextView sub=text(a,subtitle,10.5f,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(a,2);copy.addView(sub,sp);
        if(value!=null&&!value.isBlank()){TextView v=text(a,value,12,accent,true);v.setGravity(Gravity.END);row.addView(v,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));}
        TextView chevron=text(a,"›",24,STEEL,false);chevron.setGravity(Gravity.CENTER);LinearLayout.LayoutParams xp=new LinearLayout.LayoutParams(dp(a,24),dp(a,32));xp.leftMargin=dp(a,5);row.addView(chevron,xp);
        return row;
    }

    static FrameLayout featuredPanel(Activity a,String badge,String title,String body,String button,int accent,Runnable action){
        FrameLayout frame=new FrameLayout(a);frame.setBackground(gradient(a,new int[]{Color.rgb(20,16,43),PANEL_2,PANEL},22,Color.argb(185,Color.red(accent),Color.green(accent),Color.blue(accent)),1));frame.setClipToOutline(true);frame.setElevation(dp(a,4));
        CommandArtView art=new CommandArtView(a,accent);FrameLayout.LayoutParams artp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(a,210));frame.addView(art,artp);
        LinearLayout copy=vertical(a);copy.setPadding(dp(a,17),dp(a,17),dp(a,17),dp(a,17));FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);frame.addView(copy,cp);
        TextView tag=text(a,badge,9.5f,PURPLE_2,true);tag.setLetterSpacing(.10f);tag.setPadding(dp(a,7),dp(a,4),dp(a,7),dp(a,4));tag.setBackground(solid(a,Color.argb(120,70,35,126),7,Color.argb(130,180,120,255),1));copy.addView(tag,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView h=text(a,title,25,TEXT,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(a,230),ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(a,11);copy.addView(h,hp);
        TextView b=text(a,body,12,MUTED,false);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(a,220),ViewGroup.LayoutParams.WRAP_CONTENT);bp.topMargin=dp(a,6);copy.addView(b,bp);
        TextView cta=text(a,button+"   →",12.5f,TEXT,true);cta.setGravity(Gravity.CENTER);cta.setPadding(dp(a,14),dp(a,10),dp(a,14),dp(a,10));cta.setBackground(gradient(a,new int[]{Color.rgb(76,40,145),Color.rgb(52,29,109)},12,accent,1));cta.setClickable(true);cta.setFocusable(true);cta.setForeground(ripple(a,accent,12));cta.setOnClickListener(v->action.run());LinearLayout.LayoutParams ctp=new LinearLayout.LayoutParams(dp(a,210),dp(a,44));ctp.topMargin=dp(a,14);copy.addView(cta,ctp);
        return frame;
    }

    static LinearLayout metricTile(Activity a,String eyebrow,String center,String title,String detail,int accent){
        LinearLayout tile=horizontal(a);tile.setGravity(Gravity.CENTER_VERTICAL);tile.setPadding(dp(a,10),dp(a,11),dp(a,10),dp(a,11));tile.setBackground(gradient(a,new int[]{PANEL_2,PANEL},16,Color.argb(165,Color.red(accent),Color.green(accent),Color.blue(accent)),1));
        RingView ring=new RingView(a,accent,center);tile.addView(ring,new LinearLayout.LayoutParams(dp(a,72),dp(a,72)));
        LinearLayout copy=vertical(a);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);cp.leftMargin=dp(a,8);tile.addView(copy,cp);
        TextView eye=text(a,eyebrow,8.5f,accent,true);eye.setLetterSpacing(.08f);copy.addView(eye);
        TextView h=text(a,title,11.8f,TEXT,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);hp.topMargin=dp(a,4);copy.addView(h,hp);
        TextView d=text(a,detail,9.8f,MUTED,false);LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);dpv.topMargin=dp(a,3);copy.addView(d,dpv);
        return tile;
    }

    static LinearLayout panel(Activity a){LinearLayout p=vertical(a);p.setPadding(dp(a,14),dp(a,14),dp(a,14),dp(a,14));p.setBackground(gradient(a,new int[]{PANEL_2,PANEL},20,LINE,1));p.setElevation(dp(a,2));return p;}

    static TextView primaryButton(Activity a,String label,int accent,Runnable action){TextView b=text(a,label,12.5f,TEXT,true);b.setGravity(Gravity.CENTER);b.setPadding(dp(a,13),dp(a,10),dp(a,13),dp(a,10));b.setBackground(gradient(a,new int[]{Color.argb(230,Color.red(accent),Color.green(accent),Color.blue(accent)),Color.rgb(38,31,25)},12,accent,1));b.setClickable(true);b.setFocusable(true);b.setForeground(ripple(a,accent,12));b.setOnClickListener(v->action.run());return b;}

    static final class CommandArtView extends View{
        private final int accent;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();
        CommandArtView(Context c,int accent){super(c);this.accent=accent;setLayerType(LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,new int[]{Color.rgb(9,11,22),Color.rgb(19,15,42),Color.rgb(4,7,12)},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setStrokeWidth(1f);p.setStyle(Paint.Style.STROKE);p.setColor(Color.argb(42,122,96,210));for(int i=1;i<9;i++){float x=w*i/9f;c.drawLine(x,0,x,h,p);}for(int j=1;j<6;j++){float y=h*j/6f;c.drawLine(0,y,w,y,p);}
            float cx=w*.74f,cy=h*.55f;p.setColor(Color.argb(75,Color.red(accent),Color.green(accent),Color.blue(accent)));for(int r=1;r<=5;r++)c.drawCircle(cx,cy,18*r,p);for(int i=0;i<12;i++){double a=Math.PI*2*i/12;c.drawLine(cx,cy,cx+(float)Math.cos(a)*w*.4f,cy+(float)Math.sin(a)*h*.8f,p);}
            p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(cx,cy,90,new int[]{Color.argb(120,Color.red(accent),Color.green(accent),Color.blue(accent)),Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawCircle(cx,cy,100,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5f);p.setColor(Color.rgb(Math.min(255,Color.red(accent)+28),Math.min(255,Color.green(accent)+28),Math.min(255,Color.blue(accent)+28)));p.setShadowLayer(15,0,0,accent);path.reset();path.moveTo(cx-28,cy-34);path.lineTo(cx,cy-18);path.lineTo(cx+28,cy-34);path.lineTo(cx+28,cy+4);path.lineTo(cx,cy+30);path.lineTo(cx-28,cy+4);path.close();c.drawPath(path,p);p.clearShadowLayer();
            p.setStrokeWidth(3f);c.drawLine(cx-14,cy-2,cx,cy+10,p);c.drawLine(cx,cy+10,cx+15,cy-8,p);p.setStyle(Paint.Style.FILL);
        }
    }

    static final class RingView extends View{
        private final int accent;private final String center;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final RectF oval=new RectF();
        RingView(Context c,int accent,String center){super(c);this.accent=accent;this.center=center;setLayerType(LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),m=Math.min(w,h);float pad=m*.12f;oval.set(pad,pad,w-pad,h-pad);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(m*.09f);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.rgb(39,46,62));c.drawArc(oval,-90,360,false,p);p.setColor(accent);p.setShadowLayer(m*.08f,0,0,accent);c.drawArc(oval,-90,270,false,p);p.clearShadowLayer();p.setStyle(Paint.Style.FILL);p.setColor(TEXT);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(m*(center.length()>5?.17f:.22f));Paint.FontMetrics fm=p.getFontMetrics();c.drawText(center,w/2f,h/2f-(fm.ascent+fm.descent)/2f,p);}
    }
}

package com.jarvis.kisisel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.View;

/** PANDORA neon core. Subtle animation at rest, faster light/sound waves while active. */
public final class OrbView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int accent=0xFF9B65FF, secondary=0xFF24BAFF;
    private boolean active=false;
    private long epoch=SystemClock.uptimeMillis();

    public OrbView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setContentDescription("Pandora yapay zekâ çekirdeği; aktifken dönen ışıklı halkalar");
    }
    public void setActive(boolean value) {active=value;invalidate();}
    public void setAccent(int main,int other) {accent=main;secondary=other;invalidate();}
    private static int fade(int color,int alpha) {
        return (color&0x00FFFFFF)|((alpha&0xff)<<24);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float x=getWidth()/2f,y=getHeight()/2f;
        float r=Math.min(getWidth(),getHeight())*.42f;
        if(r<=0)return;
        float t=(SystemClock.uptimeMillis()-epoch)/1000f;
        float phase=t*(active?95f:22f);
        float breath=(float)Math.sin(t*(active?5.2:1.45))*.028f;
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(x,y,r*1.42f,
            new int[]{fade(accent,25),fade(secondary,12),Color.TRANSPARENT},
            new float[]{0f,.62f,1f},Shader.TileMode.CLAMP));
        c.drawCircle(x,y,r*1.42f,p);p.setShader(null);

        // Long-exposure luminescent rings instead of a static logo.
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);
        for(int ring=0;ring<3;ring++){
            float rr=r*(1.01f+ring*.135f+breath);
            p.setStrokeWidth(Math.max(1.2f,r*(.027f-ring*.006f)));
            p.setColor(fade(ring==1?secondary:accent,ring==0?80:25));
            c.drawCircle(x,y,rr,p);
        }
        for(int ring=0;ring<3;ring++){
            float rr=r*(.76f+ring*.15f+breath);
            p.setStrokeWidth(r*(ring==0?.052f:.034f));
            p.setColor(ring==1?secondary:accent);
            p.setShadowLayer(r*.14f,0,0,p.getColor());
            float start=(ring%2==0?phase:-phase)+ring*92f;
            c.drawArc(x-rr,y-rr,x+rr,y+rr,start,130f+ring*13f,false,p);
            c.drawArc(x-rr,y-rr,x+rr,y+rr,start+181f,65f+ring*9f,false,p);
            p.clearShadowLayer();
        }
        // Particle points trace separate orbits.
        p.setStyle(Paint.Style.FILL);
        for(int i=0;i<14;i++){
            double a=Math.toRadians(i*25.714+phase*(i%2==0?1:-.72));
            float rr=r*(.82f+(i%3)*.12f);
            p.setColor(fade(i%2==0?secondary:accent,135+i%3*34));
            p.setShadowLayer(r*.09f,0,0,p.getColor());
            c.drawCircle(x+(float)Math.cos(a)*rr,y+(float)Math.sin(a)*rr,r*(i%4==0?.026f:.014f),p);
            p.clearShadowLayer();
        }
        p.setShader(new RadialGradient(x-r*.12f,y-r*.17f,r*.70f,
            new int[]{fade(secondary,190),fade(accent,215),0xFF101326},
            new float[]{0f,.40f,1f},Shader.TileMode.CLAMP));
        c.drawCircle(x,y,r*.58f,p);p.setShader(null);
        p.setColor(fade(secondary,100));p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(r*.014f);c.drawCircle(x,y,r*.59f,p);

        // Preserve the P monogram in the visual identity, rather than an unrelated S.
        p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(
            x-r*.28f,y-r*.22f,x+r*.32f,y+r*.34f,
            new int[]{Color.WHITE,0xFFE2D6FF,secondary},null,Shader.TileMode.CLAMP));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(android.graphics.Typeface.create("sans-serif-black",android.graphics.Typeface.BOLD));
        p.setTextSize(r*.71f);
        p.setShadowLayer(r*.10f,0,0,accent);
        c.drawText("P",x,y+r*.245f,p);
        p.clearShadowLayer();p.setShader(null);
        // Stop redrawing when off-screen; preserve battery in background.
        if(isShown())postInvalidateDelayed(active?32:58);
    }
}
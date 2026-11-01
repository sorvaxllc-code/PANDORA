package com.jarvis.kisisel;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
public class OrbView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean active=false; private float angle=25;
    public OrbView(Context context){super(context);}
    public void setActive(boolean value){active=value;invalidate();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.80f;
        p.setShader(new RadialGradient(x,y,r*1.22f,new int[]{0x33FFFFFF,0x11FFFFFF,0x00FFFFFF},null,Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r*1.16f,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.9f);
        p.setColor(0x66888888);c.drawCircle(x,y,r,p);
        p.setColor(0xBBCCCCCC);c.drawCircle(x,y,r*.78f,p);
        p.setColor(0xFFEEEEEE);p.setStrokeWidth(4.5f);
        c.drawArc(x-r*.91f,y-r*.91f,x+r*.91f,y+r*.91f,angle,105,false,p);
        c.drawArc(x-r*.91f,y-r*.91f,x+r*.91f,y+r*.91f,angle+180,105,false,p);
        p.setStrokeWidth(2f);p.setColor(0xFFAAAAAA);
        c.drawArc(x-r*.64f,y-r*.64f,x+r*.64f,y+r*.64f,-angle,60,false,p);
        c.drawArc(x-r*.64f,y-r*.64f,x+r*.64f,y+r*.64f,-angle+150,60,false,p);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(x,y,r*.52f,new int[]{0xFFF7F7F7,0xFF777777,0xFF1B1B1B},null,Shader.TileMode.CLAMP));
        c.drawCircle(x,y,r*.49f,p);p.setShader(null);
        p.setColor(0xFF111111);p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.BOLD));
        p.setTextSize(r*.39f);c.drawText("P",x,y+r*.145f,p);
        if(active){angle+=2f;postInvalidateDelayed(30);}else angle=25;
    }
}

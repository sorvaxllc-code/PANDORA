package com.jarvis.kisisel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.os.SystemClock;

/** Low-cost animated waveform; represents assistant state, not captured microphone audio. */
public final class NeonWaveformView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color=0xFFA869FF, second=0xFF2ABFFF;
    private boolean listening=false;
    private long origin=SystemClock.uptimeMillis();
    public NeonWaveformView(Context context){super(context);}
    public void setColors(int a,int b){color=a;second=b;invalidate();}
    public void setListening(boolean active){listening=active;invalidate();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float width=getWidth(), height=getHeight();
        if(width<=0||height<=0)return;
        float now=(SystemClock.uptimeMillis()-origin)/1000f;
        float cy=height/2f;
        final int n=54;float gap=width/(n+1);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<n;i++){
            float distance=Math.abs(i-n/2f)/(n/2f);
            float envelope=(float)Math.pow(Math.max(0,1-distance),1.5);
            float oscillation=(float)(.5+.5*Math.sin(i*.84+now*(listening?7.8:2.6)));
            float heightBar=(height*.09f)+height*(listening?.43f:.29f)*envelope*(.4f+.6f*oscillation);
            paint.setColor(i%4==0?second:color);
            paint.setStrokeWidth(Math.max(2,getResources().getDisplayMetrics().density*2));
            canvas.drawLine(gap*(i+1),cy-heightBar/2,gap*(i+1),cy+heightBar/2,paint);
        }
        if(isShown())postInvalidateDelayed(listening?45:65);
    }
}
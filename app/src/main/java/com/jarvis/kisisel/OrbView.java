package com.jarvis.kisisel;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Procedural monochrome wave, no image or continuous off-screen animation. */
public class OrbView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private boolean active=false;
    public OrbView(Context context){super(context);setContentDescription("Pandora");}
    public void setActive(boolean value){active=value;invalidate();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f;
        float radius=Math.min(getWidth(),getHeight())*.29f;
        double time=active?android.os.SystemClock.uptimeMillis()/1200.0:0;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(getResources().getDisplayMetrics().density*.65f);
        for(int layer=0;layer<22;layer++){
            path.reset();
            for(int i=0;i<=180;i++){
                double angle=i*Math.PI*2/180;
                double r=radius+Math.sin(angle*3+time+layer*.13)*(radius*.14)+Math.cos(angle*5-time+layer*.09)*radius*.06+layer*.8;
                float x=cx+(float)(Math.cos(angle)*r),y=cy+(float)(Math.sin(angle)*r);
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            path.close();paint.setColor(Color.argb(75+layer*6,245,245,245));c.drawPath(path,paint);
        }
        if(active && isShown())postInvalidateOnAnimation();
    }
}

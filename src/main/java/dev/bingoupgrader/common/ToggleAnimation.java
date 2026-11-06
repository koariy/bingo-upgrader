package dev.bingoupgrader.common;
/** Reversible 280 ms frame-time transition; no dependency on tick count or frame rate. */
public final class ToggleAnimation {
    public static final long DURATION=280_000_000L;
    private double from,target;
    private long started;
    public void reset(boolean shown){from=target=shown?1:0;started=0;}
    public void setVisible(boolean shown,long now){double next=shown?1:0;if(next!=target){from=value(now);target=next;started=now;}}
    public double value(long now){double t=Math.clamp((now-started)/(double)DURATION,0,1);return from+(target-from)*VisualMath.ease(t);}
}

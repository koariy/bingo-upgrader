package dev.bingoupgrader.common;
/** Frame-time interpolation shared with deterministic checks. No game state or RNG. */
public final class VisualMath {
    private VisualMath() { }
    public static double smooth(double current,double target,double seconds,double speed){return current+(target-current)*(1-Math.exp(-Math.max(0,seconds)*speed));}
    public static double ease(double t){t=Math.clamp(t,0,1);return t*t*t*(t*(t*6-15)+10);}
    public static double ringFraction(double chance){if(chance<=0)return 0;if(chance>=1)return 1;return Math.clamp(Math.round(chance*360),1,359)/360.0;}
    public static double visualLanding(double sample,double chance){double shown=ringFraction(chance);return sample<chance?Math.min(Math.nextDown(shown),sample/chance*shown):Math.min(Math.nextDown(1.0),shown+(sample-chance)/(1-chance)*(1-shown));}
    public static double spin(double elapsed,double duration,double landing){return (6+landing)*ease(elapsed/duration);}
}

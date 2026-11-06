package dev.bingoupgrader.common;
/** One coordinate mapping for render and input, independent of physical pixel density/GUI Scale. */
public record CanvasLayout(double scale,double left,double top) {
    public static CanvasLayout fit(int screenWidth,int screenHeight,int canvasWidth,int canvasHeight){
        if(canvasWidth<=0||canvasHeight<=0)throw new IllegalArgumentException("Canvas size");
        double scale=Math.max(.05,Math.min((screenWidth-8)/(double)canvasWidth,(screenHeight-8)/(double)canvasHeight));
        return new CanvasLayout(scale,(screenWidth-canvasWidth*scale)/2,(screenHeight-canvasHeight*scale)/2);
    }
    public double x(double screenX){return (screenX-left)/scale;}
    public double y(double screenY){return (screenY-top)/scale;}
}

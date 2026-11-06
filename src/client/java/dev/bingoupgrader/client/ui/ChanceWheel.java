package dev.bingoupgrader.client.ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

/** Ring geometry is a resource atlas, not rebuilt or uploaded each frame. */
public final class ChanceWheel {
    private ChanceWheel() { }
    public static void draw(GuiGraphicsExtractor g,int x,int y,double chance,boolean waiting){
        var active=UpgradeAnimation.result();if(active!=null)chance=active.chance();
        int frame=(int)Math.round(dev.bingoupgrader.common.VisualMath.ringFraction(chance)*360);
        Ui.image(g,Ui.WHEEL,x,y,128,128,(frame%19)*128,(frame/19)*128,128,128,2432,2432);
        boolean spinning=UpgradeAnimation.spinning();var result=UpgradeAnimation.result();
        double turns=result!=null?UpgradeAnimation.turns():0;
        // Small visual detent, vanishing at the endpoint; final angle is exactly server landing.
        double jitter=spinning?Math.sin(turns*40*Math.PI*2)*.002*Math.sin(Math.PI*Math.clamp(UpgradeAnimation.elapsed()/UpgradeAnimation.DURATION,0,1)):0;
        g.pose().pushMatrix();g.pose().translate(x+64,y+64);g.pose().rotate((float)((turns+jitter)*Math.PI*2));Ui.image(g,Ui.POINTER,-8,-68,16,20,0,0,16,20,16,20);g.pose().popMatrix();
        String percent=String.format(java.util.Locale.ROOT,"%.2f%%",chance*100);
        float pulse=1.35f+(float)(.035*Math.sin(System.nanoTime()/1e9*4));var font=Minecraft.getInstance().font;
        g.pose().pushMatrix();g.pose().translate(x+64,y+51);g.pose().scale(pulse,pulse);g.centeredText(font,percent,0,0,Ui.GREEN);g.pose().popMatrix();
        Component label=Ui.tr(waiting?"waiting":spinning?"spinning":result!=null?result.won()?"success":"failure":"ready");
        Ui.center(g,label,x+64,y+77,result!=null&&!spinning&&!result.won()?Ui.RED:Ui.GOLD);
        if(result!=null&&!spinning&&UpgradeAnimation.after()<.7){double t=UpgradeAnimation.after()/.7;int alpha=(int)((1-t)*100);g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,Ui.GLOW,x,y,0f,0f,128,128,128,128,128,128,((alpha*2)<<24)|(result.won()?0x59D990:0xEF6464));}
    }
    public static void particles(GuiGraphicsExtractor g,int x,int y,boolean success){
        double age=UpgradeAnimation.after();if(UpgradeAnimation.result()==null||age>1.1)return;
        for(int i=0;i<28;i++){double a=i*2.3999632297,d=(16+(i%5)*4)*age;int px=x+(int)(Math.cos(a)*d),py=y+(int)(Math.sin(a)*d+age*age*22);int alpha=(int)(255*(1-age/1.1));g.fill(px,py,px+2,py+2,(alpha<<24)|(success?0xFFB547:0xEF6464));}
    }
}

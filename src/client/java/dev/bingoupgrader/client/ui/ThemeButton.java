package dev.bingoupgrader.client.ui;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import dev.bingoupgrader.common.VisualMath;

/** Mouse/keyboard accessible button with frame-time hover and a spring-like press offset. */
public final class ThemeButton extends AbstractWidget {
    private final Runnable action;private double hover;private long last=System.nanoTime(),pressed;
    public ThemeButton(int x,int y,int w,int h,Component title,Runnable action){super(x,y,w,h,title);this.action=action;}
    private void press(){if(active){pressed=System.nanoTime();UiSound.play("click",1);action.run();}}
    public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { }
    public void onClick(MouseButtonEvent e,boolean doubled){press();}
    public boolean keyPressed(KeyEvent e){if(active&&isFocused()&&e.isConfirmation()){press();return true;}return super.keyPressed(e);}
    protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        long now=System.nanoTime();hover=VisualMath.smooth(hover,active&&isHoveredOrFocused()?1:0,(now-last)/1e9,14);last=now;
        int offset=now-pressed<110_000_000?1:0;Ui.nine(g,hover>.2?Ui.HOVER:Ui.BUTTON,getX(),getY()+offset,width,height);
        if(hover>.01&&active)g.fill(getX()+5,getY()+height-2,getX()+5+(int)((width-10)*hover),getY()+height-1,Ui.GOLD);
        var font=net.minecraft.client.Minecraft.getInstance().font;String label=font.plainSubstrByWidth(getMessage().getString(),Math.max(1,width-6));g.centeredText(font,label,getX()+width/2,getY()+(height-8)/2+offset,active?Ui.TEXT:Ui.MUTED);
    }
    protected void updateWidgetNarration(NarrationElementOutput out){defaultButtonNarrationText(out);}
}

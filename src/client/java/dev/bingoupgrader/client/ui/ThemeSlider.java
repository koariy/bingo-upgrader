package dev.bingoupgrader.client.ui;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.function.IntConsumer;
public final class ThemeSlider extends AbstractSliderButton {
    private int maximum;private final IntConsumer changed;
    public ThemeSlider(int x,int y,int w,int max,int count,IntConsumer changed){super(x,y,w,16,Ui.tr("quantity"),(count-1)/(double)Math.max(1,max-1));maximum=max;this.changed=changed;}
    public void range(int max,int count){maximum=Math.max(1,max);value=Math.clamp((count-1)/(double)Math.max(1,maximum-1),0,1);updateMessage();}
    protected void updateMessage(){setMessage(Ui.tr("quantity_n",1+(int)Math.round(value*Math.max(0,maximum-1))));}
    protected void applyValue(){if(changed!=null)changed.accept(1+(int)Math.round(value*Math.max(0,maximum-1)));}
    public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){Ui.nine(g,Ui.SLOT,getX(),getY()+5,width,6);int pos=getX()+4+(int)((width-8)*value);g.fill(getX()+4,getY()+7,pos,getY()+10,Ui.GOLD);Ui.nine(g,Ui.HOVER,pos-3,getY()+2,7,13);}
}

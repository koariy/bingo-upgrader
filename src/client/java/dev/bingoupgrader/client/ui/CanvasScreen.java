package dev.bingoupgrader.client.ui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Logical canvas scales uniformly; pointer events undergo exactly the inverse transform. */
public abstract class CanvasScreen extends Screen {
    protected final int canvasWidth,canvasHeight;protected double scale,left,top;
    private long shown=System.nanoTime(),closing;
    private List<Component> tooltip=List.of();
    protected CanvasScreen(Component title,int w,int h){super(title);canvasWidth=w;canvasHeight=h;}
    protected void transform(){var layout=dev.bingoupgrader.common.CanvasLayout.fit(width,height,canvasWidth,canvasHeight);scale=layout.scale();left=layout.left();top=layout.top();}
    protected int logicalX(double x){return (int)((x-left)/scale);}protected int logicalY(double y){return (int)((y-top)/scale);}
    private MouseButtonEvent local(MouseButtonEvent e){return new MouseButtonEvent((e.x()-left)/scale,(e.y()-top)/scale,e.buttonInfo());}
    protected void tooltip(Component... lines){tooltip=List.of(lines);}
    protected abstract void drawCanvas(GuiGraphicsExtractor g,int mx,int my,float partial);
    public final void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        transform();g.fill(0,0,width,height,0xAA080D16);int x=logicalX(mx),y=logicalY(my);tooltip=List.of();
        double open=Math.min(1,(System.nanoTime()-shown)/220_000_000.0);double fade=closing==0?1:1-Math.min(1,(System.nanoTime()-closing)/150_000_000.0);
        g.pose().pushMatrix();g.pose().translate((float)left,(float)(top+(1-open)*10+(1-fade)*8));g.pose().scale((float)scale,(float)scale);
        Ui.panel(g,0,0,canvasWidth,canvasHeight,false);drawCanvas(g,x,y,partial);super.extractRenderState(g,x,y,partial);
        if(!tooltip.isEmpty())Ui.tooltip(g,tooltip,x,y,canvasWidth,canvasHeight);g.pose().popMatrix();
        if(open<1||fade<1){int alpha=(int)(170*(1-open*fade));g.fill((int)left,(int)top,(int)(left+canvasWidth*scale),(int)(top+canvasHeight*scale),(alpha<<24)|0x111827);}
    }
    public boolean mouseClicked(MouseButtonEvent e,boolean doubled){transform();return closing==0&&super.mouseClicked(local(e),doubled);}
    public boolean mouseReleased(MouseButtonEvent e){transform();return super.mouseReleased(local(e));}
    public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){transform();return super.mouseDragged(local(e),dx/scale,dy/scale);}
    public boolean mouseScrolled(double mx,double my,double h,double v){transform();return super.mouseScrolled((mx-left)/scale,(my-top)/scale,h,v);}
    public void onClose(){if(closing==0)closing=System.nanoTime();}
    public void tick(){super.tick();if(closing!=0&&System.nanoTime()-closing>150_000_000L)minecraft.gui.setScreen(null);}
    public boolean isPauseScreen(){return false;}
}

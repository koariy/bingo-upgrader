package dev.bingoupgrader.client.bingo;
import dev.bingoupgrader.client.network.ClientBingo;
import net.minecraft.client.gui.screens.Screen;
import dev.bingoupgrader.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;

public final class BingoHudEditor extends Screen {
    private boolean dragging;private double offsetX,offsetY;
    public BingoHudEditor(){super(Component.translatable("bingoupgrader.bingo.hud_help"));}
    protected void init(){addRenderableWidget(new ThemeButton(width/2-50,height-24,100,20,Component.translatable("gui.done"),()->minecraft.gui.setScreen(new BingoSettingsScreen())));}
    public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){g.fill(0,0,width,height,0x99111827);Ui.panel(g,6,4,width-12,24,false);g.centeredText(font,title,width/2,12,Ui.GOLD);BingoBoard.drawHud(g);super.extractRenderState(g,mx,my,partial);}
    public boolean mouseClicked(MouseButtonEvent event,boolean doubled){int s=BingoBoard.hudSize(width,height),x=BingoBoard.hudX(width,s),y=BingoBoard.hudY(height,s);if(super.mouseClicked(event,doubled))return true;if(event.button()==0 && event.x()>=x && event.x()<x+s && event.y()>=y && event.y()<y+s){dragging=true;offsetX=event.x()-x;offsetY=event.y()-y;return true;}return false;}
    public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){if(dragging){int s=BingoBoard.hudSize(width,height);ClientBingo.preferences.hudX=(event.x()-offsetX-5)/Math.max(1,width-s-10);ClientBingo.preferences.hudY=(event.y()-offsetY-5)/Math.max(1,height-s-10);ClientBingo.preferences.normalize();return true;}return super.mouseDragged(event,dx,dy);}
    public boolean mouseReleased(MouseButtonEvent event){if(dragging)mouseDragged(event,0,0);dragging=false;return super.mouseReleased(event);}
    public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){ClientBingo.preferences.hudScale+=vertical*.1;ClientBingo.preferences.normalize();return true;}
    public void removed(){ClientBingo.preferences.save();super.removed();}
    public boolean isPauseScreen(){return false;}
}

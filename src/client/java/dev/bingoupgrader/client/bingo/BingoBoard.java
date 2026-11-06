package dev.bingoupgrader.client.bingo;
import dev.bingoupgrader.client.network.ClientBingo;
import dev.bingoupgrader.client.ui.*;
import dev.bingoupgrader.common.bingo.BingoRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class BingoBoard {
    private BingoBoard() { }
    public static void draw(GuiGraphicsExtractor g,int x,int y,int size,int mouseX,int mouseY,boolean tooltips){
        var view=ClientBingo.view();if(view==null||view.card().size()!=25)return;
        int cell=Math.max(1,size/5),lines=BingoRules.winningCells(view.mask());Ui.panel(g,x-4,y-4,size+7,size+7,true);
        java.util.List<Component> tip=java.util.List.of();
        for(int i=0;i<25;i++){
            int left=x+i%5*cell,top=y+i/5*cell;boolean done=(view.mask()&(1<<i))!=0;var stack=ClientBingo.items().get(i);
            Ui.slot(g,left,top,cell-2,stack,false);
            double age=BingoVisuals.age(i);if(done){int alpha=(int)(60+Math.max(0,1-age/.65)*100);g.fill(left+3,top+3,left+cell-5,top+cell-5,(alpha<<24)|0x205C3B);g.outline(left+1,top+1,cell-4,cell-4,Ui.GREEN);}
            if((lines&(1<<i))!=0){double wave=BingoVisuals.lineAge()<2?(Math.sin(System.nanoTime()/1e9*8)+1)*.5:1;int alpha=(int)(150+wave*105);g.outline(left,top,cell-2,cell-2,(alpha<<24)|0xFFB547);}
            if(done&&cell>=12){int check=Math.min(16,cell-4);float bounce=age<.4?(float)(1+.4*Math.sin(Math.PI*age/.4)):1;g.pose().pushMatrix();g.pose().translate(left+cell-check-2,top+2);g.pose().scale(bounce,bounce);Ui.image(g,Ui.CHECK,0,0,check,check,0,0,16,16,16,16);g.pose().popMatrix();}
            if(stack.isEmpty()&&cell>=14)Ui.center(g,Component.translatable("bingoupgrader.bingo.free"),left+cell/2,top+cell/2-4,Ui.GREEN);
            if(tooltips&&mouseX>=left&&mouseX<left+cell&&mouseY>=top&&mouseY<top+cell){tip=java.util.List.of(stack.isEmpty()?Component.translatable("bingoupgrader.bingo.free"):stack.getHoverName(),Ui.tr(done?"bingo_done":"bingo_collect"));}
        }
        if(!tip.isEmpty())Ui.tooltip(g,tip,mouseX,mouseY,420,350);
    }
    public static int hudSize(int w,int h){return Math.max(10,Math.min((int)(90*ClientBingo.preferences.hudScale),Math.min(w-12,h-12))/5*5);}
    public static int hudX(int w,int size){return 5+(int)(ClientBingo.preferences.hudX*Math.max(0,w-size-10));}
    public static int hudY(int h,int size){return 5+(int)(ClientBingo.preferences.hudY*Math.max(0,h-size-10));}
    public static void drawHud(GuiGraphicsExtractor g){int size=hudSize(g.guiWidth(),g.guiHeight());draw(g,hudX(g.guiWidth(),size),hudY(g.guiHeight(),size),size,-1,-1,false);}
    public static String timer(){var v=ClientBingo.view();if(v==null)return "0:00";long seconds=v.rules().timeLimitSeconds>0?Math.max(0,v.rules().timeLimitSeconds-v.elapsedTicks()/20):v.elapsedTicks()/20;return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
}

package dev.bingoupgrader.client.bingo;
import dev.bingoupgrader.client.network.ClientBingo;
import dev.bingoupgrader.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class BingoCardScreen extends CanvasScreen {
    private final boolean scoreboard;private int page;
    public BingoCardScreen(boolean scoreboard){super(Component.translatable(scoreboard?"bingoupgrader.bingo.scoreboard":"bingoupgrader.bingo.title"),420,350);this.scoreboard=scoreboard;}
    private void button(String key,int x,int y,int w,Runnable run){addRenderableWidget(new ThemeButton(x,y,w,23,Component.translatable(key),run));}
    protected void init(){
        button("bingoupgrader.bingo.settings",12,315,128,()->ClientBingo.action("settings"));
        button(scoreboard?"bingoupgrader.bingo.title":"bingoupgrader.bingo.scoreboard",146,315,128,()->minecraft.gui.setScreen(new BingoCardScreen(!scoreboard)));
        button("gui.done",280,315,128,this::onClose);
        if(scoreboard){button("gui.back",12,285,90,()->page=Math.max(0,page-1));button("bingoupgrader.bingo.next",318,285,90,()->page++);}
    }
    protected void drawCanvas(GuiGraphicsExtractor g,int mx,int my,float partial){
        Ui.center(g,title,210,13,Ui.GOLD);var v=ClientBingo.view();if(v==null)return;
        Ui.center(g,Component.translatable("bingoupgrader.bingo.status",Component.translatable("bingoupgrader.bingo.status."+v.status()),BingoBoard.timer()),210,30,Ui.TEXT);
        if(scoreboard){int rows=10;page=Math.min(page,Math.max(0,(v.scores().size()-1)/rows));
            for(int i=page*rows;i<Math.min(v.scores().size(),(page+1)*rows);i++){var score=v.scores().get(i);int y=54+i%rows*22;Ui.nine(g,Ui.SLOT,12,y,396,20);Ui.text(g,Component.translatable("bingoupgrader.bingo.score",score.name(),score.cells(),score.lines()),20,y+6,Ui.TEXT);}
        }else if(v.card().isEmpty())Ui.center(g,Component.translatable("bingoupgrader.bingo.no_match"),210,170,Ui.MUTED);
        else BingoBoard.draw(g,85,55,250,mx,my,true);
    }
}

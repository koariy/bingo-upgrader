package dev.bingoupgrader.client.bingo;
import dev.bingoupgrader.client.network.ClientBingo;
import dev.bingoupgrader.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class BingoSettingsScreen extends CanvasScreen {
    private long revision=-1;
    public BingoSettingsScreen(){super(Component.translatable("bingoupgrader.bingo.settings"),420,330);}
    private void add(Component title,int col,int row,boolean active,Runnable action){
        int w=Math.max(30,(canvasWidth-24)/2),h=Math.clamp((canvasHeight-58)/7,14,24);
        var b=new ThemeButton(9+col*(w+6),46+row*h,w,h-2,title,action);b.active=active;addRenderableWidget(b);
    }
    private static String next(String current,List<String> values){return values.get((values.indexOf(current)+1)%values.size());}
    protected void init(){
        var v=ClientBingo.view();if(v==null)return;revision=v.controlRevision();var c=v.settings();boolean edit=v.canManage()&&!v.status().equals("running");
        add(Component.translatable("bingoupgrader.bingo.mode",Component.translatable("bingoupgrader.bingo.mode."+c.mode)),0,0,edit,()->ClientBingo.setting("mode",next(c.mode,List.of("solo","coop","competitive"))));
        add(Component.translatable("bingoupgrader.bingo.goal",Component.translatable("bingoupgrader.bingo.goal."+c.goal)),1,0,edit,()->ClientBingo.setting("goal",next(c.goal,List.of("line","two_lines","full"))));
        add(Component.translatable("bingoupgrader.bingo.free_center",Component.translatable(c.freeCenter?"options.on":"options.off")),0,1,edit,()->ClientBingo.setting("freeCenter",Boolean.toString(!c.freeCenter)));
        add(Component.translatable("bingoupgrader.bingo.surrender",Component.translatable(c.surrender?"options.on":"options.off")),1,1,edit,()->ClientBingo.setting("surrender",Boolean.toString(!c.surrender)));
        add(Component.translatable("bingoupgrader.bingo.time_minus",c.timeLimitSeconds/60),0,2,edit,()->ClientBingo.setting("timeLimitSeconds",Integer.toString(Math.max(0,c.timeLimitSeconds-300))));
        add(Component.translatable("bingoupgrader.bingo.time_plus"),1,2,edit,()->ClientBingo.setting("timeLimitSeconds",Integer.toString(Math.min(604800,c.timeLimitSeconds+300))));
        add(Component.translatable("bingoupgrader.bingo.hud",Component.translatable(ClientBingo.preferences.hudEnabled?"options.on":"options.off")),0,3,true,()->{ClientBingo.preferences.hudEnabled=!ClientBingo.preferences.hudEnabled;ClientBingo.preferences.save();rebuildWidgets();});
        add(Component.translatable("bingoupgrader.bingo.hud_edit"),1,3,true,()->minecraft.gui.setScreen(new BingoHudEditor()));
        add(Component.translatable("bingoupgrader.bingo.start"),0,4,edit,()->ClientBingo.action("start"));
        add(Component.translatable("bingoupgrader.bingo.stop"),1,4,v.canManage()&&v.status().equals("running"),()->ClientBingo.action("stop"));
        add(Component.translatable("bingoupgrader.bingo.reset"),0,5,v.canManage(),()->ClientBingo.action("reset"));
        add(Component.translatable("bingoupgrader.bingo.restart"),1,5,v.canManage(),()->ClientBingo.action("restart"));
        add(Component.translatable("bingoupgrader.bingo.title"),0,6,true,()->minecraft.gui.setScreen(new BingoCardScreen(false)));
        add(Component.translatable("gui.done"),1,6,true,this::onClose);
    }
    public void tick(){super.tick();if(ClientBingo.view()!=null && revision!=ClientBingo.view().controlRevision())rebuildWidgets();}
    protected void drawCanvas(GuiGraphicsExtractor g,int mx,int my,float partial){Ui.center(g,title,canvasWidth/2,12,Ui.GOLD);Ui.center(g,Ui.tr("settings_hint"),canvasWidth/2,29,Ui.MUTED);}
    public boolean isPauseScreen(){return false;}
}

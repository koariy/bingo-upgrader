package dev.bingoupgrader.client.ui;
import net.minecraft.client.gui.components.toasts.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class ThemeToast implements Toast {
    private final Component title;private Component body;private ItemStack icon;private boolean success;
    private Visibility visibility=Visibility.SHOW;private long started=-1;
    public ThemeToast(Component title,Component body,ItemStack icon,boolean success){this.title=title;this.body=body;this.icon=icon.copy();this.success=success;}
    public static void show(Component title,Component body,ItemStack icon,boolean success){var manager=Minecraft.getInstance().gui.toastManager();var existing=manager.getToast(ThemeToast.class,title.getString());
        if(existing!=null){existing.body=body;existing.icon=icon.copy();existing.success=success;existing.started=-1;existing.visibility=Visibility.SHOW;}
        else manager.addToast(new ThemeToast(title,body,icon,success));}
    public Object getToken(){return title.getString();}
    public int width(){return 180;}public int height(){return 48;}
    public Visibility getWantedVisibility(){return visibility;}
    public void update(ToastManager manager,long time){if(started<0)started=time;if(time-started>4000*manager.getNotificationDisplayTimeMultiplier())visibility=Visibility.HIDE;}
    public void extractRenderState(GuiGraphicsExtractor g,Font font,long time){Ui.panel(g,0,0,width(),height(),false);g.fill(2,6,4,height()-6,success?Ui.GREEN:Ui.GOLD);int x=icon.isEmpty()?12:38;if(!icon.isEmpty())Ui.slot(g,9,11,25,icon,false);g.text(font,font.plainSubstrByWidth(title.getString(),width()-x-8),x,9,Ui.GOLD);var lines=font.split(body,width()-x-8);for(int i=0;i<Math.min(2,lines.size());i++)g.text(font,lines.get(i),x,23+i*10,Ui.TEXT);}
}

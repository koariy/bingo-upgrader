package dev.bingoupgrader.client.ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Shared theme. Identifiers and textures are cached by Minecraft's resource manager. */
public final class Ui {
    public static final int GOLD=0xFFFFB547,GREEN=0xFF59D990,RED=0xFFEF6464,TEXT=0xFFE4EAF3,MUTED=0xFF91A0B4;
    public static final Identifier PANEL=id("panel"),BUTTON=id("button"),HOVER=id("button_hover"),SLOT=id("slot"),HONEY=id("honeycomb"),CHECK=id("check"),POINTER=id("pointer"),WHEEL=id("wheel_atlas"),GLOW=id("glow");
    private Ui() { }
    private static Identifier id(String s){return Identifier.fromNamespaceAndPath("bingo_upgrader","textures/gui/"+s+".png");}
    public static Component tr(String key,Object... args){return Component.translatable("bingoupgrader.ui."+key,args);}
    public static String number(double n){return String.format(Locale.ROOT,n<10?"%.2f":n<1000?"%.1f":"%.0f",n);}
    public static void image(GuiGraphicsExtractor g,Identifier texture,int x,int y,int w,int h,int u,int v,int rw,int rh,int tw,int th){g.blit(RenderPipelines.GUI_TEXTURED,texture,x,y,(float)u,(float)v,w,h,rw,rh,tw,th);}
    public static void nine(GuiGraphicsExtractor g,Identifier texture,int x,int y,int w,int h){
        int e=Math.max(1,Math.min(6,Math.min(w,h)/2));int[] dx={0,e,w-e},dy={0,e,h-e},dsx={e,w-2*e,e},dsy={e,h-2*e,e};
        for(int a=0;a<3;a++)for(int b=0;b<3;b++)if(dsx[a]>0&&dsy[b]>0)image(g,texture,x+dx[a],y+dy[b],dsx[a],dsy[b],a==0?0:a==1?6:26,b==0?0:b==1?6:26,a==1?20:6,b==1?20:6,32,32);
    }
    public static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean pattern){nine(g,PANEL,x,y,w,h);if(pattern)for(int yy=y+5;yy<y+h-5;yy+=32)for(int xx=x+5;xx<x+w-5;xx+=48){int sw=Math.min(48,x+w-5-xx),sh=Math.min(32,y+h-5-yy);image(g,HONEY,xx,yy,sw,sh,0,0,sw,sh,48,32);}}
    public static void slot(GuiGraphicsExtractor g,int x,int y,int size,ItemStack stack,boolean selected){nine(g,SLOT,x,y,size,size);if(selected)g.outline(x+1,y+1,size-2,size-2,GOLD);if(!stack.isEmpty()){float s=Math.min(1f,(size-4)/16f);g.pose().pushMatrix();g.pose().translate(x+(size-16*s)/2,y+(size-16*s)/2);g.pose().scale(s,s);g.item(stack,0,0);g.pose().popMatrix();}}
    public static void text(GuiGraphicsExtractor g,Component text,int x,int y,int color){g.text(Minecraft.getInstance().font,text,x,y,color);}
    public static void center(GuiGraphicsExtractor g,Component text,int x,int y,int color){g.centeredText(Minecraft.getInstance().font,text,x,y,color);}
    public static void small(GuiGraphicsExtractor g,Component text,int x,int y,float scale,int color){g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);g.text(Minecraft.getInstance().font,text,0,0,color);g.pose().popMatrix();}
    public static void tooltip(GuiGraphicsExtractor g,List<Component> lines,int mx,int my,int width,int height){
        var font=Minecraft.getInstance().font;int w=Math.min(width-12,lines.stream().mapToInt(font::width).max().orElse(0)+14),h=lines.size()*12+10;
        int x=Math.clamp(mx+10,4,Math.max(4,width-w-4)),y=Math.clamp(my+12,4,Math.max(4,height-h-4));
        g.nextStratum();panel(g,x,y,w,h,false);for(int i=0;i<lines.size();i++)g.text(font,font.plainSubstrByWidth(lines.get(i).getString(),w-12),x+6,y+6+i*12,i==0?GOLD:TEXT);
    }
}

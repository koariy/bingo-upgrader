package dev.bingoupgrader.client.network;
import dev.bingoupgrader.common.bingo.*;
import dev.bingoupgrader.common.network.BingoPackets.*;
import dev.bingoupgrader.client.bingo.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class ClientBingo {
    private static BingoView view;
    private static final dev.bingoupgrader.common.ToggleAnimation hudMotion=new dev.bingoupgrader.common.ToggleAnimation();
    private static List<net.minecraft.world.item.ItemStack> items=List.of();
    public static BingoClientConfig preferences;
    private static final SystemToast.SystemToastId NOTICE=new SystemToast.SystemToastId(3500);
    private ClientBingo() { }
    public static void register() {
        preferences=BingoClientConfig.load();
        var toggle=net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.bingo_upgrader.toggle_hud",org.lwjgl.glfw.GLFW.GLFW_KEY_B,dev.bingoupgrader.client.ui.UiKeys.CATEGORY));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
            while(toggle.consumeClick())if(client.player!=null&&client.gui.screen()==null)toggleHud();
        });
        ClientPlayNetworking.registerGlobalReceiver(Snapshot.TYPE,(packet,ctx)->{
            var next=BingoJson.JSON.fromJson(packet.json(),BingoView.class);
            if(next==null || next.card()==null || !(next.card().isEmpty() || next.card().size()==25) || next.scores()==null || next.scores().size()>256)throw new IllegalArgumentException("Invalid bingo view");
            next.rules().validate();next.settings().validate();
            if(view!=null && next.revision()<view.revision())return;
            if(view==null || !view.card().equals(next.card()))items=next.card().stream().map(id->{
                var key=Identifier.tryParse(id);var registry=net.minecraft.core.registries.BuiltInRegistries.ITEM;
                return key!=null && registry.containsKey(key)?new net.minecraft.world.item.ItemStack(registry.getValue(key)):net.minecraft.world.item.ItemStack.EMPTY;
            }).toList();
            dev.bingoupgrader.client.ui.BingoVisuals.update(view,next);view=next;
            switch(packet.screen()) {
                case "card"->ctx.client().gui.setScreen(new BingoCardScreen(false));
                case "scoreboard"->ctx.client().gui.setScreen(new BingoCardScreen(true));
                case "settings"->ctx.client().gui.setScreen(new BingoSettingsScreen());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(Notice.TYPE,(v,c)->{
            dev.bingoupgrader.client.ui.ThemeToast.show(Component.translatable("bingoupgrader.bingo.title"),Component.translatable("bingoupgrader.bingo.event."+v.kind(),v.player(),v.count()),net.minecraft.world.item.ItemStack.EMPTY,!v.kind().equals("timeout"));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->clear());
        ClientPlayConnectionEvents.INIT.register((h,c)->clear());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("bingo_upgrader","bingo_hud"),(g,delta)->{
            var mc=Minecraft.getInstance();
            boolean available=view!=null&&!view.card().isEmpty()&&!(mc.gui.screen() instanceof BingoHudEditor)&&!mc.gui.hud.isHidden();
            long now=System.nanoTime();hudMotion.setVisible(available&&preferences.hudEnabled,now);
            double visible=hudMotion.value(now);
            if(available&&visible>.001){
                int size=BingoBoard.hudSize(g.guiWidth(),g.guiHeight()),x=BingoBoard.hudX(g.guiWidth(),size),y=BingoBoard.hudY(g.guiHeight(),size);
                float zoom=(float)(.85+.15*visible);
                g.pose().pushMatrix();g.pose().translate(x+(1-zoom)*size/2,y+(float)((1-visible)*-22)+(1-zoom)*size/2);g.pose().scale(zoom,zoom);
                // Collapse vertically into the upper edge, then spring smoothly back to full size.
                g.pose().scale(1,(float)visible);BingoBoard.draw(g,0,0,size,-1,-1,false);g.pose().popMatrix();
            }
        });
    }
    public static void toggleHud(){preferences.hudEnabled=!preferences.hudEnabled;preferences.save();}
    public static void clear(){view=null;items=List.of();hudMotion.reset(false);}
    public static BingoView view(){return view;}
    public static List<net.minecraft.world.item.ItemStack> items(){return items;}
    public static void action(String action){send(action,"","");}
    public static void setting(String field,String value){send("set",field,value);}
    private static void send(String action,String field,String value){
        if(ClientPlayNetworking.canSend(Control.TYPE))ClientPlayNetworking.send(new Control(view==null?"":view.matchId(),view==null?0:view.controlRevision(),action,field,value));
    }
}

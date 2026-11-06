package dev.bingoupgrader.client.network;
import dev.bingoupgrader.common.network.UpgradePackets.*;
import dev.bingoupgrader.common.network.UpgradeCatalog;
import dev.bingoupgrader.common.upgrader.Offer;
import dev.bingoupgrader.client.ui.*;
import dev.bingoupgrader.client.screen.UpgraderScreen;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class ClientUpgrader {
    private static Opened opened;private static Quoted quoted;private static Result result;private static Rejected rejected;
    private static UpgradeCatalog catalog;private static long sequence,requested,quotedAt;private static boolean awaiting;private static long started;
    private ClientUpgrader() { }
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(Opened.TYPE,(v,c)->{clear();opened=v;c.client().gui.setScreen(new UpgraderScreen());});
        ClientPlayNetworking.registerGlobalReceiver(UpgradeCatalog.TYPE,(v,c)->catalog=v);
        ClientPlayNetworking.registerGlobalReceiver(Quoted.TYPE,(v,c)->{if(opened!=null&&opened.session().equals(v.session())&&(v.requestId()==requested||v.requestId()==0)){quoted=v;quotedAt=System.nanoTime();rejected=null;}});
        ClientPlayNetworking.registerGlobalReceiver(Result.TYPE,(v,c)->{if(opened!=null&&opened.session().equals(v.session())){result=v;quoted=null;awaiting=false;UpgradeAnimation.begin(v);}});
        ClientPlayNetworking.registerGlobalReceiver(Rejected.TYPE,(v,c)->{if(v.requestId()==requested||v.requestId()==0){rejected=v;quoted=null;awaiting=false;}});
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->clear());ClientPlayConnectionEvents.INIT.register((h,c)->clear());
        var category=dev.bingoupgrader.client.ui.UiKeys.CATEGORY;
        var key=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.bingo_upgrader.open",org.lwjgl.glfw.GLFW.GLFW_KEY_U,category));
        ClientTickEvents.END_CLIENT_TICK.register(client->{UpgradeAnimation.tick();if(awaiting&&System.nanoTime()-started>10_000_000_000L){awaiting=false;rejected=new Rejected("stale");}
            while(key.consumeClick())if(client.player!=null&&client.gui.screen()==null&&ClientPlayNetworking.canSend(OpenRequest.TYPE))ClientPlayNetworking.send(new OpenRequest());
        });
    }
    public static void clear(){opened=null;quoted=null;result=null;rejected=null;catalog=null;awaiting=false;requested=++sequence;UpgradeAnimation.clear();}
    public static Opened opened(){return opened;}public static Quoted quoted(){return quoted;}public static Result result(){return result;}public static Rejected rejected(){return rejected;}public static UpgradeCatalog catalog(){return catalog;}
    public static boolean busy(){return awaiting||UpgradeAnimation.spinning();}
    public static double quoteAge(){return (System.nanoTime()-quotedAt)/1e9;}
    public static boolean awaiting(){return awaiting;}
    public static void invalidate(){quoted=null;rejected=null;requested=++sequence;}
    public static long preview(List<Offer> offers,String target,int count,String mode){
        if(opened==null||busy())return -1;quoted=null;rejected=null;requested=++sequence;
        ClientPlayNetworking.send(new PreviewRequest(opened.session(),List.copyOf(offers),target,count,mode,requested));return requested;
    }
    public static void start(){if(quoted!=null&&!busy()){var q=quoted;quoted=null;result=null;awaiting=true;started=System.nanoTime();ClientPlayNetworking.send(new StartRequest(q.session(),q.token()));}}
}

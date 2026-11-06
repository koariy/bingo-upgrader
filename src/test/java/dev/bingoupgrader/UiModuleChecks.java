package dev.bingoupgrader;
import dev.bingoupgrader.common.*;
import dev.bingoupgrader.common.network.*;
import dev.bingoupgrader.common.network.UpgradePackets.*;
import dev.bingoupgrader.common.config.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.nio.file.*;

/** Presentation math and wire contracts; never fabricate a server outcome to pass a test. */
public final class UiModuleChecks {
    private static int checks;
    private static void check(boolean b){checks++;if(!b)throw new AssertionError("UI check "+checks);}
    public static void run() throws Exception {
        var motion=new ToggleAnimation();motion.setVisible(true,0);
        check(motion.value(0)==0);check(Math.abs(motion.value(ToggleAnimation.DURATION/2)-.5)<1e-10);
        check(motion.value(ToggleAnimation.DURATION)==1);
        motion.setVisible(false,ToggleAnimation.DURATION);check(motion.value(ToggleAnimation.DURATION)==1);
        long half=ToggleAnimation.DURATION+ToggleAnimation.DURATION/2;double atHalf=motion.value(half);
        motion.setVisible(true,half);check(Math.abs(motion.value(half)-atHalf)<1e-10);check(motion.value(half+ToggleAnimation.DURATION)==1);
        motion.reset(false);check(motion.value(half)==0);
        double previous=0;for(int i=0;i<=1000;i++){double v=VisualMath.ease(i/1000.0);check(v>=previous-1e-12&&v>=0&&v<=1);previous=v;}
        check(VisualMath.ease(0)==0&&VisualMath.ease(1)==1);
        for(double p:new double[]{.0005,.0036,.23,.5,.9,.9999})for(int i=0;i<1000;i++){
            double sample=(i+.5)/1000;double visual=VisualMath.visualLanding(sample,p),zone=VisualMath.ringFraction(p);
            check(visual>=0&&visual<1&&(sample<p)==(visual<zone));
            double end=VisualMath.spin(3.8,3.8,visual);check(Math.abs(end-6-visual)<1e-12);
        }
        for(double p:new double[]{.0005,.0036,.23,.9,.9999})for(double sample:new double[]{0,.5,Math.nextDown(1.0)})for(boolean won:new boolean[]{true,false}) {
            double landing=dev.bingoupgrader.common.upgrader.UpgradeMath.landing(won,p,sample);check((landing<p)==won&&landing<1);
            double shown=VisualMath.visualLanding(landing,p);check((shown<VisualMath.ringFraction(p))==won&&shown<1);
        }
        double a=0,b=0;for(int i=0;i<60;i++)a=VisualMath.smooth(a,1,1/60.0,12);for(int i=0;i<144;i++)b=VisualMath.smooth(b,1,1/144.0,12);check(Math.abs(a-b)<1e-12);
        for(int[] size:new int[][]{{320,240},{480,270},{960,540},{1920,1080},{2560,1440}}){
            var layout=CanvasLayout.fit(size[0],size[1],440,365);check(layout.left()>=0&&layout.top()>=0);
            check(Math.abs(layout.x(layout.left()+220*layout.scale())-220)<1e-9);
            check(Math.abs(layout.y(layout.top()+180*layout.scale())-180)<1e-9);
        }
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{
            var id=UUID.randomUUID();var req=new PreviewRequest(id,List.of(new dev.bingoupgrader.common.upgrader.Offer(2,16)),"minecraft:diamond",1,"manual",123456);
            PreviewRequest.CODEC.encode(buf,req);check(PreviewRequest.CODEC.decode(buf).equals(req));
            var quote=new Quoted(id,UUID.randomUUID(),"minecraft:diamond",1,16,250,.0576,false,123456);
            Quoted.CODEC.encode(buf,quote);check(Quoted.CODEC.decode(buf).equals(quote));
            var reject=new Rejected("chance",123456);Rejected.CODEC.encode(buf,reject);check(Rejected.CODEC.decode(buf).equals(reject));
            var catalog=new UpgradeCatalog(List.of("minecraft:diamond"),.1,.0005,.9,100000,7);
            UpgradeCatalog.CODEC.encode(buf,catalog);check(UpgradeCatalog.CODEC.decode(buf).equals(catalog));
        }finally{buf.release();}
        // Export defaults through the same model as runtime: examples cannot silently drift.
        Path output=Path.of("examples/config/bingoupgrader");Files.createDirectories(output);
        var json=new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        var values=new ValueConfig();values.validate();var upgrader=new UpgraderConfig();upgrader.validate();var bingo=new BingoConfig();bingo.validate();
        Files.writeString(output.resolve("values.json"),json.toJson(values));Files.writeString(output.resolve("upgrader.json"),json.toJson(upgrader));Files.writeString(output.resolve("bingo.json"),json.toJson(bingo));
        Files.writeString(output.resolve("client.json"),"{\"showItemValues\": true}\n");Files.writeString(output.resolve("bingo-client.json"),"{\"hudEnabled\":true,\"hudX\":0.02,\"hudY\":0.08,\"hudScale\":1.0}\n");
        System.out.println("UI module: "+checks+" checks passed; example configs exported");
    }
}

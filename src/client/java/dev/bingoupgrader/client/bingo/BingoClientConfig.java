package dev.bingoupgrader.client.bingo;
import dev.bingoupgrader.common.bingo.BingoJson;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
public final class BingoClientConfig {
    public boolean hudEnabled=true;
    public double hudX=.02,hudY=.08,hudScale=1;
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("bingoupgrader/bingo-client.json");}
    public static BingoClientConfig load(){try{if(Files.exists(path())){var cfg=BingoJson.JSON.fromJson(Files.readString(path()),BingoClientConfig.class);cfg.normalize();return cfg;}}catch(Exception e){dev.bingoupgrader.common.BingoUpgraderMod.LOGGER.warn("Cannot read bingo HUD preferences",e);}return new BingoClientConfig();}
    public void normalize(){if(!Double.isFinite(hudX))hudX=.02;if(!Double.isFinite(hudY))hudY=.08;if(!Double.isFinite(hudScale))hudScale=1;hudX=Math.clamp(hudX,0,1);hudY=Math.clamp(hudY,0,1);hudScale=Math.clamp(hudScale,.5,2.5);}
    public void save(){normalize();try{Files.createDirectories(path().getParent());Files.writeString(path(),BingoJson.JSON.toJson(this));}catch(Exception ex){dev.bingoupgrader.common.BingoUpgraderMod.LOGGER.warn("Cannot save bingo HUD preferences",ex);}}
}

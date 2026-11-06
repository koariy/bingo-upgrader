package dev.bingoupgrader.server.bingo;
import dev.bingoupgrader.common.config.BingoConfig;
import dev.bingoupgrader.common.value.ValueTable;
import java.util.*;
import java.util.function.Predicate;

public final class BingoGenerator {
    private BingoGenerator() { }
    /** Cheap Nether/End materials are still late-game goals despite their low numeric price. */
    public static boolean fitsDifficulty(String difficulty,String id) {
        if(!difficulty.equals("easy")||!id.startsWith("minecraft:"))return true;
        String path=id.substring("minecraft:".length());
        for(String marker:List.of("nether","crimson","warped","chorus","purpur","shulker","blaze","ghast","soul_","magma","ancient_debris","blackstone","basalt","glowstone","quartz","prismarine","sea_lantern","sponge","sculk","echo_shard","breeze","heavy_core","trial_","ominous_"))if(path.contains(marker))return false;
        return !(path.startsWith("end_")||path.startsWith("ender_")||path.equals("end_stone")||path.equals("elytra")||path.equals("dragon_breath")||path.equals("spectral_arrow")||path.equals("brewing_stand"));
    }
    public static List<String> generate(BingoConfig cfg,ValueTable values,Predicate<String> permitted,Random random) {
        cfg.validate();var result=new ArrayList<String>();
        for(var tier:cfg.tiers) {
            var candidates=new ArrayList<String>();
            values.entries().forEach((id,e)->{
                if(!e.blocked() && !e.source().equals("unknown") && e.value()>0 && e.value()>=tier.min() && e.value()<tier.max() && !cfg.blacklist.contains(id) && fitsDifficulty(cfg.displayedDifficulty(),id) && permitted.test(id))candidates.add(id);
            });
            Collections.sort(candidates);Collections.shuffle(candidates,random);
            if(candidates.size()<tier.count())throw new IllegalArgumentException("Not enough items in tier "+tier.name()+": "+candidates.size()+"/"+tier.count());
            result.addAll(candidates.subList(0,tier.count()));
        }
        Collections.shuffle(result,random);if(cfg.freeCenter)result.add(12,"");return List.copyOf(result);
    }
}

package dev.bingoupgrader.common.config;
import java.util.*;

/** Settings are frozen into each match; edits only affect future matches. */
public final class BingoConfig {
    public String mode="competitive", goal="line";
    public boolean freeCenter=false, surrender=false;
    public int timeLimitSeconds=1800;
    public List<Tier> tiers=new ArrayList<>(List.of(new Tier("easy",0,10,10),new Tier("medium",10,100,10),
        new Tier("hard",100,1000,4),new Tier("rare",1000,1e12,1)));
    public Set<String> managers=new TreeSet<>(); // UUID strings, never names.
    public Set<String> blacklist=new TreeSet<>();
    public record Tier(String name,double min,double max,int count) { }
    public void validate() {
        if(!Set.of("solo","coop","competitive").contains(mode) || !Set.of("line","two_lines","full").contains(goal)) bad();
        if(timeLimitSeconds<0 || timeLimitSeconds>604800 || tiers==null || tiers.size()!=4 || managers==null || managers.size()>256 || blacklist==null || blacklist.size()>1024) bad();
        double last=0;int total=0;Set<String> names=new HashSet<>();
        for(var t:tiers) {
            if(t==null || t.name()==null || t.name().length()>32 || !names.add(t.name()) || !Double.isFinite(t.min()) || !Double.isFinite(t.max()) || t.min()<last || t.max()<=t.min() || t.max()>1e12 || t.count()<0 || t.count()>25) bad();
            last=t.max();total+=t.count();
        }
        if(total!=(freeCenter?24:25)) bad();
        for(String id:managers) UUID.fromString(id);
        for(String id:blacklist) if(id==null || id.length()>256 || !id.contains(":" ) || net.minecraft.resources.Identifier.tryParse(id)==null) bad();
    }
    private static void bad() { throw new IllegalArgumentException("Invalid bingo configuration"); }
}

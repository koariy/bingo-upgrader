package dev.bingoupgrader.common.config;
import java.util.*;

/** Settings are frozen into each match; edits only affect future matches. */
public final class BingoConfig {
    public String mode="competitive", goal="line", difficulty="normal";
    public boolean freeCenter=false, surrender=false;
    public int timeLimitSeconds=1800;
    public List<Tier> tiers=new ArrayList<>(List.of(new Tier("easy",0,10,10),new Tier("medium",10,100,10),
        new Tier("hard",100,1000,4),new Tier("rare",1000,1e12,1)));
    public Set<String> managers=new TreeSet<>(); // UUID strings, never names.
    public Set<String> blacklist=new TreeSet<>();
    public record Tier(String name,double min,double max,int count) { }
    public static List<String> difficulties(){return List.of("easy","normal","hard","expert");}
    public static int[] distribution(String difficulty,boolean freeCenter) {
        int[] result=switch(difficulty){
            case "easy"->new int[]{20,5,0,0};
            case "normal"->new int[]{10,10,4,1};
            case "hard"->new int[]{5,10,8,2};
            case "expert"->new int[]{2,6,12,5};
            default->throw new IllegalArgumentException("Unknown bingo difficulty");
        };
        if(freeCenter)result[0]--;return result;
    }
    /** Keep custom value boundaries, but replace the tier counts for the selected preset. */
    public void applyDifficulty(String selected) {
        int[] counts=distribution(selected,freeCenter);
        if(tiers==null||tiers.size()!=4)bad();
        for(int i=0;i<4;i++){var t=tiers.get(i);tiers.set(i,new Tier(t.name(),t.min(),t.max(),counts[i]));}
        difficulty=selected;
    }
    public String displayedDifficulty(){
        if(!difficulties().contains(difficulty))return "custom";
        int[] counts=distribution(difficulty,freeCenter);
        for(int i=0;i<4;i++)if(tiers.get(i).count()!=counts[i])return "custom";
        return difficulty;
    }
    public static BingoConfig fromJson(com.google.gson.JsonObject json) {
        var cfg=dev.bingoupgrader.common.bingo.BingoJson.JSON.fromJson(json,BingoConfig.class);
        if(cfg==null)throw new IllegalArgumentException("Empty bingo config");
        if(json.has("difficulty")&&!"custom".equals(cfg.difficulty))cfg.applyDifficulty(cfg.difficulty);
        else if(!json.has("difficulty"))cfg.difficulty=cfg.displayedDifficulty();
        cfg.validate();return cfg;
    }
    public void validate() {
        if(!difficulties().contains(difficulty)&&!"custom".equals(difficulty))bad();
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

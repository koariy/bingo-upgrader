package dev.bingoupgrader.server.bingo;
import dev.bingoupgrader.common.config.BingoConfig;
import dev.bingoupgrader.common.value.ValueTable;
import java.util.*;
import java.util.function.Predicate;

public final class BingoGenerator {
    private BingoGenerator() { }
    public static List<String> generate(BingoConfig cfg,ValueTable values,Predicate<String> permitted,Random random) {
        cfg.validate();var result=new ArrayList<String>();
        for(var tier:cfg.tiers) {
            var candidates=new ArrayList<String>();
            values.entries().forEach((id,e)->{
                if(!e.blocked() && !e.source().equals("unknown") && e.value()>0 && e.value()>=tier.min() && e.value()<tier.max() && !cfg.blacklist.contains(id) && permitted.test(id))candidates.add(id);
            });
            Collections.sort(candidates);Collections.shuffle(candidates,random);
            if(candidates.size()<tier.count())throw new IllegalArgumentException("Not enough items in tier "+tier.name()+": "+candidates.size()+"/"+tier.count());
            result.addAll(candidates.subList(0,tier.count()));
        }
        Collections.shuffle(result,random);if(cfg.freeCenter)result.add(12,"");return List.copyOf(result);
    }
}

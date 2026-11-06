package dev.bingoupgrader.server.bingo;
import dev.bingoupgrader.common.bingo.*;
import net.minecraft.world.Container;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Deterministic claim transaction, shared by runtime hooks and tests; caller owns the server thread. */
public final class BingoProgress {
    private BingoProgress() { }
    public record Delta(int cells,int lines,boolean won) { }
    public static Delta claim(BingoState s,String uuid,String name,Container inventory) {
        var progress=s.players.get(uuid);
        if(!s.status.equals("running") || progress==null)return new Delta(0,0,false);
        boolean coop=s.rules.mode.equals("coop");int mask=coop?s.sharedMask:progress.mask;
        int before=BingoRules.lines(mask),added=0;
        for(int cell=0;cell<25;cell++) {
            if((mask&(1<<cell))!=0)continue;
            String wanted=s.card.get(cell);if(wanted.isEmpty())continue;
            var key=Identifier.parse(wanted);if(!BuiltInRegistries.ITEM.containsKey(key))continue;
            for(int slot=0;slot<inventory.getContainerSize();slot++) {
                var stack=inventory.getItem(slot);
                if(stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(key))continue;
                if(s.rules.surrender)inventory.removeItem(slot,1);
                mask|=1<<cell;progress.mask|=1<<cell;added++;break;
            }
        }
        if(added==0)return new Delta(0,0,false);
        if(coop)s.sharedMask=mask;else progress.mask=mask;
        int lines=BingoRules.lines(mask)-before;if(coop)progress.lineCredits+=lines;
        progress.name=name;
        boolean won=BingoRules.won(mask,s.rules.goal);
        if(won){s.status="won";s.controlRevision++;s.winners=new ArrayList<>(coop?s.players.keySet():List.of(uuid));}
        return new Delta(added,lines,won);
    }
}

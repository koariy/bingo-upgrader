package dev.bingoupgrader.common.bingo;
import dev.bingoupgrader.common.config.BingoConfig;
import java.util.*;

/** Server-owned persisted model, never shared by reference with client code. */
public final class BingoState {
    public int schemaVersion=1;
    public String matchId=UUID.randomUUID().toString(),status="idle";
    public long revision=0,controlRevision=0,elapsedTicks=0;
    public BingoConfig rules=new BingoConfig();
    public List<String> card=new ArrayList<>();
    public Map<String,Progress> players=new LinkedHashMap<>();
    public int sharedMask=0;
    public List<String> winners=new ArrayList<>();
    public static final class Progress {
        public String name;
        public int mask,lineCredits;
        public Progress(String name,int mask) {this.name=name;this.mask=mask;}
    }
    public void validate() {
        if(schemaVersion!=1 || !Set.of("idle","running","stopped","won","timeout").contains(status)) throw new IllegalArgumentException("Invalid bingo save");
        UUID.fromString(matchId);rules.validate();
        if(revision<0 || controlRevision<0 || elapsedTicks<0 || card==null || players==null || players.size()>256 || winners==null || winners.size()>256) throw new IllegalArgumentException("Invalid bingo save");
        if(!card.isEmpty() && (card.size()!=25 || new HashSet<>(card).size()!=25)) throw new IllegalArgumentException("Invalid saved card");
        if(!status.equals("idle") && card.size()!=25)throw new IllegalArgumentException("Missing card");
        if(!card.isEmpty() && rules.freeCenter && !card.get(12).isEmpty())throw new IllegalArgumentException("Invalid free center");
        for(int i=0;i<card.size();i++) if(!(rules.freeCenter && i==12 && card.get(i).isEmpty()) && net.minecraft.resources.Identifier.tryParse(card.get(i))==null) throw new IllegalArgumentException("Invalid card ID");
        if((sharedMask&~BingoRules.FULL)!=0)throw new IllegalArgumentException("Invalid mask");
        players.forEach((id,progress)->{UUID.fromString(id);if(progress==null || progress.name==null || progress.name.length()>64 || (progress.mask&~BingoRules.FULL)!=0 || progress.lineCredits<0 || progress.lineCredits>12)throw new IllegalArgumentException("Invalid progress");});
        for(String id:winners)if(!players.containsKey(id))throw new IllegalArgumentException("Invalid winner");
    }
}

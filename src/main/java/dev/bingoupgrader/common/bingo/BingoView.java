package dev.bingoupgrader.common.bingo;
import dev.bingoupgrader.common.config.BingoConfig;
import java.util.*;
/** Personalized snapshot; spectators see the same card but have no personal progress. */
public record BingoView(String matchId,long revision,long controlRevision,String status,BingoConfig rules,
    BingoConfig settings,long elapsedTicks,List<String> card,int mask,List<Score> scores,List<String> winners,boolean canManage,boolean participant) {
    public record Score(String uuid,String name,int cells,int lines) { }
}

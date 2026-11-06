package dev.bingoupgrader.common.bingo;
import java.util.*;
public final class BingoScores {
    private BingoScores() { }
    public static List<BingoView.Score> rows(BingoState state) {
        return state.players.entrySet().stream().map(e->new BingoView.Score(e.getKey(),e.getValue().name,
            BingoRules.cells(e.getValue().mask,state.rules.freeCenter),state.rules.mode.equals("coop")?e.getValue().lineCredits:BingoRules.lines(e.getValue().mask)))
            .sorted(Comparator.comparingInt(BingoView.Score::lines).reversed()
                .thenComparing(Comparator.comparingInt(BingoView.Score::cells).reversed()).thenComparing(BingoView.Score::uuid)).toList();
    }
}

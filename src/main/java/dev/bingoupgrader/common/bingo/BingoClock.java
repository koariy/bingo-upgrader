package dev.bingoupgrader.common.bingo;
/** Match time advances only while the server ticks a running match. */
public final class BingoClock {
    private BingoClock() { }
    public static boolean advance(BingoState state) {
        if(!state.status.equals("running"))return false;
        state.elapsedTicks++;
        if(state.rules.timeLimitSeconds>0 && state.elapsedTicks>=state.rules.timeLimitSeconds*20L) {
            state.status="timeout";state.controlRevision++;return true;
        }
        return false;
    }
}

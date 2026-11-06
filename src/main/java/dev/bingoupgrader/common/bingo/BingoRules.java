package dev.bingoupgrader.common.bingo;
import java.util.*;

/** Bit index = row*5+column. Twelve distinct lines, including both diagonals. */
public final class BingoRules {
    public static final int FULL=(1<<25)-1, CENTER=1<<12;
    private static final int[] LINES=createLines();
    private BingoRules() { }
    private static int[] createLines() {
        int[] lines=new int[12];
        for(int r=0;r<5;r++)for(int c=0;c<5;c++){lines[r]|=1<<(r*5+c);lines[5+c]|=1<<(r*5+c);}
        for(int i=0;i<5;i++){lines[10]|=1<<(i*5+i);lines[11]|=1<<(i*5+4-i);}return lines;
    }
    public static int lines(int mask) { return Integer.bitCount(lineFlags(mask)); }
    public static int lineFlags(int mask) {int flags=0;for(int i=0;i<12;i++)if((mask&LINES[i])==LINES[i])flags|=1<<i;return flags;}
    public static int winningCells(int mask) {int result=0;for(int line:LINES)if((mask&line)==line)result|=line;return result;}
    public static boolean won(int mask,String goal) {return switch(goal){case "line"->lines(mask)>=1;case "two_lines"->lines(mask)>=2;case "full"->mask==FULL;default->false;};}
    public static int cells(int mask,boolean free) {return Integer.bitCount(mask & (free ? FULL^CENTER : FULL));}
}

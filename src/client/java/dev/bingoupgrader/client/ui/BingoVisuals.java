package dev.bingoupgrader.client.ui;
import dev.bingoupgrader.common.bingo.*;
public final class BingoVisuals {
    private static final long[] marks=new long[25];private static long line;
    private BingoVisuals() { }
    public static void update(BingoView old,BingoView next){
        if(old==null||!old.matchId().equals(next.matchId())){java.util.Arrays.fill(marks,0);line=0;return;}
        int added=next.mask()&~old.mask();for(int i=0;i<25;i++)if((added&(1<<i))!=0)marks[i]=System.nanoTime();
        if(BingoRules.lines(next.mask())>BingoRules.lines(old.mask())){line=System.nanoTime();UiSound.play("success",1.3f);}
    }
    public static double age(int i){return marks[i]==0?100:(System.nanoTime()-marks[i])/1e9;}
    public static double lineAge(){return line==0?100:(System.nanoTime()-line)/1e9;}
}

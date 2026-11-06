package dev.bingoupgrader.client.ui;
import dev.bingoupgrader.common.network.UpgradePackets.Result;
import dev.bingoupgrader.common.VisualMath;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Server result is known before the first animation frame. Presentation never issues a reward. */
public final class UpgradeAnimation {
    public static final double DURATION=3.8;
    private static Result result;private static long started;private static boolean celebrated;private static int lastSector;
    private UpgradeAnimation() { }
    public static void begin(Result incoming){result=incoming;started=System.nanoTime();celebrated=false;lastSector=-1;}
    public static void clear(){result=null;celebrated=true;}
    public static Result result(){return result;}
    public static double elapsed(){return result==null?100:(System.nanoTime()-started)/1e9;}
    public static boolean spinning(){return result!=null&&elapsed()<DURATION;}
    public static double turns(){return result==null?0:VisualMath.spin(elapsed(),DURATION,VisualMath.visualLanding(result.landing(),result.chance()));}
    public static double after(){return Math.max(0,elapsed()-DURATION);}
    public static void tick(){if(result==null)return;
        int sector=(int)(turns()*40);
        if(spinning()&&sector!=lastSector){lastSector=sector;UiSound.play("tick",(float)(.8+Math.min(1,elapsed()/DURATION)*.5));}
        if(!spinning()&&!celebrated){celebrated=true;UiSound.play(result.won()?"success":"failure",result.won()?1.15f:.8f);
            var id=Identifier.tryParse(result.target());ItemStack item=id!=null&&BuiltInRegistries.ITEM.containsKey(id)?new ItemStack(BuiltInRegistries.ITEM.getValue(id),result.count()):ItemStack.EMPTY;
            ThemeToast.show(Ui.tr(result.won()?"success":"failure"),result.won()?Ui.tr("received",item.getHoverName(),result.count()):Ui.tr("lost"),result.won()?item:ItemStack.EMPTY,result.won());
        }
    }
}

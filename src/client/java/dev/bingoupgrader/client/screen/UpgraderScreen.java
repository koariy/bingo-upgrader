package dev.bingoupgrader.client.screen;

import dev.bingoupgrader.client.ui.*;
import dev.bingoupgrader.client.network.*;
import dev.bingoupgrader.common.upgrader.Offer;
import dev.bingoupgrader.common.VisualMath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Virtual offer slots point into real inventory. No client-side moving, debit or credit. */
public final class UpgraderScreen extends CanvasScreen {
    private final Map<Integer,Integer> offers=new TreeMap<>();
    private List<ItemStack> snapshot=List.of(),ghosts=List.of();
    private ItemStack target=new ItemStack(Items.DIAMOND);
    private int count=1,offerPage;private double input,committedInput,chance,displayChance;
    private final List<ThemeButton> quick=new ArrayList<>();
    private String mode="manual";private boolean initialized,dirty=true;
    private long changed=System.nanoTime(),sent,lastFrame=System.nanoTime();
    private long valuesRevision=-1;private UUID quoteSeen,resultSeen;private ThemeButton attempt,choose,minus,plus;private ThemeSlider slider;
    public UpgraderScreen(){super(Ui.tr("upgrader_title"),440,365);}
    protected void init(){
        if(!initialized&&minecraft.player!=null){int slot=minecraft.player.getInventory().getSelectedSlot();var stack=minecraft.player.getInventory().getItem(slot);if(!stack.isEmpty())offers.put(slot,Math.min(99,stack.getCount()));initialized=true;}
        quick.clear();
        attempt=button(12,174,130,24,Ui.tr("upgrade"),()->{
            if(ClientUpgrader.quoted()==null)return;
            ghosts=offers.keySet().stream().map(i->minecraft.player.getInventory().getItem(i).copyWithCount(offers.get(i))).toList();committedInput=input;chance=ClientUpgrader.quoted().chance();ClientUpgrader.start();
        });
        String[] modes={"x2","x4","x8","p30","p50","p70"};String[] labels={"×2","×4","×8","30%","50%","70%"};
        for(int i=0;i<6;i++){final String m=modes[i];quick.add(button(152+i*46,174,42,24,Component.literal(labels[i]),()->{if(!ClientUpgrader.busy()){mode=m;edit();}}));}
        choose=button(310,43,108,18,Ui.tr("choose"),()->{if(!ClientUpgrader.busy())minecraft.gui.setScreen(new TargetPickerScreen(this));});
        minus=button(309,139,18,18,Component.literal("−"),()->quantity(count-1));
        plus=button(400,139,18,18,Component.literal("+"),()->quantity(count+1));
        slider=addRenderableWidget(new ThemeSlider(332,140,63,target.getMaxStackSize(),count,this::quantity));
        button(280,238,148,23,Ui.tr("clear"),()->{if(!ClientUpgrader.busy()){offers.clear();edit();}});
        button(280,266,148,23,Component.translatable("bingoupgrader.bingo.title"),()->ClientBingo.action("card"));
        button(280,294,148,23,Component.translatable("bingoupgrader.bingo.settings"),()->ClientBingo.action("settings"));
        button(280,326,148,23,Component.translatable("gui.done"),this::onClose);
        refreshInventory();
    }
    private ThemeButton button(int x,int y,int w,int h,Component text,Runnable action){return addRenderableWidget(new ThemeButton(x,y,w,h,text,action));}
    public double inputValue(){return input;}public int targetCount(){return count;}
    public void selectTarget(ItemStack stack){target=stack.copyWithCount(1);count=Math.min(count,target.getMaxStackSize());mode="manual";edit();}
    private void quantity(int value){if(ClientUpgrader.busy())return;int next=Math.clamp(value,1,target.getMaxStackSize());if(count!=next){count=next;mode="manual";edit();}}
    private void edit(){ClientUpgrader.invalidate();UpgradeAnimation.clear();dirty=true;changed=System.nanoTime();revalue();}
    private void revalue(){input=0;if(minecraft.player==null)return;for(var e:offers.entrySet())input+=ClientValues.quote(minecraft.player.getInventory().getItem(e.getKey()).copyWithCount(e.getValue())).totalValue();}
    private void refreshInventory(){if(minecraft.player==null)return;boolean different=snapshot.size()!=36;
        if(!different)for(int i=0;i<36;i++)if(!ItemStack.matches(snapshot.get(i),minecraft.player.getInventory().getItem(i))){different=true;break;}
        if(different){snapshot=new ArrayList<>();for(int i=0;i<36;i++)snapshot.add(minecraft.player.getInventory().getItem(i).copy());
            offers.entrySet().removeIf(e->snapshot.get(e.getKey()).isEmpty());offers.replaceAll((i,n)->Math.min(n,snapshot.get(i).getCount()));
            if(!ClientUpgrader.busy())edit();else revalue();
        }
    }
    public void tick(){super.tick();if(minecraft.player==null)return;
        var result=ClientUpgrader.result();
        if(result!=null&&!result.token().equals(resultSeen)){resultSeen=result.token();offers.clear();dirty=false;target=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(result.target())));count=result.count();}
        refreshInventory();
        if(ClientValues.table()!=null&&valuesRevision!=ClientValues.table().revision()){valuesRevision=ClientValues.table().revision();if(!ClientUpgrader.busy())edit();}
        var q=ClientUpgrader.quoted();
        if(q!=null&&!q.token().equals(quoteSeen)){quoteSeen=q.token();chance=q.chance();target=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(q.target())));count=q.count();mode="manual";dirty=false;sent=0;}
        if(!ClientUpgrader.busy()&&q!=null&&ClientUpgrader.catalog()!=null&&ClientUpgrader.quoteAge()>ClientUpgrader.catalog().previewSeconds()*.65){edit();q=null;}
        if(!ClientUpgrader.busy()&&ClientUpgrader.rejected()!=null&&ClientUpgrader.rejected().reason().equals("stale")){edit();q=null;}
        boolean busy=ClientUpgrader.busy();attempt.active=!busy&&q!=null&&!offers.isEmpty()&&!dirty;choose.active=!busy&&ClientUpgrader.catalog()!=null;minus.active=!busy&&count>1;plus.active=!busy&&count<target.getMaxStackSize();slider.active=!busy&&target.getMaxStackSize()>1;
        slider.range(target.getMaxStackSize(),count);for(var b:quick)b.active=!busy&&!offers.isEmpty();
        if(!busy&&dirty&&System.nanoTime()-changed>180_000_000L&&!offers.isEmpty()&&ClientUpgrader.catalog()!=null){
            ClientUpgrader.preview(offers.entrySet().stream().map(e->new Offer(e.getKey(),e.getValue())).toList(),BuiltInRegistries.ITEM.getKey(target.getItem()).toString(),count,mode);dirty=false;sent=System.nanoTime();
        }
        if(ClientUpgrader.rejected()!=null)sent=0;
        if(sent!=0&&q==null&&ClientUpgrader.rejected()==null&&!busy&&System.nanoTime()-sent>2_000_000_000L){dirty=true;changed=System.nanoTime();sent=0;}
    }
    protected void drawCanvas(GuiGraphicsExtractor g,int mx,int my,float partial){
        long now=System.nanoTime();displayChance=VisualMath.smooth(displayChance,chance,(now-lastFrame)/1e9,12);lastFrame=now;
        Ui.center(g,title,220,12,Ui.GOLD);
        Ui.panel(g,12,35,130,132,true);Ui.panel(g,300,35,128,132,true);
        Ui.center(g,Ui.tr("offer"),77,42,Ui.TEXT);
        var slots=new ArrayList<>(offers.keySet());offerPage=Math.min(offerPage,Math.max(0,(slots.size()-1)/9));
        List<ItemStack> visible=ClientUpgrader.busy()&&offers.isEmpty()?ghosts:slots.stream().map(i->minecraft.player.getInventory().getItem(i).copyWithCount(offers.get(i))).toList();
        for(int n=0;n<9;n++){int index=offerPage*9+n,x=35+(n%3)*28,y=59+(n/3)*28;ItemStack stack=index<visible.size()?visible.get(index):ItemStack.EMPTY;Ui.slot(g,x,y,25,stack,!stack.isEmpty());if(!stack.isEmpty()){Ui.small(g,Component.literal(Integer.toString(stack.getCount())),x+15,y+17,.7f,Ui.TEXT);if(mx>=x&&mx<x+25&&my>=y&&my<y+25)tooltip(stack.getHoverName(),Ui.tr("remove_offer"));}}
        Ui.small(g,Ui.tr("value",Ui.number(ClientUpgrader.busy()?committedInput:input)),20,151,.85f,Ui.GOLD);
        Ui.slot(g,347,67,36,target,false);if(mx>=347&&mx<383&&my>=67&&my<103)tooltip(target.getHoverName(),Ui.tr("value",Ui.number(ClientValues.quote(target.copyWithCount(count)).totalValue())));
        Ui.center(g,Ui.tr("value",Ui.number(ClientValues.quote(target.copyWithCount(count)).totalValue())),364,109,Ui.GOLD);
        Ui.center(g,Ui.tr("quantity_n",count),364,125,Ui.TEXT);
        ChanceWheel.draw(g,156,36,displayChance,ClientUpgrader.awaiting()||sent!=0);
        if(chance>0&&chance<.01)Ui.small(g,Component.translatable("bingoupgrader.upgrade.low"),166,163,.65f,Ui.RED);
        Ui.panel(g,12,209,254,143,false);Ui.text(g,Ui.tr("inventory"),23,216,Ui.MUTED);
        if(minecraft.player!=null)for(int row=0;row<4;row++)for(int col=0;col<9;col++){
            int slot=row==3?col:9+row*9+col,x=22+col*26,y=row==3?317:231+row*26;var stack=minecraft.player.getInventory().getItem(slot);
            Ui.slot(g,x,y,24,stack,offers.containsKey(slot));if(!stack.isEmpty()){Ui.small(g,Component.literal(Integer.toString(stack.getCount())),x+14,y+17,.7f,Ui.TEXT);if(mx>=x&&mx<x+24&&my>=y&&my<y+24)tooltip(stack.getHoverName(),Ui.tr("value",Ui.number(ClientValues.quote(stack).totalValue())),Ui.tr("click_hint"));}
        }
        Ui.small(g,Ui.tr("inventory_hint"),22,344,.6f,Ui.MUTED);
        var reject=ClientUpgrader.rejected();if(reject!=null){String error=Component.translatable("bingoupgrader.upgrade.rejected."+reject.reason()).getString();Ui.small(g,Component.literal(font.plainSubstrByWidth(error,225)),280,216,.63f,Ui.RED);}else Ui.small(g,Ui.tr("server_authority"),280,216,.65f,Ui.MUTED);
        if(UpgradeAnimation.result()!=null&&!UpgradeAnimation.spinning()){
            boolean won=UpgradeAnimation.result().won();ChanceWheel.particles(g,won?220:76,won?100:95,won);
            if(won&&UpgradeAnimation.after()<.75){double t=UpgradeAnimation.after()/.75;g.pose().pushMatrix();g.pose().translate((float)(220+(365-220)*t),(float)(100+(85-100)*t-Math.sin(t*Math.PI)*45));g.pose().scale((float)(1.4-.4*t),(float)(1.4-.4*t));g.item(target,-8,-8);g.pose().popMatrix();}
        }
    }
    public boolean mouseClicked(MouseButtonEvent event,boolean doubled){if(super.mouseClicked(event,doubled))return true;if(ClientUpgrader.busy()||minecraft.player==null)return false;int mx=logicalX(event.x()),my=logicalY(event.y());
        for(int row=0;row<4;row++)for(int col=0;col<9;col++){int slot=row==3?col:9+row*9+col,x=22+col*26,y=row==3?317:231+row*26;if(mx>=x&&mx<x+24&&my>=y&&my<y+24){var stack=minecraft.player.getInventory().getItem(slot);if(stack.isEmpty())return true;int old=offers.getOrDefault(slot,0),max=Math.min(99,stack.getCount()),next=event.button()==1?event.hasShiftDown()?0:Math.max(0,old-1):event.hasShiftDown()?max:old>=max?0:old+1;if(next==0)offers.remove(slot);else offers.put(slot,next);mode="manual";edit();UiSound.play("click",1);return true;}}
        var keys=new ArrayList<>(offers.keySet());for(int n=0;n<9;n++){int index=offerPage*9+n,x=35+n%3*28,y=59+n/3*28;if(index<keys.size()&&mx>=x&&mx<x+25&&my>=y&&my<y+25){offers.remove(keys.get(index));edit();return true;}}return false;
    }
    public boolean mouseScrolled(double mx,double my,double h,double v){int x=logicalX(mx),y=logicalY(my);if(x>=12&&x<142&&y>=35&&y<167){offerPage=Math.max(0,offerPage-(int)Math.signum(v));return true;}return super.mouseScrolled(mx,my,h,v);}
}

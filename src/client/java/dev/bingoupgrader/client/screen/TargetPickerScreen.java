package dev.bingoupgrader.client.screen;
import dev.bingoupgrader.client.ui.*;
import dev.bingoupgrader.client.network.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Sorting/filtering builds a cached list only after controls or search change, never per render. */
public final class TargetPickerScreen extends CanvasScreen {
    private record Entry(String id,ItemStack stack,String search,double value,String category) { }
    private final UpgraderScreen parent;private final List<Entry> all=new ArrayList<>();private List<Entry> rows=List.of();
    private String query="";private long valueRevision=-1;private EditBox search;private int category,tier,scroll;private boolean available,descending;
    private final String[] categories={"all","blocks","equipment","consumables","materials"};
    public TargetPickerScreen(UpgraderScreen parent){super(Ui.tr("target_title"),440,365);this.parent=parent;}
    protected void init(){
        search=addRenderableWidget(new EditBox(font,19,38,230,16,Ui.tr("search")));search.setBordered(false);search.setTextColor(Ui.TEXT);search.setMaxLength(128);search.setHint(Ui.tr("search"));search.setValue(query);search.setResponder(v->{query=v;filter();});
        button(263,34,82,23,Ui.tr("category."+categories[category]),()->{category=(category+1)%categories.length;rebuildWidgets();});
        button(350,34,78,23,Ui.tr("tier."+tier),()->{tier=(tier+1)%5;rebuildWidgets();});
        button(12,65,205,22,Ui.tr(available?"available_on":"available_off"),()->{available=!available;rebuildWidgets();});
        button(223,65,205,22,Ui.tr(descending?"sort_desc":"sort_asc"),()->{descending=!descending;rebuildWidgets();});
        button(12,331,200,23,Component.translatable("gui.back"),()->minecraft.gui.setScreen(parent));
        button(222,331,206,23,Ui.tr("clear_filters"),()->{category=tier=0;available=descending=false;query="";rebuildWidgets();});
        if(all.isEmpty()&&ClientUpgrader.catalog()!=null)for(String id:ClientUpgrader.catalog().items()){
            var key=Identifier.tryParse(id);if(key==null||!BuiltInRegistries.ITEM.containsKey(key))continue;var stack=new ItemStack(BuiltInRegistries.ITEM.getValue(key));var quote=ClientValues.quote(stack);if(!quote.allowed())continue;
            String cat=stack.getItem() instanceof BlockItem?"blocks":stack.isDamageableItem()?"equipment":stack.has(DataComponents.FOOD)||stack.has(DataComponents.CONSUMABLE)?"consumables":"materials";
            all.add(new Entry(id,stack,(id+" "+stack.getHoverName().getString()).toLowerCase(Locale.ROOT),quote.unitValue(),cat));
        }
        filter();setInitialFocus(search);valueRevision=ClientValues.table()==null?-1:ClientValues.table().revision();
    }
    private void button(int x,int y,int w,int h,Component label,Runnable action){addRenderableWidget(new ThemeButton(x,y,w,h,label,action));}
    private void filter(){String text=search==null?"":search.getValue().strip().toLowerCase(Locale.ROOT);var cat=ClientUpgrader.catalog();
        var tiers=ClientBingo.view()==null?new dev.bingoupgrader.common.config.BingoConfig().tiers:ClientBingo.view().settings().tiers;
        boolean exact=text.contains(":")&&all.stream().anyMatch(e->e.id.equals(text));
        rows=all.stream().filter(e->(exact?e.id.equals(text):e.search.contains(text))&&(category==0||e.category.equals(categories[category])))
            .filter(e->tier==0||e.value>=tiers.get(tier-1).min()&&e.value<tiers.get(tier-1).max())
            .filter(e->{if(!available||cat==null)return true;double out=e.value*Math.min(parent.targetCount(),e.stack.getMaxStackSize());double raw=parent.inputValue()/out*(1-cat.houseEdge());return out>parent.inputValue()&&out<=cat.maxTargetValue()&&raw>=cat.minChance();})
            .sorted(descending?Comparator.comparingDouble(Entry::value).reversed().thenComparing(Entry::id):Comparator.comparingDouble(Entry::value).thenComparing(Entry::id)).toList();scroll=0;
    }
    public void tick(){super.tick();if(ClientValues.table()!=null&&ClientValues.table().revision()!=valueRevision){all.clear();rebuildWidgets();}}
    protected void drawCanvas(GuiGraphicsExtractor g,int mx,int my,float partial){Ui.center(g,title,220,12,Ui.GOLD);Ui.nine(g,Ui.SLOT,12,33,242,25);Ui.panel(g,12,93,416,232,false);
        int end=Math.min(rows.size(),scroll+8);for(int i=scroll;i<end;i++){var row=rows.get(i);int y=98+(i-scroll)*28;boolean over=mx>=17&&mx<421&&my>=y&&my<y+27;Ui.nine(g,over?Ui.HOVER:Ui.BUTTON,17,y,400,26);Ui.slot(g,20,y+1,24,row.stack,false);Ui.text(g,Component.literal(font.plainSubstrByWidth(row.stack.getHoverName().getString(),265)),50,y+8,Ui.TEXT);Ui.text(g,Component.literal(Ui.number(row.value)),350,y+8,Ui.GOLD);if(over)tooltip(row.stack.getHoverName(),Component.literal(row.id),Ui.tr("value",Ui.number(row.value)));}
        if(rows.isEmpty())Ui.center(g,Ui.tr("no_targets"),220,180,Ui.MUTED);
        if(rows.size()>8){int bar=Math.max(8,220*8/rows.size()),y=99+(int)((220-bar)*scroll/(double)(rows.size()-8));g.fill(421,y,423,y+bar,Ui.GOLD);}
    }
    public boolean mouseClicked(MouseButtonEvent e,boolean twice){if(super.mouseClicked(e,twice))return true;int mx=logicalX(e.x()),my=logicalY(e.y());if(mx>=17&&mx<417&&my>=98&&my<322){int index=scroll+(my-98)/28;if(index<rows.size()){parent.selectTarget(rows.get(index).stack);minecraft.gui.setScreen(parent);return true;}}return false;}
    public boolean mouseScrolled(double mx,double my,double h,double v){scroll=Math.clamp(scroll-(int)Math.signum(v)*3,0,Math.max(0,rows.size()-8));return true;}
    public boolean keyPressed(KeyEvent e){if(e.isUp()){scroll=Math.max(0,scroll-1);return true;}if(e.isDown()){scroll=Math.min(Math.max(0,rows.size()-8),scroll+1);return true;}return super.keyPressed(e);}
    public void onClose(){minecraft.gui.setScreen(parent);}
}

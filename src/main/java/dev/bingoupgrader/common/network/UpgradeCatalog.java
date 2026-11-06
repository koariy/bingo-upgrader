package dev.bingoupgrader.common.network;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** Server-filtered default targets and preview constraints; client uses them only for filtering. */
public record UpgradeCatalog(List<String> items,double houseEdge,double minChance,double maxChance,double maxTargetValue,int previewSeconds) implements CustomPacketPayload {
    public UpgradeCatalog(List<String> items,double edge,double min,double max,double target){this(items,edge,min,max,target,15);}
    public static final Type<UpgradeCatalog> TYPE=new Type<>(Identifier.fromNamespaceAndPath("bingo_upgrader","upgrade_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf,UpgradeCatalog> CODEC=StreamCodec.of((b,v)->{
        if(v.items.size()>100000)throw new IllegalArgumentException("Catalog limit");b.writeVarInt(v.items.size());
        for(String id:v.items)b.writeUtf(id,256);b.writeDouble(v.houseEdge);b.writeDouble(v.minChance);b.writeDouble(v.maxChance);b.writeDouble(v.maxTargetValue);b.writeVarInt(v.previewSeconds);
    },b->{int n=b.readVarInt();if(n<0||n>100000)throw new IllegalArgumentException("Catalog size");var ids=new ArrayList<String>();for(int i=0;i<n;i++)ids.add(b.readUtf(256));return new UpgradeCatalog(List.copyOf(ids),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readVarInt());});
    public UpgradeCatalog { items=List.copyOf(items); if(!Double.isFinite(houseEdge)||houseEdge<=0||houseEdge>=1||!Double.isFinite(minChance)||!Double.isFinite(maxChance)||minChance<=0||maxChance>1||maxChance<minChance||!Double.isFinite(maxTargetValue)||maxTargetValue<=0||previewSeconds<1||previewSeconds>60)throw new IllegalArgumentException("Catalog settings"); }
    public Type<UpgradeCatalog> type(){return TYPE;}
}

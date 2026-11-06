package dev.bingoupgrader.server.bingo;
import dev.bingoupgrader.common.bingo.BingoState;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;

/** Versioned model converted to native NBT in world SavedData; no filesystem-global match state. */
public final class BingoSavedData extends SavedData {
    public BingoState state=new BingoState();
    // NbtOps converts booleans to bytes; convert only typed boolean fields when reading back.
    private static final com.google.gson.Gson SAVE_JSON=new com.google.gson.GsonBuilder()
        .registerTypeAdapter(boolean.class,(com.google.gson.JsonDeserializer<Boolean>)(element,type,context)->{
            if(element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                int value=element.getAsInt();if(value!=0&&value!=1)throw new IllegalArgumentException("Invalid saved boolean");return value==1;
            }
            return element.getAsBoolean();
        }).create();
    private static final Codec<BingoSavedData> CODEC=Codec.PASSTHROUGH.xmap(dynamic->{
        BingoState s=SAVE_JSON.fromJson(dynamic.convert(com.mojang.serialization.JsonOps.INSTANCE).getValue(),BingoState.class);
        if(s==null)throw new IllegalArgumentException("Empty bingo state");s.validate();
        var data=new BingoSavedData();data.state=s;return data;
    },data->new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE,dev.bingoupgrader.common.bingo.BingoJson.JSON.toJsonTree(data.state)));
    public static final SavedDataType<BingoSavedData> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("bingo_upgrader","bingo_match"),BingoSavedData::new,CODEC,null);
}

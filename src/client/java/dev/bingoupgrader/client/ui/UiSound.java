package dev.bingoupgrader.client.ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import java.util.Map;
/** Resource-defined aliases can be replaced by custom OGG samples without changing Java code. */
public final class UiSound {
    private static final Map<String,SoundEvent> EVENTS=Map.of("click",event("click"),"tick",event("tick"),"success",event("success"),"failure",event("failure"));
    private UiSound() { }
    private static SoundEvent event(String s){return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("bingo_upgrader",s));}
    public static void play(String name,float pitch){Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(EVENTS.get(name),pitch));}
}

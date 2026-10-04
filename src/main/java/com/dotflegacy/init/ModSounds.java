package com.dotflegacy.init;

import com.dotflegacy.Tags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Sound events, keyed exactly like the original Dawn of the Flood sounds.json
 * (entity.pod_infector.idle / entity.pod_infector.pop / entity.pod.bite).
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModSounds {

    public static final SoundEvent POD_INFECTOR_IDLE = create("entity.pod_infector.idle");
    public static final SoundEvent POD_INFECTOR_POP = create("entity.pod_infector.pop");
    public static final SoundEvent POD_INFECTOR_BITE = create("entity.pod.bite");

    private ModSounds() {
    }

    private static SoundEvent create(String name) {
        ResourceLocation location = new ResourceLocation(Tags.MOD_ID, name);
        return new SoundEvent(location).setRegistryName(location);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        IForgeRegistry<SoundEvent> registry = event.getRegistry();
        registry.register(POD_INFECTOR_IDLE);
        registry.register(POD_INFECTOR_POP);
        registry.register(POD_INFECTOR_BITE);
    }
}

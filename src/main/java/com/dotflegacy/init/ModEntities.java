package com.dotflegacy.init;

import com.dotflegacy.DotfLegacy;
import com.dotflegacy.Tags;
import com.dotflegacy.entity.EntityPodInfector;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModEntities {

    public static final ResourceLocation POD_INFECTOR_ID = new ResourceLocation(Tags.MOD_ID, "pod_infector");

    private ModEntities() {
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityPodInfector.class)
                .id(POD_INFECTOR_ID, 0)
                .name(Tags.MOD_ID + ".pod_infector")
                .tracker(64, 3, true)
                // Primary: dark flesh red, secondary: pale green (pod spores)
                .egg(0x5A1F1B, 0x9DBF5A)
                .spawn(EnumCreatureType.MONSTER, 25, 2, 6, overworldSpawnBiomes())
                .build());
        DotfLegacy.LOGGER.info("Registered entity {}", POD_INFECTOR_ID);
    }

    /**
     * Natural spawn biomes: overworld surface biomes that are not oceans, rivers,
     * mushroom islands or snowy/icy regions. Keeps the pods out of the Nether/End.
     */
    private static Biome[] overworldSpawnBiomes() {
        List<Biome> biomes = new ArrayList<>();
        // 1.12.2 has no OVERWORLD type: iterate the registry and exclude Nether/End biomes.
        for (Biome biome : ForgeRegistries.BIOMES.getValuesCollection()) {
            if (!BiomeDictionary.hasAnyType(biome)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.NETHER)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.END)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.MUSHROOM)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.VOID)) {
                continue;
            }
            biomes.add(biome);
        }
        return biomes.toArray(new Biome[0]);
    }
}

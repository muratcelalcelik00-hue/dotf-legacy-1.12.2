package com.dotflegacy.init;

import com.dotflegacy.Tags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTableList;

public final class ModLootTables {

    public static final ResourceLocation POD_INFECTOR = new ResourceLocation(Tags.MOD_ID, "entities/pod_infector");

    private ModLootTables() {
    }

    public static void register() {
        LootTableList.register(POD_INFECTOR);
    }
}

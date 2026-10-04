package com.dotflegacy.init;

import com.dotflegacy.DotfLegacy;
import com.dotflegacy.Tags;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModItems {

    /** Drop of the Pod Infector, mirrors dotf:pod_fragment. */
    public static final Item POD_FRAGMENT = new Item()
            .setRegistryName(Tags.MOD_ID, "pod_fragment")
            .setTranslationKey(Tags.MOD_ID + ".pod_fragment")
            .setCreativeTab(DotfLegacy.CREATIVE_TAB);

    private ModItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(POD_FRAGMENT);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(POD_FRAGMENT, 0,
                new ModelResourceLocation(POD_FRAGMENT.getRegistryName(), "inventory"));
    }
}

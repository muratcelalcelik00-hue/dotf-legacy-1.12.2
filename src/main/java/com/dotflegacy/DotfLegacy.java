package com.dotflegacy;

import com.dotflegacy.proxy.CommonProxy;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib3.GeckoLib;

/**
 * Dawn of the Flood Legacy - a backport of the Pod Infector from
 * Dawn of the Flood (ASEStefan / TeamAbyssal, MIT License) to Minecraft 1.12.2.
 */
@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        dependencies = "required-after:forge@[14.23.5.2847,);required-after:geckolib3@[3.0.26,)",
        acceptedMinecraftVersions = "[1.12.2]"
)
public class DotfLegacy {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.Instance(Tags.MOD_ID)
    public static DotfLegacy instance;

    @SidedProxy(clientSide = "com.dotflegacy.proxy.ClientProxy", serverSide = "com.dotflegacy.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(Tags.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            Item fragment = com.dotflegacy.init.ModItems.POD_FRAGMENT;
            return fragment == null ? ItemStack.EMPTY : new ItemStack(fragment);
        }
    };

    public DotfLegacy() {
        // GeckoLib must be initialised in the mod constructor so that the resource
        // reload listener for geo models / animations is registered on the client.
        GeckoLib.initialize();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Loading {} {}", Tags.MOD_NAME, Tags.VERSION);
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}

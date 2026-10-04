package com.dotflegacy.proxy;

import com.dotflegacy.init.ModLootTables;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ModLootTables.register();
    }

    public void init(FMLInitializationEvent event) {
    }
}

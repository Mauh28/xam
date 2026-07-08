package org.xam;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.xam.config.ConfigManager;
import org.xam.network.XamNetwork;

@Mod(XamConstants.MODID)
public class Xam {
    public Xam() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        XamConstants.LOGGER.info("Initializing xd Absolute Mastery (XAM)");
        ConfigManager.loadConfig();
        XamNetwork.register();
    }
}

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

/*
 * XAM (xd Absolute Mastery)
 * Copyright (C) 2024 xd Team, Mauh28
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

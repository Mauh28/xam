package org.xam.config;

import net.minecraftforge.eventbus.api.Event;

public class ConfigReloadedEvent extends Event {
    private final long newVersion;

    public ConfigReloadedEvent(long newVersion) {
        this.newVersion = newVersion;
    }

    public long getNewVersion() {
        return newVersion;
    }
}

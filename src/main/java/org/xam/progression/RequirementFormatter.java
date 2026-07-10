package org.xam.progression;

import net.minecraft.network.chat.Component;
import org.xam.config.Requirement;

public class RequirementFormatter {
    public static String formatRequirementDescription(Requirement req) {
        String name = req.getId();
        if (name.contains(":")) {
            String[] split = name.split(":");
            name = split[split.length - 1].replace("_", " ");
        }
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        switch (req.getType()) {
            case "craft": return Component.translatable("xam.req_type.craft", name).getString();
            case "collect": return Component.translatable("xam.req_type.collect", name).getString();
            case "combat":
            case "kill": return Component.translatable("xam.req_type.combat", name).getString();
            case "advancement": return Component.translatable("xam.req_type.advancement", name).getString();
            default: return Component.translatable("xam.req_type.default", req.getType(), name).getString();
        }
    }
}

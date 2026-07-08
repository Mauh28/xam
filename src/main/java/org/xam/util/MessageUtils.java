package org.xam.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessageUtils {
    // Thread-safe maps for cooldowns and last held items
    public static final Map<String, Long> COOLDOWNS = new ConcurrentHashMap<>();
    public static final Map<UUID, String> LAST_MAINHAND = new ConcurrentHashMap<>();
    public static final Map<UUID, String> LAST_OFFHAND = new ConcurrentHashMap<>();

    public static void sendWarning(Player player, Component message) {
        long now = System.currentTimeMillis();
        String key = player.getUUID().toString() + "_" + message.getString();
        Long last = COOLDOWNS.get(key);
        if (last == null || (now - last) >= 5000) {
            COOLDOWNS.put(key, now);
            player.sendSystemMessage(message.copy().withStyle(net.minecraft.ChatFormatting.RED));
        }
    }

    public static void sendItemWarning(Player player, ItemStack stack) {
        if (ItemUtils.isWeapon(stack)) {
            sendWarning(player, Component.translatable("xam.msg.weapon_no_effect"));
        } else {
            sendWarning(player, Component.translatable("xam.msg.tool_no_effect"));
        }
    }
}

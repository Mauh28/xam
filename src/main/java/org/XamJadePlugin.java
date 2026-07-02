package org;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.config.IPluginConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

@WailaPlugin
public class XamJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new BlockProvider(), Block.class);
        registration.registerEntityComponent(new EntityProvider(), Entity.class);
    }

    private static class BlockProvider implements IBlockComponentProvider {
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(xdAbsoluteMastery.MODID, "block_provider");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Player player = accessor.getPlayer();
            if (player != null) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    Block block = accessor.getBlock();
                    ResourceLocation blockId = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block);
                    if (blockId != null) {
                        String namespace = blockId.getNamespace();
                        
                        // Check if block namespace is a registered path and if it's restricted for the player
                        for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                            if (path.mod_id != null && path.mod_id.equals(namespace)) {
                                if (xdAbsoluteMastery.mustSelectPath(player, data)) {
                                    tooltip.add(Component.literal("✖ Bloqueado - Elige maestría").withStyle(ChatFormatting.RED));
                                    return;
                                }
                                
                                ItemStack dummyStack = new ItemStack(net.minecraft.world.item.Item.byBlock(block));
                                if (!dummyStack.isEmpty() && !xdAbsoluteMastery.isItemValid(dummyStack, data)) {
                                    tooltip.add(Component.literal("✖ Requiere maestría: " + path.name).withStyle(ChatFormatting.RED));
                                }
                                break;
                            }
                        }
                    }
                });
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private static class EntityProvider implements IEntityComponentProvider {
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(xdAbsoluteMastery.MODID, "entity_provider");

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            Player player = accessor.getPlayer();
            if (player != null) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    Entity entity = accessor.getEntity();
                    ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                    if (entityId != null) {
                        String namespace = entityId.getNamespace();
                        
                        // Check if entity namespace is a registered path and if it's restricted
                        for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                            if (path.mod_id != null && path.mod_id.equals(namespace)) {
                                if (xdAbsoluteMastery.mustSelectPath(player, data)) {
                                    tooltip.add(Component.literal("✖ Bloqueado - Elige maestría").withStyle(ChatFormatting.RED));
                                    return;
                                }
                                
                                boolean hasPathActiveOrMastered = false;
                                if (data.getCurrentPath() != null && data.getCurrentPath().equals(path.id)) {
                                    hasPathActiveOrMastered = true;
                                } else if (data.getMasteredPaths().contains(path.id)) {
                                    hasPathActiveOrMastered = true;
                                } else if (data.getStartedPaths().contains(path.id)) {
                                    hasPathActiveOrMastered = true;
                                }
                                
                                if (!hasPathActiveOrMastered && !data.isDevMode()) {
                                    tooltip.add(Component.literal("✖ Requiere maestría: " + path.name).withStyle(ChatFormatting.RED));
                                }
                                break;
                            }
                        }
                    }
                });
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}

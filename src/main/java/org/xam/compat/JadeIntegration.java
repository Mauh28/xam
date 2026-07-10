package org.xam.compat;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xam.XamConstants;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.data.PlayerDataProvider;
import org.xam.progression.MasteryService;

@WailaPlugin(XamConstants.MODID)
public class JadeIntegration implements IWailaPlugin {
    private static final Logger LOGGER = LogManager.getLogger(JadeIntegration.class);

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        LOGGER.info("[XAM-JADE] registerClient called! Registering block and entity providers.");
        registration.registerBlockComponent(new BlockProvider(), Block.class);
        registration.registerEntityComponent(new EntityProvider(), Entity.class);
    }

    private static class BlockProvider implements IBlockComponentProvider {
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(XamConstants.MODID, "block_provider");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Player player = accessor.getPlayer();
            if (player != null) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    ItemStack stack = accessor.getPickedResult();
                    if (!stack.isEmpty() && !MasteryService.isItemValid(stack, data)) {
                        String reqPathName = null;
                        ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                        if (rl != null) {
                            String namespace = rl.getNamespace();
                            for (PathInfo path : ConfigManager.PATHS) {
                                if (path.mod_id != null && path.mod_id.equals(namespace)) {
                                    reqPathName = path.name;
                                    break;
                                }
                            }
                        }

                        if (MasteryService.mustSelectPath(player, data)) {
                            tooltip.add(Component.translatable("xam.msg.locked_choose_mastery").withStyle(ChatFormatting.RED));
                        } else if (reqPathName != null) {
                            tooltip.add(Component.translatable("xam.msg.locked_requires_mastery", reqPathName).withStyle(ChatFormatting.RED));
                        } else {
                            tooltip.add(Component.translatable("xam.msg.locked_incompatible").withStyle(ChatFormatting.RED));
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
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(XamConstants.MODID, "entity_provider");

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            Player player = accessor.getPlayer();
            if (player != null) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    ItemStack stack = accessor.getPickedResult();
                    if (!stack.isEmpty() && !MasteryService.isItemValid(stack, data)) {
                        String reqPathName = null;
                        ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                        if (rl != null) {
                            String namespace = rl.getNamespace();
                            for (PathInfo path : ConfigManager.PATHS) {
                                if (path.mod_id != null && path.mod_id.equals(namespace)) {
                                    reqPathName = path.name;
                                    break;
                                }
                            }
                        }

                        if (MasteryService.mustSelectPath(player, data)) {
                            tooltip.add(Component.translatable("xam.msg.locked_choose_mastery").withStyle(ChatFormatting.RED));
                        } else if (reqPathName != null) {
                            tooltip.add(Component.translatable("xam.msg.locked_requires_mastery", Component.translatable(reqPathName)).withStyle(ChatFormatting.RED));
                        } else {
                            tooltip.add(Component.translatable("xam.msg.locked_incompatible").withStyle(ChatFormatting.RED));
                        }
                        return; // Done
                    }

                    // Fallback to Entity namespace checking if stack is empty
                    Entity entity = accessor.getEntity();
                    ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                    if (entityId != null) {
                        String namespace = entityId.getNamespace();
                        
                        // Check if entity namespace is a registered path and if it's restricted
                        for (PathInfo path : ConfigManager.PATHS) {
                            if (path.mod_id != null && path.mod_id.equals(namespace)) {
                                if (MasteryService.mustSelectPath(player, data)) {
                                    tooltip.add(Component.translatable("xam.msg.locked_choose_mastery").withStyle(ChatFormatting.RED));
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
                                    tooltip.add(Component.translatable("xam.msg.jade_requires_mastery", Component.translatable(path.name)).withStyle(ChatFormatting.RED));
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

package org.xam.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.xam.XamConstants;
import org.xam.client.ClientPacketHandler;
import org.xam.client.gui.MasteryHubScreen;
import org.xam.client.gui.PathSelectionScreen;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.data.PlayerDataProvider;
import org.xam.progression.MasteryService;
import org.xam.util.MessageUtils;

@Mod.EventBusSubscriber(modid = XamConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                if (ClientPacketHandler.shouldOpenPathSelection && mc.screen == null) {
                    ClientPacketHandler.shouldOpenPathSelection = false;
                    mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                        mc.setScreen(new PathSelectionScreen(data));
                    });
                }

                // Check if they pressed the key
                if (ClientModEvents.MASTERY_KEY.consumeClick()) {
                    mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                        mc.setScreen(new MasteryHubScreen(data));
                    });
                }

                // Key suppression check for invalid items or no mastery selected
                mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    boolean suppress = MasteryService.mustSelectPath(mc.player, data);
                    if (!suppress) {
                        ItemStack mainHand = mc.player.getMainHandItem();
                        ItemStack offHand = mc.player.getOffhandItem();
                        suppress = (!mainHand.isEmpty() && !MasteryService.isItemValid(mainHand, data))
                                || (!offHand.isEmpty() && !MasteryService.isItemValid(offHand, data));
                    }
                    if (suppress) {
                        for (net.minecraft.client.KeyMapping keyMapping : mc.options.keyMappings) {
                            if (isInteractionKey(keyMapping, mc.options)) {
                                while (keyMapping.consumeClick()) {
                                    // Drain all clicks
                                }
                                keyMapping.setDown(false);
                            }
                        }
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onScreenInitPost(ScreenEvent.Init.Post event) {
        net.minecraft.client.gui.screens.Screen screen = event.getScreen();
        if (screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen || screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) {
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) screen;
            int leftPos = containerScreen.getGuiLeft();
            int topPos = containerScreen.getGuiTop();

            int btnX = leftPos - 22;
            int btnY = topPos + 8;

            // Collision detection with other buttons (ftbquests, etc.)
            // Shift down if another button is already at this position
            boolean collision = true;
            while (collision) {
                collision = false;
                for (net.minecraft.client.gui.components.events.GuiEventListener child : screen.children()) {
                    if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                        if (widget.getX() == btnX && widget.getY() == btnY) {
                            btnY += 22;
                            collision = true;
                            break;
                        }
                    }
                }
            }

            net.minecraft.client.gui.components.Button button = new net.minecraft.client.gui.components.Button(
                    btnX, btnY, 20, 20, Component.empty(),
                    b -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                                mc.setScreen(new MasteryHubScreen(data));
                            });
                        }
                    },
                    supplier -> supplier.get()
            ) {
                @Override
                public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                    guiGraphics.renderFakeItem(new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK), this.getX() + 2, this.getY() + 2);
                }
            };

            event.addListener(button);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player p = event.getEntity();
        if (p == null) {
            p = Minecraft.getInstance().player;
        }
        final Player player = p;
        if (player != null) {
            player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                ItemStack stack = event.getItemStack();
                if (!stack.isEmpty() && !MasteryService.isItemValid(stack, data)) {
                    String reqPathName = null;
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (rl != null) {
                        String namespace = rl.getNamespace();
                        for (PathInfo path : ConfigManager.PATHS) {
                            if (path.getModId() != null && path.getModId().equals(namespace)) {
                                reqPathName = path.getName();
                                break;
                            }
                        }
                    }

                    if (MasteryService.mustSelectPath(player, data)) {
                        event.getToolTip().add(Component.translatable("xam.msg.locked_choose_mastery").withStyle(net.minecraft.ChatFormatting.RED));
                    } else if (reqPathName != null) {
                        event.getToolTip().add(Component.translatable("xam.msg.locked_requires_mastery", Component.translatable(reqPathName)).withStyle(net.minecraft.ChatFormatting.RED));
                    } else {
                        event.getToolTip().add(Component.translatable("xam.msg.locked_incompatible").withStyle(net.minecraft.ChatFormatting.RED));
                    }
                }
            });
        }
    }

    private static java.util.Set<net.minecraft.client.KeyMapping> ALLOWED_KEYS = null;

    private static boolean isInteractionKey(net.minecraft.client.KeyMapping key, net.minecraft.client.Options options) {
        if (ALLOWED_KEYS == null) {
            ALLOWED_KEYS = java.util.Set.of(
                options.keyUp,
                options.keyDown,
                options.keyLeft,
                options.keyRight,
                options.keyJump,
                options.keyShift,
                options.keySprint,
                options.keyInventory,
                options.keyChat,
                options.keyCommand,
                options.keyPlayerList,
                options.keyScreenshot,
                options.keySmoothCamera,
                options.keyFullscreen,
                options.keySpectatorOutlines
            );
        }
        return !ALLOWED_KEYS.contains(key);
    }
}

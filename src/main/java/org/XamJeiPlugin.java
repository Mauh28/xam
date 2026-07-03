package org;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryDecorator;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class XamJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(xdAbsoluteMastery.MODID, "jei_plugin");
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static IJeiRuntime getRuntime() {
        return runtime;
    }

    private <T> void addLockDecorator(IAdvancedRegistration registration, RecipeType<T> type) {
        registration.addRecipeCategoryDecorator(type, new IRecipeCategoryDecorator<T>() {
            @Override
            public void draw(T recipe, mezz.jei.api.recipe.category.IRecipeCategory<T> recipeCategory, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
                var player = Minecraft.getInstance().player;
                if (player == null) return;

                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    for (IRecipeSlotView slot : recipeSlotsView.getSlotViews()) {
                        if (slot instanceof IRecipeSlotDrawable drawable) {
                            var displayed = slot.getDisplayedItemStack();
                            if (displayed.isPresent()) {
                                ItemStack stack = displayed.get();
                                if (!xdAbsoluteMastery.isItemValid(stack, data)) {
                                    Rect2i rect = drawable.getRect();
                                    int rx = rect.getX() + 9;
                                    int ry = rect.getY() + 9;
                                    // Draw lock overlay on the slot coordinates
                                    guiGraphics.fill(rx, ry, rx + 7, ry + 7, 0xD05F9E8E); // Oxidized copper
                                    guiGraphics.fill(rx + 1, ry + 1, rx + 6, ry + 6, 0xE89E5F3F); // copper center
                                    guiGraphics.fill(rx + 3, ry + 2, rx + 4, ry + 3, 0xFFDF9E3F); // brass arch
                                    guiGraphics.fill(rx + 3, ry + 4, rx + 4, ry + 5, 0xFF000000); // black keyhole
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        addLockDecorator(registration, mezz.jei.api.constants.RecipeTypes.CRAFTING);
        addLockDecorator(registration, mezz.jei.api.constants.RecipeTypes.STONECUTTING);
        addLockDecorator(registration, mezz.jei.api.constants.RecipeTypes.SMELTING);
        addLockDecorator(registration, mezz.jei.api.constants.RecipeTypes.SMOKING);
        addLockDecorator(registration, mezz.jei.api.constants.RecipeTypes.BLASTING);
        addLockDecorator(registration, mezz.jei.api.constants.RecipeTypes.SMITHING);
        addLockDecorator(registration, mezz.jei.api.constants.RecipeTypes.ANVIL);
    }
}

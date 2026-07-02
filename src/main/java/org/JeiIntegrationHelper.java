package org;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class JeiIntegrationHelper {
    public static void showRecipe(String itemId) {
        if (XamJeiPlugin.getRuntime() != null) {
            ResourceLocation rl = ResourceLocation.tryParse(itemId);
            if (rl != null) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    ItemStack stack = new ItemStack(item);
                    var runtime = XamJeiPlugin.getRuntime();
                    var focusFactory = runtime.getJeiHelpers().getFocusFactory();
                    var focus = focusFactory.createFocus(
                            mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,
                            mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                            stack
                    );
                    runtime.getRecipesGui().show(focus);
                }
            }
        }
    }
}

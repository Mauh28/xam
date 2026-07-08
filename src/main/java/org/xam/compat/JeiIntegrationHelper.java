package org.xam.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class JeiIntegrationHelper {
    public static void showRecipe(String itemId) {
        if (JeiIntegration.getRuntime() != null) {
            ResourceLocation rl = ResourceLocation.tryParse(itemId);
            if (rl != null) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    ItemStack stack = new ItemStack(item);
                    var runtime = JeiIntegration.getRuntime();
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

package dorkix.armored.elytra.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dorkix.armored.elytra.RenderHelper;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.item.ItemStack;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraRendererMixin {

  @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 0)
  private ItemStack replaceElytraWithChestplate(ItemStack stack) {
    return RenderHelper.modifyStackWithElytra(stack);
  }
}

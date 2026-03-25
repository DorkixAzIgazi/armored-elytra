package dorkix.armored.elytra.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import dorkix.armored.elytra.RenderHelper;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.item.ItemStack;

@Mixin(HumanoidArmorLayer.class)
public abstract class ArmoredElytraModelMixin {

	@ModifyVariable(method = "renderArmorPiece", at = @At("HEAD"), ordinal = 0)
	private ItemStack replaceElytraWithChestplate(ItemStack stack) {
		return RenderHelper.modifyStackWithArmor(stack);
	}
}
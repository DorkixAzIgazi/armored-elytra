package dorkix.armored.elytra.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dorkix.armored.elytra.ArmoredElytra;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

@Mixin(ArmorFeatureRenderer.class)
public class ArmoredElytraModelMixin {
	@SuppressWarnings("resource")
	@ModifyVariable(method = "renderArmor", at = @At("STORE"), ordinal = 0)
	private ItemStack replaceElytraWithChestplate(ItemStack itemStack) {
		var player = MinecraftClient.getInstance().player;
		if (!itemStack.isOf(Items.ELYTRA) || player == null)
			return itemStack;

		// get the saved chestplate ItemStack as nbt
		NbtCompound chestplateData = itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
				.copyNbt().getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());

		if (chestplateData.isEmpty())
			return itemStack;

		// Convert the Nbt data to an ItemStack
		var armorItem = ItemStack.fromNbt(player.getRegistryManager(), chestplateData);

		if (!armorItem.isPresent())
			return itemStack;

		// return the chestplate to render on the model
		return armorItem.get();
	}
}
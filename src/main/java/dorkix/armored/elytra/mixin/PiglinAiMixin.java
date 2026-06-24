package dorkix.armored.elytra.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dorkix.armored.elytra.ArmoredElytra;

@Mixin(PiglinAi.class)
public class PiglinAiMixin {

  @Inject(method = "isWearingSafeArmor(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
  private static void isWearingSafeArmor(LivingEntity livingEntity, CallbackInfoReturnable<Boolean> cbi) {
    ItemStack elytra = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
    if (ArmoredElytra.isArmoredElytra(elytra)) {
      CompoundTag customData = elytra
          .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
          .copyTag();

      Optional<CompoundTag> armorDataNbt = customData.getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());
      CompoundTag armorData = armorDataNbt.get();
      ItemStack armorItemStack = ItemStack.CODEC
          .parse(RegistryOps.create(NbtOps.INSTANCE, livingEntity.registryAccess()), armorData)
          .resultOrPartial().orElse(ItemStack.EMPTY);

      cbi.setReturnValue(armorItemStack.is(ItemTags.PIGLIN_SAFE_ARMOR));
    }
  }
}
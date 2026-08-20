package dorkix.armored.elytra.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dorkix.armored.elytra.ArmoredElytra;

/**
 * Some mods (e.g. Advanced Netherite) pacify endermen towards players wearing
 * a chestplate tagged as "pacify_endermen_armor". Because an armored elytra
 * hides the actual chestplate inside its custom data, those mods can't see it
 * in the chest equipment slot. This mixin re-checks the embedded chestplate
 * for that same tag, without requiring any dependency on those mods.
 */
@Mixin(EnderMan.class)
public class EnderManMixin {

  private static final TagKey<Item> PACIFY_ENDERMEN_ARMOR = TagKey.create(Registries.ITEM,
      Identifier.fromNamespaceAndPath("advancednetherite", "pacify_endermen_armor"));

  @Inject(method = "isBeingStaredBy", at = @At("RETURN"), cancellable = true)
  private void isBeingStaredBy(Player player, CallbackInfoReturnable<Boolean> cbi) {
    if (!cbi.getReturnValue()) {
      return;
    }

    ItemStack elytra = player.getItemBySlot(EquipmentSlot.CHEST);
    if (!ArmoredElytra.isArmoredElytra(elytra)) {
      return;
    }

    ItemStack armor = ArmoredElytra.getEmbeddedChestplate(elytra, player.registryAccess());
    if (armor.is(PACIFY_ENDERMEN_ARMOR)) {
      cbi.setReturnValue(false);
    }
  }
}

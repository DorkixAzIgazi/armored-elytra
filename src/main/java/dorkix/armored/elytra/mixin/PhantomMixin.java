package dorkix.armored.elytra.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dorkix.armored.elytra.ArmoredElytra;

/**
 * Some mods (e.g. Advanced Netherite) pacify phantoms towards players wearing
 * a chestplate tagged as "pacify_phantoms_armor". Because an armored elytra
 * hides the actual chestplate inside its custom data, those mods can't see it
 * in the chest equipment slot. This mixin re-checks the embedded chestplate
 * for that same tag, without requiring any dependency on those mods.
 */
@Mixin(Phantom.class)
public class PhantomMixin {

  private static final TagKey<Item> PACIFY_PHANTOMS_ARMOR = TagKey.create(Registries.ITEM,
      Identifier.fromNamespaceAndPath("advancednetherite", "pacify_phantoms_armor"));

  @Inject(method = "tick", at = @At("HEAD"))
  private void tick(CallbackInfo ci) {
    Phantom phantom = (Phantom) (Object) this;
    LivingEntity target = phantom.getTarget();

    if (!(target instanceof Player player)) {
      return;
    }

    // Don't pacify a phantom that was angered by being attacked
    if (phantom.getLastHurtByMob() == target) {
      return;
    }

    ItemStack elytra = player.getItemBySlot(EquipmentSlot.CHEST);
    if (!ArmoredElytra.isArmoredElytra(elytra)) {
      return;
    }

    ItemStack armor = ArmoredElytra.getEmbeddedChestplate(elytra, player.registryAccess());
    if (armor.is(PACIFY_PHANTOMS_ARMOR)) {
      phantom.setTarget(null);
    }
  }
}

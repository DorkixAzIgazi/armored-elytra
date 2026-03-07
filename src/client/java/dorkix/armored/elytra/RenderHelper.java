package dorkix.armored.elytra;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;

public class RenderHelper {

  public static ItemStack modifyStackWithArmor(ItemStack stack) {
    var player = Minecraft.getInstance().player;
    if (!stack.is(Items.ELYTRA) || player == null)
      return stack;

    // Vanilla Tweaks compatibility
    BundleContents bundleContents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
    if (!bundleContents.isEmpty()) {
      for (ItemStack item : bundleContents.items()) {
        if (item.is(ItemTags.CHEST_ARMOR)) {
          return item;
        }
      }
    }

    // get the saved chestplate ItemStack as nbt
    Optional<CompoundTag> chestplateDataNbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
        .copyTag().getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());

    if (chestplateDataNbt.isEmpty()) {
      return stack;
    }

    CompoundTag chestplateData = chestplateDataNbt.get();

    if (chestplateData.isEmpty())
      return stack;

    // Convert the Nbt data to an ItemStack
    return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, player.registryAccess()), chestplateData)
        .resultOrPartial().orElse(stack);
  }

  public static ItemStack modifyStackWithElytra(ItemStack stack) {
    var player = Minecraft.getInstance().player;
    if (!stack.is(Items.ELYTRA) || player == null)
      return stack;

    // Vanilla Tweaks compatibility
    BundleContents bundleContents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
    if (!bundleContents.isEmpty()) {
      for (ItemStack item : bundleContents.items()) {
        if (item.is(Items.ELYTRA)) {
          return item;
        }
      }
    }

    // get the saved elytra ItemStack as nbt
    Optional<CompoundTag> elytraDataNbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
        .copyTag().getCompound(ArmoredElytra.ELYTRA_DATA.toString());

    if (elytraDataNbt.isEmpty())
      return stack;

    CompoundTag elytraData = elytraDataNbt.get();

    if (elytraData.isEmpty())
      return stack;

    // Convert the Nbt data to an ItemStack
    return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, player.registryAccess()), elytraData)
        .resultOrPartial().orElse(stack);
  }
}

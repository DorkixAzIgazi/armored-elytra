package dorkix.armored.elytra;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

public class RenderHelper {

  public static ItemStack modifyStackWithArmor(ItemStack stack) {
    var player = MinecraftClient.getInstance().player;
    if (!stack.isOf(Items.ELYTRA) || player == null)
      return stack;

    // get the saved chestplate ItemStack as nbt
    NbtCompound chestplateData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
        .copyNbt().getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());

    if (chestplateData.isEmpty())
      return stack;

    // Convert the Nbt data to an ItemStack
    var armorItem = ItemStack.fromNbt(player.getRegistryManager(),
        chestplateData);

    if (!armorItem.isPresent())
      return stack;

    return armorItem.get();
  }

  public static ItemStack modifyStackWithElytra(ItemStack stack) {
    var player = MinecraftClient.getInstance().player;
    if (!stack.isOf(Items.ELYTRA) || player == null)
      return stack;

    // get the saved elytra ItemStack as nbt
    NbtCompound elytraData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
        .copyNbt().getCompound(ArmoredElytra.ELYTRA_DATA.toString());

    if (elytraData.isEmpty())
      return stack;

    // Convert the Nbt data to an ItemStack
    var elytraItem = ItemStack.fromNbt(player.getRegistryManager(),
        elytraData);

    if (!elytraItem.isPresent())
      return stack;

    return elytraItem.get();
  }
}

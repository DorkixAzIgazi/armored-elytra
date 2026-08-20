package dorkix.armored.elytra;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class RenderHelper {

  public static ItemStack modifyStackWithArmor(ItemStack stack) {
    var player = Minecraft.getInstance().player;
    if (!ArmoredElytra.isArmoredElytra(stack) || player == null)
      return stack;

    return ArmoredElytra.getEmbeddedChestplate(stack, player.registryAccess());
  }

  public static ItemStack modifyStackWithElytra(ItemStack stack) {
    var player = Minecraft.getInstance().player;
    if (!ArmoredElytra.isArmoredElytra(stack) || player == null)
      return stack;

    return ArmoredElytra.getEmbeddedElytra(stack, player.registryAccess());
  }
}

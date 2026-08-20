package dorkix.armored.elytra.compat;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Function;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;

import dorkix.armored.elytra.ArmoredElytra;

/**
 * Soft integration with "Detail Armor Bar Reconstructed" (mod id
 * "detailabreconst").
 * Everything below is done via reflection against that mod's public API so that
 * Armored Elytra has no compile-time or runtime dependency on it - if the mod
 * is
 * absent this class simply does nothing.
 */
public final class DetailArmorBarCompat {
  private static final String DETAIL_ARMOR_BAR_MOD_ID = "detailabreconst";

  private DetailArmorBarCompat() {
  }

  public static void tryRegister() {
    if (!FabricLoader.getInstance().isModLoaded(DETAIL_ARMOR_BAR_MOD_ID)) {
      return;
    }
    try {
      register();
      ArmoredElytra.LOGGER.info("Detail Armor Bar Reconstructed detected, registered armored elytra icon compat");
    } catch (Throwable t) {
      ArmoredElytra.LOGGER
          .warn("Failed to hook into Detail Armor Bar Reconstructed, armored elytra icons may not render correctly", t);
    }
  }

  private static void register() throws Throwable {
    Class<?> apiClass = Class.forName("com.redlimerl.detailab.api.DetailArmorBarAPI");
    Class<?> builderClass = Class.forName("com.redlimerl.detailab.api.ArmorBarBuilder");
    Class<?> textureOffsetClass = Class.forName("com.redlimerl.detailab.api.render.TextureOffset");
    Class<?> renderManagerClass = Class.forName("com.redlimerl.detailab.api.render.ArmorBarRenderManager");
    Class<?> detailArmorBarClass = Class.forName("com.redlimerl.detailab.DetailArmorBar");

    Identifier texture = (Identifier) detailArmorBarClass.getField("GUI_ARMOR_BAR").get(null);
    Method isVanillaTexture = detailArmorBarClass.getMethod("isVanillaTexture");
    Constructor<?> offsetCtor = textureOffsetClass.getConstructor(int.class, int.class);
    Constructor<?> renderManagerCtor = renderManagerClass.getConstructor(Identifier.class, int.class, int.class,
        textureOffsetClass, textureOffsetClass, textureOffsetClass, textureOffsetClass, Color.class);

    Function<ItemStack, Object> renderFn = elytraStack -> {
      try {
        return buildRenderManager(elytraStack, texture, isVanillaTexture, offsetCtor, renderManagerCtor);
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    };

    Object builder = apiClass.getMethod("customArmorBarBuilder").invoke(null);
    builderClass.getMethod("armor", Item[].class).invoke(builder, (Object) new Item[] { Items.ELYTRA });
    builderClass.getMethod("render", Function.class).invoke(builder, renderFn);
    builderClass.getMethod("register").invoke(builder);
  }

  // Icon offsets copied from DetailArmorBar's own registration of vanilla
  // chestplates,
  // keyed here by the chestplate embedded in the armored elytra instead.
  private static Object buildRenderManager(ItemStack elytraStack, Identifier texture, Method isVanillaTexture,
      Constructor<?> offsetCtor, Constructor<?> renderManagerCtor) throws Throwable {
    int v = (int) isVanillaTexture.invoke(null);
    Object outline = offsetCtor.newInstance(9, 0);
    Object outlineHalf = offsetCtor.newInstance(27, 0);

    ItemStack chestplate = getEmbeddedChestplate(elytraStack);

    int fullX = 63, fullY = 9 + v, halfX = 54, halfY = 9 + v;
    Color color = Color.WHITE;

    if (chestplate.is(Items.NETHERITE_CHESTPLATE)) {
      fullX = 9;
      fullY = 9 + v;
      halfX = 0;
      halfY = 9 + v;
    } else if (chestplate.is(Items.DIAMOND_CHESTPLATE)) {
      fullX = 27;
      fullY = 9 + v;
      halfX = 18;
      halfY = 9 + v;
    } else if (chestplate.is(Items.IRON_CHESTPLATE)) {
      fullX = 63;
      fullY = 9 + v;
      halfX = 54;
      halfY = 9 + v;
    } else if (chestplate.is(Items.CHAINMAIL_CHESTPLATE)) {
      fullX = 81;
      fullY = 9 + v;
      halfX = 72;
      halfY = 9 + v;
    } else if (chestplate.is(Items.GOLDEN_CHESTPLATE)) {
      fullX = 99;
      fullY = 9 + v;
      halfX = 90;
      halfY = 9 + v;
    } else if (chestplate.is(Items.COPPER_CHESTPLATE)) {
      fullX = 9;
      fullY = 74 + v;
      halfX = 0;
      halfY = 74 + v;
    } else if (chestplate.is(Items.LEATHER_CHESTPLATE)) {
      fullX = 117;
      fullY = 9 + v;
      halfX = 108;
      halfY = 9 + v;
      color = new Color(DyedItemColor.getOrDefault(chestplate, -6265536));
    }

    Object full = offsetCtor.newInstance(fullX, fullY);
    Object half = offsetCtor.newInstance(halfX, halfY);

    return renderManagerCtor.newInstance(texture, 128, 128, full, half, outline, outlineHalf, color);
  }

  private static ItemStack getEmbeddedChestplate(ItemStack elytraStack) {
    if (!ArmoredElytra.isArmoredElytra(elytraStack)) {
      return ItemStack.EMPTY;
    }
    var player = Minecraft.getInstance().player;
    if (player == null) {
      return ItemStack.EMPTY;
    }
    return ArmoredElytra.getEmbeddedChestplate(elytraStack, player.registryAccess());
  }
}

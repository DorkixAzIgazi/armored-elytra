package dorkix.armored.elytra.mixin.client;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dorkix.armored.elytra.ArmoredElytra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

@Mixin(ItemModelResolver.class)
public abstract class ArmoredElytraIconMixin {

  // 1 Minecraft model unit = 16 pixels.
  // 2 pixels = 2 / 16 = 0.125
  private static final float ELYTRA_Y_OFFSET = -2.0F / 16.0F;
  private static final float CHESTPLATE_Y_OFFSET = 2.0F / 16.0F;
  // elytra will be shown behind the chestplate, so it needs to be offset in the
  // negative z direction
  private static final float Z_SEPARATION = 0.5F / 16.0F;
  private static final float ELYTRA_Z_OFFSET = -Z_SEPARATION;
  private static final float CHESTPLATE_Z_OFFSET = Z_SEPARATION;

  @Inject(method = "appendItemLayers", at = @At("HEAD"), cancellable = true)
  private void armoredElytra$layerIcon(
      ItemStackRenderState output,
      ItemStack item,
      ItemDisplayContext displayContext,
      @Nullable Level level,
      @Nullable ItemOwner owner,
      int seed,
      CallbackInfo ci) {

    if (!ArmoredElytra.isArmoredElytra(item)) {
      return;
    }

    ItemModelResolver self = (ItemModelResolver) (Object) this;
    RegistryAccess registryAccess = resolveRegistryAccess(level);

    if (registryAccess == null) {
      self.appendItemLayers(
          output,
          new ItemStack(Items.ELYTRA),
          displayContext,
          level,
          owner,
          seed);

      ci.cancel();
      return;
    }

    ItemStack embeddedElytra = ArmoredElytra.getEmbeddedElytra(item, registryAccess);
    ItemStack embeddedChestplate = ArmoredElytra.getEmbeddedChestplate(item, registryAccess);
    ItemStackRenderStateAccessor accessor = (ItemStackRenderStateAccessor) (Object) output;

    int elytraLayerIndex = accessor.armoredElytra$getActiveLayerCount();

    self.appendItemLayers(
        output,
        embeddedElytra.isEmpty()
            ? new ItemStack(Items.ELYTRA)
            : embeddedElytra,
        displayContext,
        level,
        owner,
        seed);

    LayerRenderState elytraLayer = accessor.armoredElytra$getLayers()[elytraLayerIndex];

    armoredElytra$offsetLayer(
        elytraLayer,
        ELYTRA_Y_OFFSET,
        ELYTRA_Z_OFFSET);

    if (!embeddedChestplate.isEmpty()) {

      int chestplateLayerIndex = accessor.armoredElytra$getActiveLayerCount();

      self.appendItemLayers(
          output,
          embeddedChestplate,
          displayContext,
          level,
          owner,
          seed);

      LayerRenderState chestplateLayer = accessor.armoredElytra$getLayers()[chestplateLayerIndex];

      armoredElytra$offsetLayer(
          chestplateLayer,
          CHESTPLATE_Y_OFFSET,
          CHESTPLATE_Z_OFFSET);
    }

    ci.cancel();
  }

  private static void armoredElytra$offsetLayer(
      LayerRenderState layer,
      float yOffset,
      float zOffset) {
    LayerRenderStateAccessor accessor = (LayerRenderStateAccessor) (Object) layer;
    Matrix4f transform = accessor.armoredElytra$getLocalTransform();
    transform.setTranslation(0.0F, yOffset, zOffset);
  }

  private static @Nullable RegistryAccess resolveRegistryAccess(
      @Nullable Level level) {

    if (level != null) {
      return level.registryAccess();
    }

    ClientLevel fallback = Minecraft.getInstance().level;

    return fallback != null
        ? fallback.registryAccess()
        : null;
  }
}
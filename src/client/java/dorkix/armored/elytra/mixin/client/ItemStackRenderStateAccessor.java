package dorkix.armored.elytra.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;

@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {

  @Accessor("layers")
  LayerRenderState[] armoredElytra$getLayers();

  @Accessor("activeLayerCount")
  int armoredElytra$getActiveLayerCount();
}
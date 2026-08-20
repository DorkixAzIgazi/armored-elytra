package dorkix.armored.elytra.mixin.client;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;

@Mixin(LayerRenderState.class)
public interface LayerRenderStateAccessor {

  @Accessor("itemTransform")
  ItemTransform armoredElytra$getItemTransform();

  @Accessor("localTransform")
  Matrix4f armoredElytra$getLocalTransform();
}
package dorkix.armored.elytra.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dorkix.armored.elytra.ArmoredElytra;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

@Mixin(Entity.class)
public abstract class EntityLavaHurtMixin {
    @Inject(method = "lavaHurt", at = @At("TAIL"))
    private void splitNetheriteInLava(CallbackInfo ci) {
        if ((Entity) (Object) this instanceof ItemEntity) {
            ItemEntity thisObject = (ItemEntity) (Object) this;

            ItemStack itemStack = thisObject.getItem();

            if (ArmoredElytra.isArmoredElytra(itemStack)) {
                var chestplate = ArmoredElytra.getEmbeddedChestplate(itemStack, thisObject.level().registryAccess());
                if (chestplate.is(Items.NETHERITE_CHESTPLATE)) {
                    thisObject.setItem(chestplate);
                }
                Level world = thisObject.level();
                world.playSound((Entity) null, thisObject.getX(), thisObject.getY(),
                        thisObject.getZ(), SoundEvents.GENERIC_BURN,
                        thisObject.getSoundSource(), 0.4F, 2.0F + thisObject.getRandom().nextFloat() * 0.4F);
            }
        }
    }
}

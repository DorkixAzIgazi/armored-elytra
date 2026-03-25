package dorkix.armored.elytra.mixin;

import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dorkix.armored.elytra.ArmoredElytra;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

@Mixin(Entity.class)
public abstract class EntityLavaHurtMixin {
    @Inject(method = "lavaHurt", at = @At("TAIL"))
    private void splitNetheriteInLava(CallbackInfo ci) {
        if ((Entity) (Object) this instanceof ItemEntity) {
            ItemEntity thisObject = (ItemEntity) (Object) this;

            ItemStack itemStack = thisObject.getItem();
            if (itemStack.is(Items.ELYTRA)) {
                BundleContents bundleContents = itemStack.getOrDefault(
                        DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
                if (!bundleContents.isEmpty()) {
                    // Handle Vanilla Tweaks data
                    bundleContents.items().forEach(item -> {
                        if (item.is(Items.NETHERITE_CHESTPLATE)) {
                            thisObject.setItem(item.create());
                        }
                    });
                } else {
                    // Handle native mod data
                    Optional<CompoundTag> armorDataNbt = itemStack
                            .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                            .copyTag().getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());
                    if (armorDataNbt.isEmpty())
                        return;

                    CompoundTag armorData = armorDataNbt.get();
                    if (armorData.isEmpty())
                        return;

                    ItemStack chestplate = ItemStack.CODEC
                            .parse(RegistryOps.create(NbtOps.INSTANCE, thisObject.level().registryAccess()),
                                    armorData)
                            .resultOrPartial().orElse(ItemStack.EMPTY);
                    ((ItemEntity) (Object) this).setItem(chestplate);
                }
                Level world = thisObject.level();
                world.playSound((Entity) null, thisObject.getX(), thisObject.getY(),
                        thisObject.getZ(), SoundEvents.GENERIC_BURN,
                        thisObject.getSoundSource(), 0.4F, 2.0F + thisObject.getRandom().nextFloat() * 0.4F);
            }
        }
    }
}

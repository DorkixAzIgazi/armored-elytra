package dorkix.armored.elytra.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dorkix.armored.elytra.ArmoredElytra;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

@Mixin(GrindstoneMenu.class)
public abstract class GrindStoneMixin extends AbstractContainerMenu {

    protected GrindStoneMixin(MenuType<?> type, int syncId) {
        super(type, syncId);
    }

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Shadow
    @Final
    private Container resultSlots;

    @Shadow
    @Final
    Container repairSlots;

    @Inject(method = "Lnet/minecraft/world/inventory/GrindstoneMenu;createResult()V", at = @At("RETURN"))
    private void replaceArmoredElytraResult(CallbackInfo ci) {
        var inputItem1 = this.repairSlots.getItem(0);
        var inputItem2 = this.repairSlots.getItem(1);
        if (inputItem1.is(Items.ELYTRA)) {
            showSplitResult(inputItem1, 0);

        } else if (inputItem2.is(Items.ELYTRA)) {
            showSplitResult(inputItem2, 1);
        }
    }

    private void showSplitResult(ItemStack inputItem, int slot) {

        // get the saved chestplate ItemStack as nbt
        Optional<CompoundTag> armorDataNbt = inputItem
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());
        Optional<CompoundTag> elytraDataNbt = inputItem
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompound(ArmoredElytra.ELYTRA_DATA.toString());

        // Vanilla Tweaks Compatibility
        BundleContents bundleContents = inputItem
                .getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (!bundleContents.isEmpty()) {
            bundleContents.items().forEach(item -> {
                if (item.is(ItemTags.CHEST_ARMOR)) {
                    this.access.execute((world, blockpos) -> {
                        this.resultSlots.setItem(slot, item.create());
                    });
                    broadcastChanges();
                    return;
                }
            });
        }

        if (elytraDataNbt.isEmpty() || armorDataNbt.isEmpty()) {
            return;
        }

        CompoundTag elytraData = elytraDataNbt.get();
        CompoundTag armorData = armorDataNbt.get();

        // if any of the source item data is missing skip this action
        if (armorData.isEmpty() || elytraData.isEmpty())
            return;

        // if found set the GrindStone result slot to contain the chestplate item
        this.access.execute((world, blockpos) -> {
            this.resultSlots.setItem(slot,
                    ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, world.registryAccess()), armorData)
                            .resultOrPartial().orElse(ItemStack.EMPTY));
        });

        broadcastChanges();
    }

    // to access the Grindstone screen and its data in the ResultSlotMixin
    @Mixin(GrindstoneMenu.class)
    public interface GrindstoneScreenHandlerAccessor {
        @Accessor
        Container getResultSlots();

        @Accessor
        Container getRepairSlots();

        @Accessor
        ContainerLevelAccess getAccess();
    }

    // target GrindstoneMenu's result slot
    @Mixin(targets = "net/minecraft/world/inventory/GrindstoneMenu$4")
    public static abstract class ResultSlotMixin extends Slot {
        public ResultSlotMixin(Container inventory, int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @Shadow(aliases = "this$0")
        @Final
        GrindstoneMenu grindstoneMenu;

        // try split the elytra for the given slot
        private boolean trySplitArmoredElytra(int slot) {
            var armoredElytra = ((GrindstoneScreenHandlerAccessor) grindstoneMenu).getRepairSlots().getItem(slot);
            // get the armored elytra source items nbt data
            CompoundTag customData = armoredElytra
                    .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag();

            Optional<CompoundTag> elytraDataNbt = customData.getCompound(ArmoredElytra.ELYTRA_DATA.toString());
            Optional<CompoundTag> armorDataNbt = customData.getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());

            if (elytraDataNbt.isEmpty() || armorDataNbt.isEmpty()) {
                return false;
            }

            CompoundTag elytraData = elytraDataNbt.get();
            CompoundTag armorData = armorDataNbt.get();

            // if not an armored elytra return to normal functioning
            if (elytraData.isEmpty() || armorData.isEmpty()) {
                return false;
            }
            var context = ((GrindstoneScreenHandlerAccessor) grindstoneMenu).getAccess();

            context.execute((world, blockPos) -> {
                // spawn a little xp
                if (world instanceof ServerLevel serverLevel) {
                    ExperienceOrb.award(serverLevel, Vec3.atCenterOf(blockPos.above()), 1);
                }

                // play the grindstone sound
                world.playSound(null, blockPos, SoundEvents.GRINDSTONE_USE,
                        SoundSource.BLOCKS);

                // replace the input armored elytra with the source elytra
                var registryNbtOps = RegistryOps.create(NbtOps.INSTANCE, world.registryAccess());
                var sourceElytra = ItemStack.CODEC.parse(registryNbtOps, elytraData).resultOrPartial()
                        .orElse(ItemStack.EMPTY);
                ;
                var sourceArmor = ItemStack.CODEC.parse(registryNbtOps, armorData).resultOrPartial()
                        .orElse(ItemStack.EMPTY);
                ;

                // check for compatible later added enchants
                var currentEnchants = armoredElytra.getEnchantments().keySet();
                for (var ce : currentEnchants) {
                    if (!ce.value().isSupportedItem(sourceElytra)) {
                        continue;
                    }

                    int currentLevel = armoredElytra.getEnchantments().getLevel(ce);
                    int elytraLevel = sourceElytra.getEnchantments().getLevel(ce);
                    int armorLevel = sourceArmor.getEnchantments().getLevel(ce);

                    if (currentLevel > elytraLevel && elytraLevel > 0 && elytraLevel >= armorLevel) {
                        sourceElytra.enchant(ce, currentLevel);
                    }

                }

                ((GrindstoneScreenHandlerAccessor) grindstoneMenu).getRepairSlots().setItem(slot,
                        sourceElytra);
            });
            return true;
        }

        // try split the elytra for the given slot (Vanilla Tweaks Format)
        private boolean trySplitVTArmoredElytra(int slot) {
            BundleContents bundleContents = ((GrindstoneScreenHandlerAccessor) grindstoneMenu)
                    .getRepairSlots().getItem(slot).getOrDefault(DataComponents.BUNDLE_CONTENTS,
                            BundleContents.EMPTY);
            if (bundleContents.isEmpty())
                return false;

            var context = ((GrindstoneScreenHandlerAccessor) grindstoneMenu).getAccess();
            bundleContents.items().forEach(item -> {
                if (item.is(Items.ELYTRA)) {
                    context.execute((world, blockPos) -> {
                        world.playSound(null, blockPos, SoundEvents.GRINDSTONE_USE,
                                SoundSource.BLOCKS);
                        ((GrindstoneScreenHandlerAccessor) grindstoneMenu).getRepairSlots().setItem(slot,
                                item.create());
                    });
                }
            });
            return true;
        }

        // When the user takes out result chestplate from the
        // GrindStoneMixin.showSplitResult() try getting the source elytra from the
        // input slots, replace the armored elytra with the source elytra and cancel the
        // takeout function so that the grindstone does not destroy the source elytra in
        // the input slot.
        // If the input slots do not contain an armored elytra just return to normal
        // grindstone function
        @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
        private void takeSeparatedChestplate(Player player, ItemStack stack,
                CallbackInfo ci) {

            if (!trySplitArmoredElytra(0) && !trySplitArmoredElytra(1)
                    && !trySplitVTArmoredElytra(0) && !trySplitVTArmoredElytra(1)) {
                return;
            }

            ci.cancel();
        }
    }
}

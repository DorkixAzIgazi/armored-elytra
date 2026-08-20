package dorkix.armored.elytra.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dorkix.armored.elytra.ArmoredElytra;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

        if (!ArmoredElytra.isArmoredElytra(inputItem)) {
            return;
        }

        // if the item is an Armored Elytra set the GrindStone result slot to contain
        // the chestplate item
        this.access.execute((world, blockpos) -> {
            this.resultSlots.setItem(slot,
                    ArmoredElytra.getEmbeddedChestplate(inputItem, world.registryAccess()));
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

            if (!ArmoredElytra.isArmoredElytra(armoredElytra)) {
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

                var sourceElytra = ArmoredElytra.getEmbeddedElytra(armoredElytra, world.registryAccess());
                var sourceArmor = ArmoredElytra.getEmbeddedChestplate(armoredElytra, world.registryAccess());

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

            if (!trySplitArmoredElytra(0) && !trySplitArmoredElytra(1)) {
                return;
            }

            ci.cancel();
        }
    }
}

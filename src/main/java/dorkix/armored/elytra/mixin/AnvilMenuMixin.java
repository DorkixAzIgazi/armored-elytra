package dorkix.armored.elytra.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dorkix.armored.elytra.ArmoredElytra;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    public AnvilMenuMixin(@Nullable MenuType<?> type, int syncId, Inventory playerInventory,
            ContainerLevelAccess context, ItemCombinerMenuSlotDefinition forgingSlotsManager) {
        super(type, syncId, playerInventory, context, forgingSlotsManager);
    }

    // hack access to cost member, if this is not set the item cant be removed
    // from the anvil
    @Shadow
    @Final
    private DataSlot cost;

    @Shadow
    private String itemName;

    // At all return statements of the AnvilMenu.createResult() function
    // check if the inputs are a chestplate and elytra
    // and set the result regardless of what the vanilla code set (this might
    // override other mod code, sorry :( )
    @Inject(method = "Lnet/minecraft/world/inventory/AnvilMenu;createResult()V", at = @At("RETURN"))
    private void showCombinedResult(CallbackInfo ci) {
        var inputItem1 = inputSlots.getItem(0);
        var inputItem2 = inputSlots.getItem(1);

        if (!tryCombine(inputItem1, inputItem2) && !tryCombine(inputItem2, inputItem1)) {
            return;
        }
    }

    private boolean tryCombine(ItemStack elytra, ItemStack armor) {
        if (elytra.is(Items.ELYTRA) && armor.is(ItemTags.CHEST_ARMOR)) {

            // Do not allow infinite combination of armored elytras
            if (ArmoredElytra.isArmoredElytra(elytra)) {
                return false;
            }

            resultSlots.setItem(0, ArmoredElytra.createArmoredElytra(
                    elytra, armor, this.access, itemName));
            cost.set(1);
            broadcastChanges();
            return true;
        }

        return false;
    }
}

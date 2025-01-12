package dorkix.armored.elytra.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dorkix.armored.elytra.ArmoredElytra;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.ForgingSlotsManager;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilMenuMixin extends ForgingScreenHandler {
    public AnvilMenuMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
            ScreenHandlerContext context, ForgingSlotsManager forgingSlotsManager) {
        super(type, syncId, playerInventory, context, forgingSlotsManager);
    }

    // hack access to levelCost member, if this is not set the item cant be removed
    // from the anvil
    @Shadow
    @Final
    private Property levelCost;

    @Shadow
    private String newItemName;

    // At all return statements of the AnvilScreenHandler.updateResult() function
    // check if the inputs are a chestplate and elytra
    // and set the result regardless of what the vanilla code set (this might
    // overrride other mod code, sorry :( )
    @Inject(method = "Lnet/minecraft/screen/AnvilScreenHandler;updateResult()V", at = @At("RETURN"))
    private void showCombinedResult(CallbackInfo ci) {
        var inputItem1 = input.getStack(0);
        var inputItem2 = input.getStack(1);

        if (!tryCombine(inputItem1, inputItem2) && !tryCombine(inputItem2, inputItem1)) {
            return;
        }
    }

    private boolean tryCombine(ItemStack elytra, ItemStack armor) {
        if (elytra.isOf(Items.ELYTRA) && armor.isIn(ItemTags.CHEST_ARMOR)) {

            // Do not allow infinte combination of armored elytras
            if (ArmoredElytra.isArmoredElytra(elytra)) {
                return false;
            }

            output.setStack(0, ArmoredElytra.createArmoredElytra(
                    elytra, armor, this.context, newItemName));
            levelCost.set(1);
            sendContentUpdates();
            return true;
        }

        return false;
    }
}

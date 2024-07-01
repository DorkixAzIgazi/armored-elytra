package dorkix.armored.elytra.mixin;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;

import dorkix.armored.elytra.ArmoredElytra;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Model;
import net.minecraft.data.client.Models;
import net.minecraft.data.client.TextureKey;
import net.minecraft.data.client.TextureMap;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

@Mixin(ItemModelGenerator.class)
public class ArmoredElytraItemMixin {

    @Shadow
    @Final
    public BiConsumer<Identifier, Supplier<JsonElement>> writer;

    @Inject(method = "registerArmor", at = @At(value = "TAIL", target = "Lnet/minecraft/data/client/Model;upload(Lnet/minecraft/util/Identifier;Lnet/minecraft/data/client/TextureMap;Ljava/util/function/BiConsumer;Lnet/minecraft/data/client/Model$JsonFactory;)Lnet/minecraft/util/Identifier;", ordinal = 0))
    private void uploadArmoredElytra(CallbackInfo ci, @Local(ordinal = 0) Identifier identifier,
            @Local(ordinal = 1) Identifier identifier2, @Local(ordinal = 2) Identifier identifier3,
            @Local ArmorItem armor) {
        // System.out.println(identifier.toString() + identifier2.toString() +
        // identifier3.toString());
        ArmoredElytra.LOGGER.info(identifier.toString() + identifier2.toString() +
                identifier3.toString());

        // Models.GENERATED_TWO_LAYERS
        // .upload(identifier, TextureMap.layered(identifier2, identifier3),
        // this.writer,
        // (id, textures) -> this.createArmorJson(id, textures, armor.getMaterial()));
    }

    @Mixin(Model.class)
    private static class ModelMixin {
        @Inject(method = "Lnet/minecraft/data/client/Model;createJson(Lnet/minecraft/util/Identifier;Ljava/util/Map;)Lcom/google/gson/JsonObject;", at = @At("TAIL"))
        private void hookJson(Identifier id, Map<TextureKey, Identifier> textures,
                CallbackInfoReturnable<JsonObject> ci,
                @Local JsonObject jsonObject) {

            //ArmoredElytra.LOGGER.info(ci.getReturnValue().getAsString());
        }

        @Inject(method = "Lnet/minecraft/data/client/Model;upload(Lnet/minecraft/util/Identifier;Lnet/minecraft/data/client/TextureMap;Ljava/util/function/BiConsumer;Lnet/minecraft/data/client/Model$JsonFactory;)Lnet/minecraft/util/Identifier;", at = @At("TAIL"))
        private void aaaa(Identifier id, TextureMap textures,
                BiConsumer<Identifier, Supplier<JsonElement>> modelCollector, Model.JsonFactory jsonFactory,
                CallbackInfoReturnable<Identifier> ci,
                @Local Map<TextureKey, Identifier> map) {

            ArmoredElytra.LOGGER.info(jsonFactory.create(id, map).toString());
        }
    }

}

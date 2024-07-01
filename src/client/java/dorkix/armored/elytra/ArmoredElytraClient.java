package dorkix.armored.elytra;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class ArmoredElytraClient implements ClientModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("ArmoredElytraClient");


	public static final Map<String, Float> TRIM_MATERIALS = Map.of(
			"minecraft:quartz",0.001f, 
			"minecraft:iron", 0.002f,
			"minecraft:netherite", 0.003f,
			"minecraft:redstone", 0.004f,
			"minecraft:copper", 0.005f,
			"minecraft:gold", 0.006f,
			"minecraft:emerald", 0.007f,
			"minecraft:diamond", 0.008f,
			"minecraft:lapis", 0.009f,
			"minecraft:amethyst", 0.010f);

			public static final Map<String, Float> MATERIALS = Map.of(
			"minecraft:leather_chestplate",0.1f, 
			"minecraft:chainmail_chestplate", 0.2f,
			"minecraft:iron_chestplate", 0.3f,
			"minecraft:golden_chestplate", 0.4f,
			"minecraft:diamond_chestplate", 0.5f,
			"minecraft:netherite_chestplate", 0.6f);

	public static String getTrimMaterialName(NbtCompound armorData) {
		return armorData.getCompound("components").getCompound("minecraft:trim").getString("material");
	}

	public static String getMaterialName(NbtCompound armorData) {
		return armorData.getString("id");
	}

	public static int getColor(NbtCompound armorData) {
		var colorRgb = armorData.getCompound("components").getCompound("minecraft:dyed_color").getInt("rgb");
		if(colorRgb == 0) {
			return 0xFFA06540; // default leather color
		}
		return 0xFF000000 + colorRgb; // ColorComponent stores RGB not ARGB
	}

	public static float getTrimValue(NbtCompound armorData) {
		return TRIM_MATERIALS.getOrDefault(getTrimMaterialName(armorData), 0.0f);
	}

	public static float getMaterialValue(NbtCompound armorData) {
		return MATERIALS.getOrDefault(getMaterialName(armorData), 0.0f);
	}

	@Override
	public void onInitializeClient() {
		ModelPredicateProviderRegistry.register(Items.ELYTRA, Identifier.of("armored"),
				(stack, world, ent, i) -> {
					NbtCompound customData = stack
							.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
							.copyNbt();

					NbtCompound elytraData = customData.getCompound(ArmoredElytra.ELYTRA_DATA.toString());
					NbtCompound armorData = customData.getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());
					
					if (elytraData.isEmpty() || armorData.isEmpty()) {
						return 0.0f;
					}
					
					return getMaterialValue(armorData) + getTrimValue(armorData);
				});

		// Color armored elytra with leather chestplate
		ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
			// leather_chestplate is always layer1, return -1 for other layers not to color them
			if(tintIndex != 1) {
				return -1;
			}

			NbtCompound customData = stack
							.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
							.copyNbt();

					NbtCompound armorData = customData.getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());
					
					if (armorData.isEmpty()) {
						return -1;
					}
					// check for actual armor type
					var material = getMaterialName(armorData);
					if(!material.equals("minecraft:leather_chestplate")) {
						return -1;
					}

					return getColor(armorData);
		}, Items.ELYTRA);
	}
}
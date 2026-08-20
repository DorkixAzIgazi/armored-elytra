package dorkix.armored.elytra;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.fabricmc.api.ModInitializer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

public class ArmoredElytra implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("ArmoredElytra");
	public static final String MOD_ID = "armored_elytra";

	public static final Identifier ELYTRA_DATA = id("elytra");
	public static final Identifier CHESTPLATE_DATA = id("chestplate");
	public static final Identifier TRIM_MATERIAL_DATA = id("trim_material");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		DebugCommand.register();
	}

	public static ItemStack createArmoredElytra(ItemStack elytra, ItemStack armor,
			ContainerLevelAccess context, String newItemName) {
		// return on invalid items
		if (!(armor.is(ItemTags.CHEST_ARMOR) && elytra.is(Items.ELYTRA)))
			return armor;

		var newElytra = elytra.copy();

		CompoundTag customData = elytra.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		// Convert ItemStack to Nbt and store it in the custom data component of the
		// elytra to restore the items later

		context.execute((world, blockPos) -> {
			customData.put(ArmoredElytra.ELYTRA_DATA.toString(),
					ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, world.registryAccess()), elytra)
							.getOrThrow());
			customData.put(ArmoredElytra.CHESTPLATE_DATA.toString(),
					ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, world.registryAccess()), armor).getOrThrow());
		});

		// ChestPlate Durability
		if (armor.getMaxDamage() > newElytra.getMaxDamage()) {
			newElytra.set(DataComponents.MAX_DAMAGE, armor.getMaxDamage());
			newElytra.set(DataComponents.DAMAGE, armor.getDamageValue());
		} else {
			newElytra.set(DataComponents.MAX_DAMAGE, elytra.getMaxDamage());
			newElytra.set(DataComponents.DAMAGE, elytra.getDamageValue());
		}

		// Copy Attribute modifiers - merge elytra + armor modifiers
		var armor_attr = armor.get(DataComponents.ATTRIBUTE_MODIFIERS);
		var elytra_attr = newElytra.get(DataComponents.ATTRIBUTE_MODIFIERS);
		var builder = ItemAttributeModifiers.builder();
		if (elytra_attr != null) {
			for (var ea : elytra_attr.modifiers()) {
				builder.add(ea.attribute(), ea.modifier(), ea.slot());
			}
		}
		if (armor_attr != null) {
			for (var aa : armor_attr.modifiers()) {
				builder.add(aa.attribute(), aa.modifier(), aa.slot());
			}
		}
		newElytra.applyComponents(
				DataComponentMap.builder().set(DataComponents.ATTRIBUTE_MODIFIERS,
						builder.build()).build());

		// Copy Armor Trims
		var trims = armor.get(DataComponents.TRIM);
		if (trims != null) {
			customData.putString(ArmoredElytra.TRIM_MATERIAL_DATA.toString(), trims.material().getRegisteredName());
		}

		var armorType = armor.getItem().toString();
		if (armorType.equals(Items.LEATHER_CHESTPLATE.toString())) {
			var color = armor.get(DataComponents.DYED_COLOR);
			if (color != null) {
				newElytra.applyComponents(
						DataComponentPatch.builder().set(DataComponents.DYED_COLOR,
								color).build());
			}
		}

		// Copy Enchantments

		for (var ench : armor.getEnchantments().keySet()) {
			int level = 1;
			var key = ench.unwrapKey();
			if (key.isPresent()) {
				level = armor.getEnchantments().getLevel(ench);
			}
			newElytra.enchant(ench, level);
		}

		// Set Armored elytra name or custom name from anvil
		Component name = Component.literal(newItemName != null ? newItemName : "");
		boolean hasNewName = newItemName != null && !newItemName.isEmpty();
		if (!hasNewName) {
			name = Component.translatableWithFallback("item." + ArmoredElytra.MOD_ID + ".item_name", "Armored Elytra");
		}
		newElytra.applyComponents(
				DataComponentMap.builder().set(DataComponents.CUSTOM_NAME,
						name.copy().setStyle(
								Style.EMPTY.withItalic(hasNewName).withColor(ChatFormatting.LIGHT_PURPLE)))
						.build());

		// Set description
		var armorHasCustomName = armor.get(DataComponents.CUSTOM_NAME) != null;

		List<Component> loreTexts = Lists.newArrayList();

		if (trims != null && loreTexts != null) {
			List<Component> trimTexts = Lists.newArrayList();
			// We cannot add the trim component to the armored elytra because of the
			// rendering, it needs to be faked with the lore component
			trims.addToTooltip(Item.TooltipContext.EMPTY, trimTexts::add, TooltipFlag.NORMAL,
					armor.getComponents());

			var upgradeText = trimTexts.get(0).copy()
					.setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY));
			var trimText = trimTexts.get(1).copy()
					.setStyle(Style.EMPTY.withItalic(false));
			var materialText = trimTexts.get(2).copy()
					.setStyle(Style.EMPTY.withItalic(false));

			loreTexts.addAll(List.of(
					CommonComponents.EMPTY,
					upgradeText,
					trimText,
					materialText));
		}

		loreTexts.addAll(List.of(
				CommonComponents.EMPTY,
				Component.translatableWithFallback(
						"item." + ArmoredElytra.MOD_ID + ".item_lore_text", "With chestplate:")
						.copy()
						.setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY)),
				CommonComponents.space().append(armor.getHoverName())
						.setStyle(Style.EMPTY.withItalic(armorHasCustomName)
								.withColor(ChatFormatting.LIGHT_PURPLE))));

		var loreComponent = new ItemLore(loreTexts);

		newElytra.applyComponents(
				DataComponentMap.builder()
						.set(DataComponents.LORE,
								loreComponent)
						.build());

		// set Custom data
		newElytra.applyComponents(
				DataComponentMap.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(customData)).build());

		newElytra.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(Collections.emptyList(),
				Collections.emptyList(), List.of(armorType), Collections.emptyList()));

		return newElytra;
	}

	public static boolean isArmoredElytra(ItemStack elytra) {
		if (!elytra.is(Items.ELYTRA)) {
			return false;
		}

		CompoundTag customData = elytra
				.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag();

		Optional<CompoundTag> elytraDataNbt = customData.getCompound(ArmoredElytra.ELYTRA_DATA.toString());
		Optional<CompoundTag> armorDataNbt = customData.getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());

		if (elytraDataNbt.isEmpty() || armorDataNbt.isEmpty()) {
			return false;
		}

		CompoundTag elytraData = elytraDataNbt.get();
		CompoundTag armorData = armorDataNbt.get();

		if (elytraData.isEmpty() || armorData.isEmpty()) {
			return false;
		}

		return true;
	}

	/**
	 * Extracts the chestplate embedded inside an armored elytra so mixins can
	 * check its item tags (e.g. mob-pacifying tags added by other mods) even
	 * though the chestplate itself is not present in any equipment slot.
	 */
	public static ItemStack getEmbeddedChestplate(ItemStack elytra, RegistryAccess registryAccess) {
		CompoundTag customData = elytra.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

		Optional<CompoundTag> armorDataNbt = customData.getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());
		if (armorDataNbt.isEmpty()) {
			return ItemStack.EMPTY;
		}

		return ItemStack.CODEC
				.parse(RegistryOps.create(NbtOps.INSTANCE, registryAccess), armorDataNbt.get())
				.resultOrPartial().orElse(ItemStack.EMPTY);
	}
}
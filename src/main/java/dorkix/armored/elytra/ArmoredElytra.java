package dorkix.armored.elytra;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ArmoredElytra implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("ArmoredElytra");
	public static final String MOD_ID = "armored_elytra";

	public static final Identifier ELYTRA_DATA = id("elytra");
	public static final Identifier CHESTPLATE_DATA = id("chestplate");

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
	}

	public static ItemStack createArmoredElytra(ItemStack elytra, ItemStack armor,
			ScreenHandlerContext context, String newItemName) {
		// return on invalid items
		if (!(armor.isIn(ItemTags.CHEST_ARMOR) && armor.getItem() instanceof ArmorItem && elytra.isOf(Items.ELYTRA)))
			return armor;

		var newElytra = elytra.copy();

		NbtCompound customData = elytra.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
		// Convert ItemStack to Nbt and store it in the custom data component of the
		// elytra to restore the items later

		context.run((world, blockPos) -> {
			customData.put(ArmoredElytra.ELYTRA_DATA.toString(),
					elytra.toNbt(world.getRegistryManager()));
			customData.put(ArmoredElytra.CHESTPLATE_DATA.toString(),
					armor.toNbt(world.getRegistryManager()));
		});

		// Copy Attribute modifiers
		var armor_attr = armor.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
		var builder = AttributeModifiersComponent.builder();
		for (var aa : armor_attr.modifiers()) {
			builder.add(aa.attribute(), aa.modifier(), aa.slot());
		}
		var attr = builder.build();
		newElytra.applyComponentsFrom(
				ComponentMap.builder().add(DataComponentTypes.ATTRIBUTE_MODIFIERS,
						attr).build());

		// Copy Armor Trims

		var trims = armor.getComponentChanges().get(DataComponentTypes.TRIM);
		if (trims != null && trims.isPresent()) {
			newElytra.applyChanges(ComponentChanges.builder().add(DataComponentTypes.TRIM,
					trims.get()).build());
		}

		// Copy Lava immunity
		var fire_res = armor.get(DataComponentTypes.DAMAGE_RESISTANT);
		if (fire_res != null && fire_res.types() == DamageTypeTags.IS_FIRE) {
			newElytra.applyChanges(
					ComponentChanges.builder().add(DataComponentTypes.DAMAGE_RESISTANT,
							fire_res).build());
		}

		var armorType = armor.getItem().toString();
		if (armorType.equals(Items.LEATHER_CHESTPLATE.toString())) {
			var color = armor.get(DataComponentTypes.DYED_COLOR);
			if (color != null) {
				newElytra.applyChanges(
						ComponentChanges.builder().add(DataComponentTypes.DYED_COLOR,
								color).build());
			}
		}

		// Copy Enchaments

		for (var ench : armor.getEnchantments().getEnchantments()) {
			int level = 1;
			var key = ench.getKey();
			if (key.isPresent()) {
				level = armor.getEnchantments().getLevel(ench);
			}
			newElytra.addEnchantment(ench, level);
		}

		// Set Armored elytra name or custom name from anvil
		Text name = Text.of(newItemName);
		boolean hasNewName = newItemName != null && !newItemName.isEmpty();
		if (!hasNewName) {
			name = Text.translatableWithFallback("item." + ArmoredElytra.MOD_ID + ".item_name", "Armored Elytra");
		}
		newElytra.applyComponentsFrom(
				ComponentMap.builder().add(DataComponentTypes.CUSTOM_NAME,
						name.copy().setStyle(
								Style.EMPTY.withItalic(hasNewName).withColor(Formatting.LIGHT_PURPLE)))
						.build());

		// Set description
		var armorHasCustomName = armor.get(DataComponentTypes.CUSTOM_NAME) != null;
		newElytra.applyComponentsFrom(
				ComponentMap.builder()
						.add(DataComponentTypes.LORE,
								new LoreComponent(List.of(
										Text.of(""),
										Text.translatableWithFallback(
												"item." + ArmoredElytra.MOD_ID + ".item_lore_text", "With chesplate:")
												.copy()
												.setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)),
										Text.of(" ").copy().append(armor.getName())
												.setStyle(Style.EMPTY.withItalic(armorHasCustomName)
														.withColor(Formatting.LIGHT_PURPLE)))))
						.build());

		// set Custom data
		newElytra.applyComponentsFrom(
				ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData)).build());

		return newElytra;
	}

	public static boolean isArmoredElytra(ItemStack elytra) {
		if (!elytra.isOf(Items.ELYTRA)) {
			return false;
		}

		NbtCompound customData = elytra
				.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
				.copyNbt();

		NbtCompound elytraData = customData.getCompound(ArmoredElytra.ELYTRA_DATA.toString());
		NbtCompound armorData = customData.getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());

		if (elytraData.isEmpty() || armorData.isEmpty()) {
			return false;
		}

		return true;
	}
}
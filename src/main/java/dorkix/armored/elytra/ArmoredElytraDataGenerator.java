package dorkix.armored.elytra;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ArmoredElytraDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		pack.addProvider(MyModelGenerator::new);
	}

	private static class MyModelGenerator extends FabricModelProvider {
		private MyModelGenerator(FabricDataOutput generator) {
			super(generator);
		}

		@Override
		public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
			// ...
		}

		@Override
		public void generateItemModels(ItemModelGenerator itemModelGenerator) {
			Map<Identifier, Supplier<JsonElement>> map2 = Maps.<Identifier, Supplier<JsonElement>>newHashMap();
			BiConsumer<Identifier, Supplier<JsonElement>> biConsumer = (id, jsonSupplier) -> {
				Supplier<JsonElement> supplier = (Supplier<JsonElement>) map2.put(id, jsonSupplier);
				if (supplier != null) {
					throw new IllegalStateException("Duplicate model definition for " + id);
				}
			};
			var a = new ItemModelGenerator(biConsumer);

			for (Item item : Registries.ITEM) {
				if (item instanceof ArmorItem armorItem && ((ArmorItem) item).getSlotType() == EquipmentSlot.CHEST) {
					a.registerArmor(armorItem);
				}
			}
		}
	}
}

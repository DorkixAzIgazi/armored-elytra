package dorkix.armored.elytra;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public class ArmoredElytraDataGenerator implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(ArmoredElytraResourceGenerator::new);
	}

	// -------------------------------------------------------------------------
	// Data definitions
	// -------------------------------------------------------------------------

	/** All chestplate armor types. */
	private static final List<ArmorEntry> ARMORS = List.of(
			new ArmorEntry("chainmail_chestplate", false),
			new ArmorEntry("copper_chestplate", false),
			new ArmorEntry("diamond_chestplate", false),
			new ArmorEntry("golden_chestplate", false),
			new ArmorEntry("iron_chestplate", false),
			new ArmorEntry("leather_chestplate", true),
			new ArmorEntry("netherite_chestplate", false));

	/**
	 * All trim materials. The {@code darkerFor} list contains the armor types that
	 * use the {@code <material>_darker} palette instead of the standard one (e.g.
	 * iron trim on iron armor uses the iron_darker palette).
	 */
	private static final List<TrimMaterial> TRIM_MATERIALS = List.of(
			new TrimMaterial("amethyst", List.of()),
			new TrimMaterial("copper", List.of("copper_chestplate")),
			new TrimMaterial("diamond", List.of("diamond_chestplate")),
			new TrimMaterial("emerald", List.of()),
			new TrimMaterial("gold", List.of("golden_chestplate")),
			new TrimMaterial("iron", List.of("iron_chestplate")),
			new TrimMaterial("lapis", List.of()),
			new TrimMaterial("netherite", List.of("netherite_chestplate")),
			new TrimMaterial("quartz", List.of()),
			new TrimMaterial("redstone", List.of()),
			new TrimMaterial("resin", List.of()));

	// -------------------------------------------------------------------------
	// Record helpers
	// -------------------------------------------------------------------------

	/**
	 * Describes a chestplate armor type.
	 *
	 * @param name       the Minecraft item name (e.g. {@code iron_chestplate})
	 * @param hasOverlay whether the armor has a dyeable overlay layer (leather)
	 */
	record ArmorEntry(String name, boolean hasOverlay) {
		public String overlayLayerName() {
			return name + "_overlay";
		}
	}

	/**
	 * Describes a trim material.
	 *
	 * @param name      the material name (e.g. {@code iron})
	 * @param darkerFor armor types for which the {@code <name>_darker} palette is
	 *                  used
	 */
	record TrimMaterial(String name, List<String> darkerFor) {
		/** Returns the palette file stem for the given armor (without ".png"). */
		public String paletteNameFor(String armorName) {
			return darkerFor.contains(armorName) ? name + "_darker" : name;
		}

		/** Returns the trim texture file stem for the given armor. */
		public String trimTextureNameFor(String armorName) {
			return "chestplate_trim_" + paletteNameFor(armorName);
		}

		/** Returns the model-name suffix for the given armor. */
		public String modelSuffixFor(String armorName) {
			return name + (darkerFor.contains(armorName) ? "_darker" : "") + "_trim";
		}
	}

	// -------------------------------------------------------------------------
	// Provider
	// -------------------------------------------------------------------------

	private static class ArmoredElytraResourceGenerator implements DataProvider {

		private static final Logger LOGGER = LoggerFactory.getLogger(ArmoredElytraResourceGenerator.class);
		private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

		private final FabricPackOutput output;

		public ArmoredElytraResourceGenerator(FabricPackOutput output) {
			this.output = output;
		}

		@Override
		public CompletableFuture<?> run(CachedOutput cache) {
			try {
				generateAll(cache);
			} catch (IOException e) {
				throw new RuntimeException("Failed to generate Armored Elytra resources", e);
			}
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public String getName() {
			return "Armored Elytra Resource Generator";
		}

		private void generateAll(CachedOutput cache) throws IOException {
			// The data-gen task runs with the working directory set to build/datagen.
			// Walk up to the project root (the first ancestor that contains
			// 'source_assets/').
			Path workDir = Path.of(System.getProperty("user.dir"));
			Path projectRoot = workDir;
			while (projectRoot != null && !Files.isDirectory(projectRoot.resolve("source_assets"))) {
				projectRoot = projectRoot.getParent();
			}
			if (projectRoot == null) {
				throw new IOException("Could not locate the 'source_assets/' folder relative to " + workDir);
			}
			LOGGER.info("ArmoredElytra DataGen: project root = {}", projectRoot);
			Path mcTextures = projectRoot.resolve("source_assets/minecraft/textures");
			Path mcItemTextures = mcTextures.resolve("item");
			Path mcTrimItems = mcTextures.resolve("trims/items");
			Path mcTrimPalettes = mcTextures.resolve("trims/color_palettes");

			// Output roots (resource-pack target).
			// getOutputFolder(RESOURCE_PACK) already returns the "assets" directory,
			// so we resolve namespace paths directly from it.
			Path resourcePackRoot = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK);
			LOGGER.info("ArmoredElytra DataGen: output root = {}", resourcePackRoot);
			Path outTextures = resourcePackRoot.resolve("armored_elytra/textures/item");
			Path outModels = resourcePackRoot.resolve("armored_elytra/models/item");
			Path outItems = resourcePackRoot.resolve("minecraft/items");

			// -----------------------------------------------------------------
			// 1. Generate shifted textures
			// -----------------------------------------------------------------

			// 1a. Elytra – shift DOWN by height/8 pixels so it aligns with the chestplate
			generateShiftedTexture(
					mcItemTextures.resolve("elytra.png"),
					outTextures.resolve("elytra.png"),
					/* down= */ true, cache);

			// Load and shift the trim template (shared base for all coloured trims)
			BufferedImage trimTemplate = loadImage(mcTrimItems.resolve("chestplate_trim.png"));
			BufferedImage shiftedTrimTemplate = shiftImage(trimTemplate, -(trimTemplate.getHeight() / 8));

			// Save the uncoloured (shifted) trim template
			saveImage(shiftedTrimTemplate, outTextures.resolve("chestplate_trim.png"), cache);

			// Load the palette key (trim_palette.png maps gray shades → palette indices)
			BufferedImage paletteKeyImg = loadImage(mcTrimPalettes.resolve("trim_palette.png"));
			int[] keyColors = extractPaletteColors(paletteKeyImg);

			for (ArmorEntry armor : ARMORS) {
				// 1b. Chestplate – shift UP by height/8 pixels
				generateShiftedTexture(
						mcItemTextures.resolve(armor.name() + ".png"),
						outTextures.resolve(armor.name() + ".png"),
						/* down= */ false, cache);

				// 1c. Leather overlay – shift UP by height/8 pixels
				if (armor.hasOverlay()) {
					generateShiftedTexture(
							mcItemTextures.resolve(armor.overlayLayerName() + ".png"),
							outTextures.resolve(armor.overlayLayerName() + ".png"),
							/* down= */ false, cache);
				}

				// 1d. Coloured trim textures for this armor
				for (TrimMaterial trim : TRIM_MATERIALS) {
					String paletteName = trim.paletteNameFor(armor.name());
					BufferedImage paletteImg = loadImage(mcTrimPalettes.resolve(paletteName + ".png"));
					int[] targetColors = extractPaletteColors(paletteImg);

					BufferedImage coloredTrim = applyPalette(shiftedTrimTemplate, keyColors, targetColors);
					String trimTextureName = trim.trimTextureNameFor(armor.name());
					saveImage(coloredTrim, outTextures.resolve(trimTextureName + ".png"), cache);
				}
			}

			// -----------------------------------------------------------------
			// 2. Generate item model JSON files
			// -----------------------------------------------------------------

			for (ArmorEntry armor : ARMORS) {
				// Plain model (no trim)
				saveJson(buildArmorModel(armor, null),
						outModels.resolve("elytra_" + armor.name() + ".json"), cache);

				// One model per trim material
				for (TrimMaterial trim : TRIM_MATERIALS) {
					String modelFile = "elytra_" + armor.name() + "_" + trim.modelSuffixFor(armor.name()) + ".json";
					saveJson(buildArmorModel(armor, trim), outModels.resolve(modelFile), cache);
				}
			}

			// -----------------------------------------------------------------
			// 3. Generate elytra.json item definition
			// -----------------------------------------------------------------

			saveJson(buildElytraItemDefinition(), outItems.resolve("elytra.json"), cache);

			LOGGER.info("ArmoredElytra DataGen: generation complete");
		}

		// -----------------------------------------------------------------
		// Texture helpers
		// -----------------------------------------------------------------

		/**
		 * Shifts an image vertically. A positive {@code delta} moves content DOWN
		 * (rows are filled from the top), a negative {@code delta} moves it UP.
		 */
		private static BufferedImage shiftImage(BufferedImage src, int delta) {
			int w = src.getWidth();
			int h = src.getHeight();
			BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

			for (int y = 0; y < h; y++) {
				int srcY = y - delta;
				if (srcY < 0 || srcY >= h)
					continue;
				for (int x = 0; x < w; x++) {
					dst.setRGB(x, y, src.getRGB(x, srcY));
				}
			}
			return dst;
		}

		/**
		 * Shifts an image vertically by {@code height / 8} pixels. Pass {@code true}
		 * for {@code down} to shift content downward, {@code false} to shift upward.
		 */
		private static void generateShiftedTexture(Path src, Path dst, boolean down, CachedOutput cache)
				throws IOException {
			BufferedImage img = loadImage(src);
			int delta = img.getHeight() / 8;
			saveImage(shiftImage(img, down ? delta : -delta), dst, cache);
		}

		/**
		 * Loads a PNG from the filesystem without applying any embedded colour-profile
		 * conversion. Minecraft treats all item-texture PNGs as raw sRGB, so we need
		 * the pixel byte values exactly as stored in the file, not gamma-encoded by
		 * Java's default sRGB conversion.
		 *
		 * <p>
		 * {@code BufferedImage.getRGB()} internally converts from the image's declared
		 * colour space to Java's default sRGB, which changes pixel values when the PNG
		 * has an embedded linear or custom ICC profile (e.g. {@code gAMA} γ=1.0).
		 * We avoid this by copying the raw integer samples from the {@link Raster}
		 * directly into a new {@code TYPE_INT_ARGB} image.
		 *
		 * <p>
		 * Exception: indexed (palette) PNGs are loaded through {@code getRGB} because
		 * their Raster only contains palette indices, not colour samples. The
		 * {@code getRGB} look-up correctly handles the transparency entry for that
		 * format, and indexed PNGs never carry ICC profiles. Detection uses
		 * {@code instanceof IndexColorModel} rather than the image type integer
		 * because 4-bit palette PNGs load as {@code TYPE_BYTE_BINARY} while 8-bit
		 * ones load as {@code TYPE_BYTE_INDEXED}; both share {@link IndexColorModel}.
		 */
		private static BufferedImage loadImage(Path path) throws IOException {
			try (InputStream is = Files.newInputStream(path)) {
				BufferedImage img = ImageIO.read(is);
				if (img == null)
					throw new IOException("Failed to read image: " + path);
				// TYPE_INT_ARGB pixels are already stored as sRGB integers — getRGB()
				// is a direct read with no conversion, so we can return as-is.
				if (img.getType() == BufferedImage.TYPE_INT_ARGB)
					return img;
				int w = img.getWidth(), h = img.getHeight();
				BufferedImage argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				// Indexed/palette images (1-, 2-, 4-, or 8-bit): getRGB() maps palette
				// indices — including the transparency entry — to ARGB correctly.
				// We detect by ColorModel type rather than BufferedImage type because
				// 4-bit palette PNGs load as TYPE_BYTE_BINARY (12) while 8-bit ones
				// load as TYPE_BYTE_INDEXED (13); both share IndexColorModel.
				if (img.getColorModel() instanceof IndexColorModel) {
					for (int y = 0; y < h; y++)
						for (int x = 0; x < w; x++)
							argb.setRGB(x, y, img.getRGB(x, y));
					return argb;
				}
				// All other types: copy raw Raster sample values (0-255 per channel)
				// into the ARGB image, treating them as sRGB without any gamma / ICC
				// transformation.
				Raster src = img.getRaster();
				WritableRaster dst = argb.getRaster();
				int bands = src.getNumBands();
				int[] sp = new int[bands];
				int[] dp = new int[4]; // R, G, B, A
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						src.getPixel(x, y, sp);
						dp[0] = sp[0]; // R
						dp[1] = (bands >= 3) ? sp[1] : sp[0]; // G (copy of R for greyscale)
						dp[2] = (bands >= 3) ? sp[2] : sp[0]; // B (copy of R for greyscale)
						dp[3] = (bands == 4) ? sp[3] // A from RGBA
								: (bands == 2) ? sp[1] // A from greyscale+A
										: 255; // fully opaque otherwise
						dst.setPixel(x, y, dp);
					}
				}
				return argb;
			}
		}

		/**
		 * Reads the ARGB value of each pixel in row 0 of a 1×N palette strip.
		 * Returns one entry per pixel, left-to-right.
		 */
		private static int[] extractPaletteColors(BufferedImage palette) {
			int n = palette.getWidth();
			int[] colors = new int[n];
			for (int i = 0; i < n; i++) {
				colors[i] = palette.getRGB(i, 0);
			}
			return colors;
		}

		/**
		 * Re-colours a shifted, grayscale trim template using the target palette.
		 *
		 * <p>
		 * The trim template pixels use the same set of grayscale values that
		 * appear in {@code trim_palette.png} (the "key"). For each non-transparent
		 * template pixel the gray value is matched against the key, and the
		 * corresponding entry in {@code targetColors} is used as the output colour.
		 *
		 * @param template     the shifted grayscale trim template
		 * @param keyColors    ARGB values from {@code trim_palette.png} (index 0 =
		 *                     brightest)
		 * @param targetColors ARGB values from the material-specific palette image
		 */
		private static BufferedImage applyPalette(BufferedImage template,
				int[] keyColors, int[] targetColors) {
			int w = template.getWidth();
			int h = template.getHeight();
			BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

			// Build gray-value → target-ARGB lookup table
			Map<Integer, Integer> grayToColor = new LinkedHashMap<>();
			for (int i = 0; i < keyColors.length && i < targetColors.length; i++) {
				int gray = (keyColors[i] >> 16) & 0xFF; // R channel (key is grayscale)
				grayToColor.put(gray, targetColors[i]);
			}

			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int argb = template.getRGB(x, y);
					int alpha = (argb >> 24) & 0xFF;
					if (alpha == 0)
						continue; // leave transparent pixels unchanged

					int gray = (argb >> 16) & 0xFF;
					Integer mapped = grayToColor.get(gray);
					if (mapped != null) {
						// Keep the template's alpha; apply the mapped RGB
						int mappedAlpha = (mapped >> 24) & 0xFF;
						int outAlpha = mappedAlpha > 0 ? alpha : 0;
						dst.setRGB(x, y, (mapped & 0x00FFFFFF) | (outAlpha << 24));
					}
				}
			}
			return dst;
		}

		@SuppressWarnings("deprecation")
		private static void saveImage(BufferedImage img, Path dst, CachedOutput cache)
				throws IOException {
			Files.createDirectories(dst.getParent());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, "PNG", baos);
			byte[] bytes = baos.toByteArray();
			cache.writeIfNeeded(dst, bytes, Hashing.sha1().hashBytes(bytes));
		}

		// -----------------------------------------------------------------
		// Model JSON helpers
		// -----------------------------------------------------------------

		private static JsonObject buildArmorModel(ArmorEntry armor, TrimMaterial trim) {
			JsonObject model = new JsonObject();
			model.addProperty("parent", "minecraft:item/generated");

			JsonObject textures = new JsonObject();
			textures.addProperty("layer0", "armored_elytra:item/elytra");
			textures.addProperty("layer1", "armored_elytra:item/" + armor.name());

			int nextLayer = 2;

			if (armor.hasOverlay()) {
				textures.addProperty("layer" + nextLayer, "armored_elytra:item/" + armor.overlayLayerName());
				nextLayer++;
			}

			if (trim != null) {
				textures.addProperty("layer" + nextLayer,
						"armored_elytra:item/" + trim.trimTextureNameFor(armor.name()));
			}

			model.add("textures", textures);
			return model;
		}

		// -----------------------------------------------------------------
		// elytra.json item-definition builder
		// -----------------------------------------------------------------

		private static JsonObject buildElytraItemDefinition() {
			// Top-level structure: broken condition → select on custom_model_data
			JsonObject root = new JsonObject();
			JsonObject topModel = new JsonObject();
			root.add("model", topModel);

			topModel.addProperty("type", "minecraft:condition");
			topModel.addProperty("property", "minecraft:broken");
			topModel.add("on_true", brokenModel());

			// Select the armored-elytra variant based on which chestplate is embedded
			JsonObject select = new JsonObject();
			select.addProperty("type", "minecraft:select");
			select.addProperty("property", "minecraft:custom_model_data");

			JsonArray cases = new JsonArray();
			for (ArmorEntry armor : ARMORS) {
				JsonObject caseEntry = new JsonObject();
				caseEntry.addProperty("when", "minecraft:" + armor.name());
				caseEntry.add("model", buildTrimConditionTree(armor));
				cases.add(caseEntry);
			}
			select.add("cases", cases);
			select.add("fallback", vanillaElytraModel());

			topModel.add("on_false", select);
			return root;
		}

		/** Reference to the standard (non-broken) elytra model. */
		private static JsonObject vanillaElytraModel() {
			JsonObject m = new JsonObject();
			m.addProperty("type", "minecraft:model");
			m.addProperty("model", "minecraft:item/elytra");
			return m;
		}

		/** Reference to the broken elytra model. */
		private static JsonObject brokenModel() {
			JsonObject m = new JsonObject();
			m.addProperty("type", "minecraft:model");
			m.addProperty("model", "minecraft:item/elytra_broken");
			return m;
		}

		/**
		 * Builds a nested {@code minecraft:condition} tree that checks the
		 * {@code armored_elytra:trim_material} custom-data field to select the
		 * correct trim variant. Trims are checked from last to first so the
		 * innermost {@code on_false} is the plain (untrimmed) model.
		 */
		private static JsonObject buildTrimConditionTree(ArmorEntry armor) {
			JsonObject current = armorModel(armor, null); // plain model = innermost fallback

			List<TrimMaterial> reversed = new ArrayList<>(TRIM_MATERIALS);
			Collections.reverse(reversed);

			for (TrimMaterial trim : reversed) {
				JsonObject condition = new JsonObject();
				condition.addProperty("type", "minecraft:condition");
				condition.addProperty("property", "minecraft:component");
				condition.addProperty("predicate", "minecraft:custom_data");

				JsonObject value = new JsonObject();
				value.addProperty("armored_elytra:trim_material", "minecraft:" + trim.name());
				condition.add("value", value);

				condition.add("on_true", armorModel(armor, trim));
				condition.add("on_false", current);
				current = condition;
			}

			return current;
		}

		/**
		 * Returns a {@code minecraft:model} reference for the given armor and
		 * optional trim. Leather chestplate gets additional tint entries.
		 */
		private static JsonObject armorModel(ArmorEntry armor, TrimMaterial trim) {
			JsonObject m = new JsonObject();
			m.addProperty("type", "minecraft:model");

			String modelName = trim == null
					? "armored_elytra:item/elytra_" + armor.name()
					: "armored_elytra:item/elytra_" + armor.name() + "_" + trim.modelSuffixFor(armor.name());
			m.addProperty("model", modelName);

			if (armor.hasOverlay()) {
				// Leather needs tints: layer0 (elytra) = white, layer1 (leather) = dye colour
				JsonArray tints = new JsonArray();

				JsonObject whiteTint = new JsonObject();
				whiteTint.addProperty("type", "minecraft:constant");
				JsonArray whiteVal = new JsonArray();
				whiteVal.add(1);
				whiteVal.add(1);
				whiteVal.add(1);
				whiteTint.add("value", whiteVal);
				tints.add(whiteTint);

				JsonObject dyeTint = new JsonObject();
				dyeTint.addProperty("type", "minecraft:dye");
				dyeTint.addProperty("default", -6265536);
				tints.add(dyeTint);

				m.add("tints", tints);
			}

			return m;
		}

		// -----------------------------------------------------------------
		// JSON save helper
		// -----------------------------------------------------------------

		@SuppressWarnings("deprecation")
		private static void saveJson(JsonObject json, Path dst, CachedOutput cache)
				throws IOException {
			Files.createDirectories(dst.getParent());
			byte[] bytes = (GSON.toJson(json) + "\n").getBytes(StandardCharsets.UTF_8);
			cache.writeIfNeeded(dst, bytes, Hashing.sha1().hashBytes(bytes));
		}
	}
}

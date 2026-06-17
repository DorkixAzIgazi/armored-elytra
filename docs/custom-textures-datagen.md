# Generating Custom Armored Elytra Textures from a Resource Pack

This guide explains how to regenerate the mod's item textures so they match a custom resource pack or texture pack. The mod ships with textures generated from the default Minecraft art. If you use a texture pack that reskins armor or elytra, you can run the data generator yourself to produce matching icons.

---

## What the generator does

The data generator reads vanilla Minecraft textures from the `source_assets/` folder and produces:

- Shifted item textures (elytra, each chestplate, leather overlay, trim overlays) that align correctly when layered
- One item model JSON per armor × trim combination
- The `elytra.json` item definition that routes to the right model at runtime

The output lands in `src/client/generated/resources/` and is included in the mod jar when you build.

---

## Prerequisites

### Java 25

The data generator runs on Java 25. To check whether it is already installed, open a terminal and run:

```
java -version
```

You should see output like `openjdk version "25.x.x"`. If the command is not found or the version is below 21, download and install a JDK 25 build — [Eclipse Temurin](https://adoptium.net/) is a free, widely-used option. During installation, tick the option to set `JAVA_HOME` and add Java to your `PATH` so that the `java` command works from any terminal.

### Git and the mod source code

You need the full mod source code, not just the compiled jar.

1. Install [Git](https://git-scm.com/downloads) if you do not have it already.
2. Clone the repository to a folder on your machine:

   ```
   git clone https://github.com/DorkixAzIgazi/armored-elytra.git
   ```

3. Open a terminal inside the cloned folder (the one that contains `gradlew.bat`). All commands in this guide must be run from there.

### Gradle (no installation needed)

The repository includes a `gradlew.bat` (Windows) / `gradlew` (Linux/macOS) wrapper script that downloads the correct version of Gradle automatically the first time it runs. You do not need to install Gradle separately.

> **Windows note:** If you see a security warning when running `gradlew.bat` for the first time, click **Run anyway** (or run `Set-ExecutionPolicy RemoteSigned` in PowerShell if the script is blocked outright).

---

## Step 1 — Replace the source textures

The generator reads textures from the `source_assets/minecraft/` folder in the project root. Its layout mirrors a standard Minecraft resource pack's `assets/minecraft/` folder.

The files that matter for item icon generation are:

```
source_assets/
└── minecraft/
    └── textures/
        ├── item/
        │   ├── elytra.png
        │   ├── chainmail_chestplate.png
        │   ├── copper_chestplate.png
        │   ├── diamond_chestplate.png
        │   ├── golden_chestplate.png
        │   ├── iron_chestplate.png
        │   ├── leather_chestplate.png
        │   ├── leather_chestplate_overlay.png
        │   └── netherite_chestplate.png
        └── trims/
            ├── color_palettes/
            │   ├── trim_palette.png        ← grayscale key (do not replace)
            │   ├── amethyst.png
            │   ├── copper.png
            │   ├── copper_darker.png
            │   ├── diamond.png
            │   ├── diamond_darker.png
            │   ├── emerald.png
            │   ├── gold.png
            │   ├── gold_darker.png
            │   ├── iron.png
            │   ├── iron_darker.png
            │   ├── lapis.png
            │   ├── netherite.png
            │   ├── netherite_darker.png
            │   ├── quartz.png
            │   ├── redstone.png
            │   └── resin.png
            └── items/
                └── chestplate_trim.png
```

Copy the textures you want to customise from your resource pack into the matching paths under `source_assets/`. You only need to replace the files you have custom versions of — anything you leave as-is will use the vanilla defaults.

> **Note:** Do not replace `trim_palette.png`. It is an internal grayscale key that the generator uses to map palette indices. Replacing it will produce wrong trim colors.

---

## Step 2 — Run the data generator

Open a terminal in the project root folder (the one that contains `gradlew.bat`) and run:

**Windows / Linux / macOS:**

```sh
./gradlew runDatagen
```

Gradle will download dependencies on the first run, which may take a few minutes. Subsequent runs are faster.

When it completes you should see a `BUILD SUCCESSFUL` message. The generated files are written to:

```
src/client/generated/resources/assets/
├── armored_elytra/
│   ├── models/item/        ← .json model files
│   └── textures/item/      ← .png texture files
└── minecraft/
    └── items/
        └── elytra.json     ← item definition
```

---

## Step 3 — Package the assets into a resource pack

Instead of rebuilding the mod jar, you can ship the generated assets as a standalone resource pack zip that players drop into their `resourcepacks/` folder.

1. Create a new folder with any name, e.g. `armored-elytra-custom/`.

2. Inside it, create a `pack.mcmeta` file with the following content (use `pack_format` **69** for Minecraft 1.21.11 — update the number if your version differs https://minecraft.wiki/w/Pack_format):

   ```json
   {
     "pack": {
       "min_format": 69,
       "max_format": 69,
       "description": "§c§l§oCustom Armored Elytra Icons"
     }
   }
   ```

3. Copy the entire `assets/` folder from the datagen output into your new folder:

   ```
   src/client/generated/resources/assets/  →  armored-elytra-custom/assets/
   ```

   The final layout should look like:

   ```
   armored-elytra-custom/
   ├── pack.mcmeta
   └── assets/
       ├── armored_elytra/
       │   ├── models/item/
       │   └── textures/item/
       └── minecraft/
           └── items/
               └── elytra.json
   ```

4. Zip the **contents** of `armored-elytra-custom/` (not the folder itself) into `armored-elytra-custom.zip`.

   On Windows you can select both `pack.mcmeta` and `assets/` inside the folder, right-click → **Compress to ZIP file**.

5. Place the zip in your Minecraft `resourcepacks/` folder, enable it in-game, and make sure it sits **above** any other packs that modify elytra or armor item models.

---

## Troubleshooting

| Symptom                                        | Likely cause                                                                                                                                                                                                  |
| ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Could not locate the 'source_assets/' folder` | Run the command from the project root, not from a subdirectory                                                                                                                                                |
| Textures look misaligned                       | Your texture pack uses a different resolution — the shift is `height / 8` pixels, so a 32×32 texture shifts by 4px, 64×64 by 8px, etc. Make sure all textures in `source_assets/` use the **same resolution** |
| Trim colors are wrong                          | `trim_palette.png` was replaced — restore it from the original repository                                                                                                                                     |
| `BUILD FAILED` / Java not found                | Make sure Java 21 is installed; set `JAVA_HOME` if needed                                                                                                                                                     |

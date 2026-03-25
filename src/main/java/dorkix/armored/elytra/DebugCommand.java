package dorkix.armored.elytra;

import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;

public class DebugCommand {

  private static final List<ItemStackTemplate> NON_LEATHER_ARMORS = List.of(
      new ItemStackTemplate(Items.CHAINMAIL_CHESTPLATE),
      new ItemStackTemplate(Items.COPPER_CHESTPLATE),
      new ItemStackTemplate(Items.DIAMOND_CHESTPLATE),
      new ItemStackTemplate(Items.GOLDEN_CHESTPLATE),
      new ItemStackTemplate(Items.IRON_CHESTPLATE),
      new ItemStackTemplate(Items.NETHERITE_CHESTPLATE));

  // All trim materials supported by this mod
  private static final List<ResourceKey<TrimMaterial>> TRIM_MATERIAL_KEYS = List.of(
      TrimMaterials.AMETHYST,
      TrimMaterials.COPPER,
      TrimMaterials.DIAMOND,
      TrimMaterials.EMERALD,
      TrimMaterials.GOLD,
      TrimMaterials.IRON,
      TrimMaterials.LAPIS,
      TrimMaterials.NETHERITE,
      TrimMaterials.QUARTZ,
      TrimMaterials.REDSTONE,
      TrimMaterials.RESIN);

  public static void register() {
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      dispatcher.register(Commands.literal("ae_debug")
          .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
          .then(Commands.literal("spawn_all")
              .executes(DebugCommand::spawnAll)));
    });
  }

  private static int spawnAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    var source = ctx.getSource();
    ServerPlayer player = source.getPlayerOrException();
    var level = source.getLevel();
    var access = ContainerLevelAccess.create(level, player.blockPosition());

    var registryAccess = source.getServer().registryAccess();
    Registry<TrimMaterial> trimMaterialRegistry = registryAccess.lookupOrThrow(Registries.TRIM_MATERIAL);
    Registry<TrimPattern> trimPatternRegistry = registryAccess.lookupOrThrow(Registries.TRIM_PATTERN);
    // Use "sentry" as placeholder pattern — only the material color matters for
    // icon testing
    TrimPattern sentryPatternValue = trimPatternRegistry.getValueOrThrow(TrimPatterns.SENTRY);
    Holder<TrimPattern> sentryPattern = trimPatternRegistry.wrapAsHolder(sentryPatternValue);

    double x = player.getX(), y = player.getY(), z = player.getZ();

    var elytra = new ItemStack(Items.ELYTRA);
    var brokenElytra = new ItemStack(Items.ELYTRA);
    brokenElytra.set(DataComponents.DAMAGE, brokenElytra.getMaxDamage()); // 0 remaining durability

    // Prepare the three leather variants we test
    var leatherDefault = new ItemStack(Items.LEATHER_CHESTPLATE); // default brown dye color
    var leatherNoColor = new ItemStack(Items.LEATHER_CHESTPLATE);
    leatherNoColor.remove(DataComponents.DYED_COLOR); // no color component
    var leatherBlue = new ItemStack(Items.LEATHER_CHESTPLATE);
    leatherBlue.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3355FF)); // explicit blue dye

    // ── no trim: normal elytra + broken elytra, all armor variants ──────
    for (var baseElytra : List.of(elytra, brokenElytra)) {
      for (var armor : NON_LEATHER_ARMORS) {
        give(level, x, y, z, ArmoredElytra.createArmoredElytra(baseElytra.copy(), armor.create(), access, null));
      }
      give(level, x, y, z, ArmoredElytra.createArmoredElytra(baseElytra.copy(), leatherDefault.copy(), access, null));
      give(level, x, y, z, ArmoredElytra.createArmoredElytra(baseElytra.copy(), leatherNoColor.copy(), access, null));
      give(level, x, y, z, ArmoredElytra.createArmoredElytra(baseElytra.copy(), leatherBlue.copy(), access, null));
    }

    // ── with trim: normal elytra only, all armor variants × all trims ───
    for (var trimKey : TRIM_MATERIAL_KEYS) {
      TrimMaterial materialValue = trimMaterialRegistry.getValueOrThrow(trimKey);
      Holder<TrimMaterial> materialHolder = trimMaterialRegistry.wrapAsHolder(materialValue);
      var trim = new ArmorTrim(materialHolder, sentryPattern);

      for (var armor : NON_LEATHER_ARMORS) {
        var armorWithTrim = armor.create();
        armorWithTrim.set(DataComponents.TRIM, trim);
        give(level, x, y, z, ArmoredElytra.createArmoredElytra(elytra.copy(), armorWithTrim, access, null));
      }

      var leatherDefaultTrim = leatherDefault.copy();
      leatherDefaultTrim.set(DataComponents.TRIM, trim);
      give(level, x, y, z, ArmoredElytra.createArmoredElytra(elytra.copy(), leatherDefaultTrim, access, null));

      var leatherNoColorTrim = leatherNoColor.copy();
      leatherNoColorTrim.set(DataComponents.TRIM, trim);
      give(level, x, y, z, ArmoredElytra.createArmoredElytra(elytra.copy(), leatherNoColorTrim, access, null));

      var leatherBlueTrim = leatherBlue.copy();
      leatherBlueTrim.set(DataComponents.TRIM, trim);
      give(level, x, y, z, ArmoredElytra.createArmoredElytra(elytra.copy(), leatherBlueTrim, access, null));
    }

    // 9 no-trim × 2 elytra variants = 18, 9 × 11 trims = 99 → 117 total
    source.sendSuccess(() -> Component.literal("Spawned 117 armored elytra items!"), false);
    return Command.SINGLE_SUCCESS;
  }

  private static void give(ServerLevel level, double x, double y, double z, ItemStack stack) {
    ItemEntity entity = new ItemEntity(level, x, y, z, stack);
    entity.setDefaultPickUpDelay();
    level.addFreshEntity(entity);
  }
}

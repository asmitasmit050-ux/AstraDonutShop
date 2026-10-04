package net.notdakuxd.nxdonutshop.util;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class NxSpriteResolver {
   private static final Map<String, NxSpriteResolver.TargetSprite> SPRITE_OVERRIDES = Map.ofEntries(
      new Entry[]{
         Map.entry("SNOW_BLOCK", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/snow"))),
         Map.entry("SNOW", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/snow"))),
         Map.entry("SMOOTH_SANDSTONE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/sandstone_top"))),
         Map.entry("SMOOTH_RED_SANDSTONE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/red_sandstone_top"))),
         Map.entry("SMOOTH_BASALT", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/smooth_basalt_side"))),
         Map.entry("SMOOTH_STONE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/smooth_stone"))),
         Map.entry("SMOOTH_QUARTZ", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/smooth_quartz"))),
         Map.entry("QUARTZ_BLOCK", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/quartz_block_side"))),
         Map.entry(
            "CHISELED_QUARTZ_BLOCK", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/chiseled_quartz_block"))
         ),
         Map.entry("QUARTZ_PILLAR", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/quartz_pillar"))),
         Map.entry("QUARTZ_BRICKS", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/quartz_bricks"))),
         Map.entry("GLASS_PANE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/glass"))),
         Map.entry("WHEAT", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/wheat"))),
         Map.entry("WHEAT_SEEDS", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/wheat_seeds"))),
         Map.entry("STONECUTTER", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/stonecutter_side"))),
         Map.entry("POINTED_DRIPSTONE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/pointed_dripstone"))),
         Map.entry("SUNFLOWER", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/sunflower"))),
         Map.entry("PITCHER_POD", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/pitcher_pod"))),
         Map.entry("PITCHER_PLANT", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/pitcher_plant"))),
         Map.entry("RESIN_CLUMP", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/resin_clump"))),
         Map.entry("RESIN_BRICKS", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/resin_bricks"))),
         Map.entry("RESIN_BRICK_WALL", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/resin_bricks"))),
         Map.entry("LEVER", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/lever"))),
         Map.entry(
            "ENCHANTED_GOLDEN_APPLE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/enchanted_golden_apple"))
         ),
         Map.entry("GOLDEN_APPLE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/golden_apple"))),
         Map.entry("LIGHTNING_ROD", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/lightning_rod"))),
         Map.entry("LECTERN", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/lectern_top"))),
         Map.entry("NETHER_WART", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/nether_wart"))),
         Map.entry("NETHER_WART_BLOCK", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/nether_wart_block"))),
         Map.entry("CHEST", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/chest_minecart"))),
         Map.entry("TRAPPED_CHEST", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/chest_minecart"))),
         Map.entry("ENDER_CHEST", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/obsidian"))),
         Map.entry("RECOVERY_COMPASS", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/recovery_compass_00"))),
         Map.entry("CLOCK", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/clock_00"))),
         Map.entry("COMPASS", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/compass_00"))),
         Map.entry("BARREL", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/barrel_side"))),
         Map.entry("COMPOSTER", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/composter_side"))),
         Map.entry("SMITHING_TABLE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/smithing_table_front"))),
         Map.entry("FLETCHING_TABLE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/fletching_table_front"))),
         Map.entry("BARRIER", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/barrier"))),
         Map.entry("SPYGLASS", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "items"), Key.key("minecraft", "item/spyglass"))),
         Map.entry("COBWEB", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/cobweb"))),
         Map.entry("GRASS_BLOCK", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/grass_block_side"))),
         Map.entry("ANCIENT_DEBRIS", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/ancient_debris_side"))),
         Map.entry("AZALEA", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/azalea_side"))),
         Map.entry("FLOWERING_AZALEA", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/flowering_azalea_side"))),
         Map.entry("BASALT", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/basalt_side"))),
         Map.entry("POLISHED_BASALT", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/polished_basalt_side"))),
         Map.entry("BONE_BLOCK", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/bone_block_side"))),
         Map.entry("DRIED_KELP_BLOCK", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/dried_kelp_side"))),
         Map.entry("MYCELIUM", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/mycelium_side"))),
         Map.entry("PODZOL", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/podzol_side"))),
         Map.entry("DIRT_PATH", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/dirt_path_side"))),
         Map.entry("FURNACE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/furnace_front"))),
         Map.entry("CRAFTING_TABLE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/crafting_table_front"))),
         Map.entry("TNT", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/tnt_side"))),
         Map.entry("CACTUS", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/cactus_side"))),
         Map.entry("PUMPKIN", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/pumpkin_side"))),
         Map.entry("JACK_O_LANTERN", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/jack_o_lantern"))),
         Map.entry("BEACON", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/beacon"))),
         Map.entry("DISPENSER", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/dispenser_front"))),
         Map.entry("DROPPER", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/dropper_front"))),
         Map.entry("OBSERVER", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/observer_front"))),
         Map.entry("PISTON", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/piston_top"))),
         Map.entry("STICKY_PISTON", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/piston_top_sticky"))),
         Map.entry("SMOKER", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/smoker_front"))),
         Map.entry("BLAST_FURNACE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/blast_furnace_front"))),
         Map.entry("ENCHANTING_TABLE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/enchanting_table_top"))),
         Map.entry("BOOKSHELF", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/bookshelf"))),
         Map.entry(
            "CHISELED_BOOKSHELF", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/chiseled_bookshelf_empty"))
         ),
         Map.entry("JUKEBOX", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/jukebox_top"))),
         Map.entry("NOTE_BLOCK", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/note_block"))),
         Map.entry("DAYLIGHT_DETECTOR", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/daylight_detector_top"))),
         Map.entry("TARGET", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/target_side"))),
         Map.entry("HAY_BLOCK", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/hay_block_side"))),
         Map.entry("BEE_NEST", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/bee_nest_side"))),
         Map.entry("BEEHIVE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/beehive_side"))),
         Map.entry("LODESTONE", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/lodestone_side"))),
         Map.entry("RESPAWN_ANCHOR", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/respawn_anchor_top"))),
         Map.entry("ANVIL", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/anvil"))),
         Map.entry("CHIPPED_ANVIL", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/chipped_anvil_top"))),
         Map.entry("DAMAGED_ANVIL", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/damaged_anvil_top"))),
         Map.entry("RAIL", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/rail"))),
         Map.entry("POWERED_RAIL", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/powered_rail"))),
         Map.entry("DETECTOR_RAIL", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/detector_rail"))),
         Map.entry("ACTIVATOR_RAIL", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/activator_rail"))),
         Map.entry("CRAFTER", new NxSpriteResolver.TargetSprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "block/crafter_top")))
      }
   );
   private static final Set<String> ITEM_ATLAS_ITEMS = Set.of(
      new String[]{
         "REDSTONE",
         "REPEATER",
         "COMPARATOR",
         "LEVER",
         "STRING",
         "HOPPER",
         "CAULDRON",
         "BREWING_STAND",
         "ITEM_FRAME",
         "GLOW_ITEM_FRAME",
         "PAINTING",
         "ARMOR_STAND",
         "CAMPFIRE",
         "SOUL_CAMPFIRE",
         "LANTERN",
         "SOUL_LANTERN",
         "CHAIN",
         "END_CRYSTAL",
         "FLOWER_POT",
         "SUGAR_CANE",
         "KELP",
         "BAMBOO",
         "BELL",
         "BARRIER",
         "SPYGLASS",
         "LIGHTNING_ROD",
         "BRUSH",
         "BUNDLE",
         "MACE",
         "WIND_CHARGE",
         "BREEZE_ROD",
         "TRIAL_KEY",
         "OMINOUS_TRIAL_KEY",
         "OMINOUS_BOTTLE",
         "WHEAT",
         "WHEAT_SEEDS",
         "POINTED_DRIPSTONE",
         "SUNFLOWER",
         "PITCHER_POD",
         "PITCHER_PLANT",
         "RESIN_CLUMP"
      }
   );

   public static Component renderItemSprite(Material material) {
      if (material != null && !material.isAir()) {
         try {
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(Class.forName("dev.nx.emojis.api.NxEmojisApi"));
            if (rsp != null) {
               Object api = rsp.getProvider();
               if (api != null) {
                  Method replaceMethod = api.getClass().getMethod("replace", new Class[]{Component.class});
                  Component textComponent = Component.text(":item:" + material.name().toLowerCase(Locale.ROOT) + ":");
                  if (replaceMethod.invoke(api, new Object[]{textComponent}) instanceof Component c) {
                     return c;
                  }
               }
            }
         } catch (Throwable var7) {
         }

         NxSpriteResolver.TargetSprite target = resolveTargetSprite(material);
         return Component.object(ObjectContents.sprite(target.atlas(), target.sprite()));
      } else {
         return Component.empty();
      }
   }

   public static NxSpriteResolver.TargetSprite resolveTargetSprite(Material material) {
      String name = material.name();
      String matKey = material.getKey().getKey();
      String namespace = material.getKey().getNamespace();
      if (SPRITE_OVERRIDES.containsKey(name)) {
         return (NxSpriteResolver.TargetSprite)SPRITE_OVERRIDES.get(name);
      }

      if (name.endsWith("_HELMET")
         || name.endsWith("_CHESTPLATE")
         || name.endsWith("_LEGGINGS")
         || name.endsWith("_BOOTS")
         || name.endsWith("_SWORD")
         || name.endsWith("_PICKAXE")
         || name.endsWith("_AXE")
         || name.endsWith("_SHOVEL")
         || name.endsWith("_HOE")
         || name.equals("BOW")
         || name.equals("TRIDENT")
         || name.equals("MACE")
         || name.equals("FISHING_ROD")
         || name.equals("SHEARS")
         || name.equals("FLINT_AND_STEEL")
         || name.endsWith("_SMITHING_TEMPLATE")
         || name.endsWith("_DISC")) {
         return new NxSpriteResolver.TargetSprite(Key.key(namespace, "items"), Key.key(namespace, "item/" + matKey));
      }

      if (name.endsWith("_DOOR")
         || name.endsWith("_SIGN")
         || name.endsWith("_BOAT")
         || name.endsWith("_RAFT")
         || name.endsWith("_MINECART")
         || ITEM_ATLAS_ITEMS.contains(name)) {
         return new NxSpriteResolver.TargetSprite(Key.key(namespace, "items"), Key.key(namespace, "item/" + matKey));
      }

      if (name.endsWith("_WOOD")) {
         return new NxSpriteResolver.TargetSprite(Key.key(namespace, "blocks"), Key.key(namespace, "block/" + matKey.replace("_wood", "_log")));
      }

      if (name.endsWith("_HYPHAE")) {
         return new NxSpriteResolver.TargetSprite(Key.key(namespace, "blocks"), Key.key(namespace, "block/" + matKey.replace("_hyphae", "_stem")));
      }

      if (name.endsWith("_BUTTON") || name.endsWith("_PRESSURE_PLATE") || name.endsWith("_FENCE") || name.endsWith("_FENCE_GATE") || name.endsWith("_WALL")) {
         String parentPlank = getParentBlockTexture(name);
         if (parentPlank != null) {
            return new NxSpriteResolver.TargetSprite(Key.key(namespace, "blocks"), Key.key(namespace, "block/" + parentPlank));
         }
      }

      return material.isBlock()
         ? new NxSpriteResolver.TargetSprite(Key.key(namespace, "blocks"), Key.key(namespace, "block/" + matKey))
         : new NxSpriteResolver.TargetSprite(Key.key(namespace, "items"), Key.key(namespace, "item/" + matKey));
   }

   private static String getParentBlockTexture(String name) {
      if (name.startsWith("ACACIA_")) {
         return "acacia_planks";
      } else if (name.startsWith("OAK_")) {
         return "oak_planks";
      } else if (name.startsWith("BIRCH_")) {
         return "birch_planks";
      } else if (name.startsWith("SPRUCE_")) {
         return "spruce_planks";
      } else if (name.startsWith("JUNGLE_")) {
         return "jungle_planks";
      } else if (name.startsWith("DARK_OAK_")) {
         return "dark_oak_planks";
      } else if (name.startsWith("MANGROVE_")) {
         return "mangrove_planks";
      } else if (name.startsWith("CHERRY_")) {
         return "cherry_planks";
      } else if (name.startsWith("BAMBOO_")) {
         return "bamboo_planks";
      } else if (name.startsWith("CRIMSON_")) {
         return "crimson_planks";
      } else if (name.startsWith("WARPED_")) {
         return "warped_planks";
      } else if (name.startsWith("PALE_OAK_")) {
         return "pale_oak_planks";
      } else if (name.startsWith("COBBLESTONE_") || name.equals("COBBLESTONE_WALL")) {
         return "cobblestone";
      } else if (name.startsWith("MOSSY_COBBLESTONE_")) {
         return "mossy_cobblestone";
      } else if (name.startsWith("STONE_BRICK_")) {
         return "stone_bricks";
      } else if (name.startsWith("MOSSY_STONE_BRICK_")) {
         return "mossy_stone_bricks";
      } else if (name.startsWith("STONE_")) {
         return "stone";
      } else if (name.startsWith("SMOOTH_STONE_")) {
         return "smooth_stone";
      } else if (name.startsWith("BRICK_") || name.equals("BRICK_WALL")) {
         return "bricks";
      } else if (name.startsWith("NETHER_BRICK_") || name.equals("NETHER_BRICK_WALL")) {
         return "nether_bricks";
      } else if (name.startsWith("RED_NETHER_BRICK_") || name.equals("RED_NETHER_BRICK_WALL")) {
         return "red_nether_bricks";
      } else if (name.startsWith("SANDSTONE_") || name.equals("SANDSTONE_WALL")) {
         return "sandstone";
      } else if (name.startsWith("RED_SANDSTONE_") || name.equals("RED_SANDSTONE_WALL")) {
         return "red_sandstone";
      } else if (name.startsWith("PRISMARINE_BRICK_")) {
         return "prismarine_bricks";
      } else if (name.startsWith("DARK_PRISMARINE_")) {
         return "dark_prismarine";
      } else if (name.startsWith("PRISMARINE_") || name.equals("PRISMARINE_WALL")) {
         return "prismarine";
      } else if (name.startsWith("PURPUR_")) {
         return "purpur_block";
      } else if (name.startsWith("QUARTZ_")) {
         return "quartz_block_side";
      } else if (name.startsWith("SMOOTH_QUARTZ_")) {
         return "smooth_quartz";
      } else if (name.startsWith("DEEPSLATE_BRICK_") || name.equals("DEEPSLATE_BRICK_WALL")) {
         return "deepslate_bricks";
      } else if (name.startsWith("DEEPSLATE_TILE_") || name.equals("DEEPSLATE_TILE_WALL")) {
         return "deepslate_tiles";
      } else if (name.startsWith("COBBLED_DEEPSLATE_") || name.equals("COBBLED_DEEPSLATE_WALL")) {
         return "cobbled_deepslate";
      } else if (name.startsWith("POLISHED_DEEPSLATE_") || name.equals("POLISHED_DEEPSLATE_WALL")) {
         return "polished_deepslate";
      } else if (name.startsWith("POLISHED_BLACKSTONE_BRICK_") || name.equals("POLISHED_BLACKSTONE_BRICK_WALL")) {
         return "polished_blackstone_bricks";
      } else if (name.startsWith("POLISHED_BLACKSTONE_") || name.equals("POLISHED_BLACKSTONE_WALL")) {
         return "polished_blackstone";
      } else if (name.startsWith("BLACKSTONE_") || name.equals("BLACKSTONE_WALL")) {
         return "blackstone";
      } else if (name.startsWith("POLISHED_ANDESITE_")) {
         return "polished_andesite";
      } else if (name.startsWith("ANDESITE_") || name.equals("ANDESITE_WALL")) {
         return "andesite";
      } else if (name.startsWith("POLISHED_DIORITE_")) {
         return "polished_diorite";
      } else if (name.startsWith("DIORITE_") || name.equals("DIORITE_WALL")) {
         return "diorite";
      } else if (name.startsWith("POLISHED_GRANITE_")) {
         return "polished_granite";
      } else if (name.startsWith("GRANITE_") || name.equals("GRANITE_WALL")) {
         return "granite";
      } else if (name.startsWith("POLISHED_TUFF_") || name.equals("POLISHED_TUFF_WALL")) {
         return "polished_tuff";
      } else if (name.startsWith("TUFF_BRICK_") || name.equals("TUFF_BRICK_WALL")) {
         return "tuff_bricks";
      } else if (name.startsWith("TUFF_") || name.equals("TUFF_WALL")) {
         return "tuff";
      } else if (name.startsWith("MUD_BRICK_") || name.equals("MUD_BRICK_WALL")) {
         return "mud_bricks";
      } else if (name.startsWith("END_STONE_BRICK_") || name.equals("END_STONE_BRICK_WALL")) {
         return "end_stone_bricks";
      } else {
         return !name.startsWith("RESIN_BRICK_") && !name.equals("RESIN_BRICK_WALL") ? null : "resin_bricks";
      }
   }

   public record TargetSprite(Key atlas, Key sprite) {
   }
}

package net.notdakuxd.nxdonutshop.shop;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.Map.Entry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class EnchantmentManager {
   private final JavaPlugin plugin;
   private final Map<String, Map<Integer, Double>> customLevelPrices = new HashMap();
   private final Map<String, Double> pricePerLevel = new HashMap();
   private File configFile;
   private FileConfiguration config;

   public EnchantmentManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.loadConfig();
   }

   public void loadConfig() {
      if (!this.plugin.getDataFolder().exists()) {
         this.plugin.getDataFolder().mkdirs();
      }

      this.configFile = new File(this.plugin.getDataFolder(), "enchantment.yml");
      if (!this.configFile.exists()) {
         this.plugin.saveResource("enchantment.yml", false);
      }

      this.config = YamlConfiguration.loadConfiguration(this.configFile);
      this.customLevelPrices.clear();
      this.pricePerLevel.clear();
      ConfigurationSection section = this.config.getConfigurationSection("enchantments");
      if (section != null) {
         for (String key : section.getKeys(false)) {
            String upperKey = key.toUpperCase(Locale.ROOT);
            if (section.isConfigurationSection(key + ".levels")) {
               ConfigurationSection levelsSec = section.getConfigurationSection(key + ".levels");
               if (levelsSec != null) {
                  Map<Integer, Double> lvlMap = new HashMap();

                  for (String lvlStr : levelsSec.getKeys(false)) {
                     try {
                        int lvl = Integer.parseInt(lvlStr);
                        double price = levelsSec.getDouble(lvlStr, 50.0);
                        lvlMap.put(lvl, price);
                     } catch (Exception var12) {
                     }
                  }

                  this.customLevelPrices.put(upperKey, lvlMap);
               }
            } else if (section.contains(key + ".price-per-level")) {
               double ppl = section.getDouble(key + ".price-per-level", 50.0);
               this.pricePerLevel.put(upperKey, ppl);
            } else if (section.contains(key + ".price")) {
               double p = section.getDouble(key + ".price", 50.0);
               this.pricePerLevel.put(upperKey, p);
            }
         }
      }
   }

   public double getEnchantmentPrice(Enchantment ench, int level) {
      if (ench != null && level > 0) {
         String key = ench.getKey().getKey().toUpperCase(Locale.ROOT);
         String name = ench.getName().toUpperCase(Locale.ROOT);
         if (this.customLevelPrices.containsKey(key) && ((Map)this.customLevelPrices.get(key)).containsKey(level)) {
            return (Double)((Map)this.customLevelPrices.get(key)).get(level);
         } else if (this.customLevelPrices.containsKey(name) && ((Map)this.customLevelPrices.get(name)).containsKey(level)) {
            return (Double)((Map)this.customLevelPrices.get(name)).get(level);
         } else if (this.pricePerLevel.containsKey(key)) {
            return (Double)this.pricePerLevel.get(key) * level;
         } else {
            return this.pricePerLevel.containsKey(name) ? (Double)this.pricePerLevel.get(name) * level : 50.0 * level;
         }
      } else {
         return 0.0;
      }
   }

   public double getTotalEnchantmentPrice(Map<Enchantment, Integer> enchants) {
      if (enchants != null && !enchants.isEmpty()) {
         double total = 0.0;

         for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            total += this.getEnchantmentPrice((Enchantment)entry.getKey(), (Integer)entry.getValue());
         }

         return total;
      } else {
         return 0.0;
      }
   }

   public boolean isEnchantable(Material material) {
      if (material != null && !material.isAir() && material.isItem()) {
         String name = material.name();
         if (!name.endsWith("_SWORD")
            && !name.endsWith("_PICKAXE")
            && !name.endsWith("_AXE")
            && !name.endsWith("_SHOVEL")
            && !name.endsWith("_HOE")
            && !name.endsWith("_HELMET")
            && !name.endsWith("_CHESTPLATE")
            && !name.endsWith("_LEGGINGS")
            && !name.endsWith("_BOOTS")
            && !name.equals("BOW")
            && !name.equals("CROSSBOW")
            && !name.equals("TRIDENT")
            && !name.equals("MACE")
            && !name.equals("FISHING_ROD")
            && !name.equals("SHEARS")
            && !name.equals("FLINT_AND_STEEL")
            && !name.equals("SHIELD")
            && !name.equals("BRUSH")
            && !name.equals("ELYTRA")
            && !name.equals("CARROT_ON_A_STICK")
            && !name.equals("WARPED_FUNGUS_ON_A_STICK")
            && !name.equals("ENCHANTED_BOOK")
            && !name.equals("BOOK")) {
            try {
               ItemStack dummy = new ItemStack(material);

               for (Enchantment e : Enchantment.values()) {
                  if (!e.isCursed() && e.canEnchantItem(dummy)) {
                     return true;
                  }
               }
            } catch (Throwable var8) {
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public List<Enchantment> getApplicableEnchantments(Material material) {
      List<Enchantment> result = new ArrayList();
      if (!this.isEnchantable(material)) {
         return result;
      }

      ItemStack dummy = new ItemStack(material);
      String name = material.name();

      for (Enchantment ench : this.getPriorityOrderForMaterial(name)) {
         if (ench != null && !ench.isCursed() && (ench.canEnchantItem(dummy) || this.isUniversalOrCustomAllowed(name, ench)) && !result.contains(ench)) {
            result.add(ench);
         }
      }

      for (Enchantment ench : Enchantment.values()) {
         if (ench != null && !ench.isCursed() && !result.contains(ench) && (ench.canEnchantItem(dummy) || this.isUniversalOrCustomAllowed(name, ench))) {
            result.add(ench);
         }
      }

      return result;
   }

   private List<Enchantment> getPriorityOrderForMaterial(String matName) {
      List<Enchantment> list = new ArrayList();
      if (matName.endsWith("_SWORD")) {
         this.addIfNotNull(list, Enchantment.SHARPNESS);
         this.addIfNotNull(list, Enchantment.SMITE);
         this.addIfNotNull(list, Enchantment.BANE_OF_ARTHROPODS);
         this.addIfNotNull(list, Enchantment.SWEEPING_EDGE);
         this.addIfNotNull(list, Enchantment.FIRE_ASPECT);
         this.addIfNotNull(list, Enchantment.KNOCKBACK);
         this.addIfNotNull(list, Enchantment.LOOTING);
         this.addIfNotNull(list, Enchantment.UNBREAKING);
         this.addIfNotNull(list, Enchantment.MENDING);
      } else if (matName.endsWith("_PICKAXE") || matName.endsWith("_SHOVEL") || matName.endsWith("_HOE")) {
         this.addIfNotNull(list, Enchantment.EFFICIENCY);
         this.addIfNotNull(list, Enchantment.SILK_TOUCH);
         this.addIfNotNull(list, Enchantment.FORTUNE);
         this.addIfNotNull(list, Enchantment.UNBREAKING);
         this.addIfNotNull(list, Enchantment.MENDING);
      } else if (matName.endsWith("_AXE")) {
         this.addIfNotNull(list, Enchantment.SHARPNESS);
         this.addIfNotNull(list, Enchantment.SMITE);
         this.addIfNotNull(list, Enchantment.BANE_OF_ARTHROPODS);
         this.addIfNotNull(list, Enchantment.EFFICIENCY);
         this.addIfNotNull(list, Enchantment.SILK_TOUCH);
         this.addIfNotNull(list, Enchantment.FORTUNE);
         this.addIfNotNull(list, Enchantment.UNBREAKING);
         this.addIfNotNull(list, Enchantment.MENDING);
      } else if (matName.endsWith("_HELMET")) {
         this.addIfNotNull(list, Enchantment.PROTECTION);
         this.addIfNotNull(list, Enchantment.FIRE_PROTECTION);
         this.addIfNotNull(list, Enchantment.BLAST_PROTECTION);
         this.addIfNotNull(list, Enchantment.PROJECTILE_PROTECTION);
         this.addIfNotNull(list, Enchantment.RESPIRATION);
         this.addIfNotNull(list, Enchantment.AQUA_AFFINITY);
         this.addIfNotNull(list, Enchantment.THORNS);
         this.addIfNotNull(list, Enchantment.UNBREAKING);
         this.addIfNotNull(list, Enchantment.MENDING);
      } else if (matName.endsWith("_CHESTPLATE") || matName.endsWith("_LEGGINGS")) {
         this.addIfNotNull(list, Enchantment.PROTECTION);
         this.addIfNotNull(list, Enchantment.FIRE_PROTECTION);
         this.addIfNotNull(list, Enchantment.BLAST_PROTECTION);
         this.addIfNotNull(list, Enchantment.PROJECTILE_PROTECTION);
         this.addIfNotNull(list, Enchantment.THORNS);
         if (matName.endsWith("_LEGGINGS")) {
            this.addIfNotNull(list, Enchantment.SWIFT_SNEAK);
         }

         this.addIfNotNull(list, Enchantment.UNBREAKING);
         this.addIfNotNull(list, Enchantment.MENDING);
      } else if (matName.endsWith("_BOOTS")) {
         this.addIfNotNull(list, Enchantment.PROTECTION);
         this.addIfNotNull(list, Enchantment.FIRE_PROTECTION);
         this.addIfNotNull(list, Enchantment.BLAST_PROTECTION);
         this.addIfNotNull(list, Enchantment.PROJECTILE_PROTECTION);
         this.addIfNotNull(list, Enchantment.FEATHER_FALLING);
         this.addIfNotNull(list, Enchantment.DEPTH_STRIDER);
         this.addIfNotNull(list, Enchantment.FROST_WALKER);
         this.addIfNotNull(list, Enchantment.SOUL_SPEED);
         this.addIfNotNull(list, Enchantment.THORNS);
         this.addIfNotNull(list, Enchantment.UNBREAKING);
         this.addIfNotNull(list, Enchantment.MENDING);
      } else if (matName.equals("BOW")) {
         this.addIfNotNull(list, Enchantment.POWER);
         this.addIfNotNull(list, Enchantment.PUNCH);
         this.addIfNotNull(list, Enchantment.FLAME);
         this.addIfNotNull(list, Enchantment.INFINITY);
         this.addIfNotNull(list, Enchantment.UNBREAKING);
         this.addIfNotNull(list, Enchantment.MENDING);
      } else if (matName.equals("CROSSBOW")) {
         this.addIfNotNull(list, Enchantment.QUICK_CHARGE);
         this.addIfNotNull(list, Enchantment.MULTISHOT);
         this.addIfNotNull(list, Enchantment.PIERCING);
         this.addIfNotNull(list, Enchantment.UNBREAKING);
         this.addIfNotNull(list, Enchantment.MENDING);
      } else if (matName.equals("TRIDENT")) {
         this.addIfNotNull(list, Enchantment.IMPALING);
         this.addIfNotNull(list, Enchantment.LOYALTY);
         this.addIfNotNull(list, Enchantment.CHANNELING);
         this.addIfNotNull(list, Enchantment.RIPTIDE);
         this.addIfNotNull(list, Enchantment.UNBREAKING);
         this.addIfNotNull(list, Enchantment.MENDING);
      } else if (matName.equals("MACE")) {
         this.addIfNotNull(list, Enchantment.DENSITY);
         this.addIfNotNull(list, Enchantment.BREACH);
         this.addIfNotNull(list, Enchantment.WIND_BURST);
         this.addIfNotNull(list, Enchantment.FIRE_ASPECT);
         this.addIfNotNull(list, Enchantment.SMITE);
         this.addIfNotNull(list, Enchantment.BANE_OF_ARTHROPODS);
         this.addIfNotNull(list, Enchantment.UNBREAKING);
         this.addIfNotNull(list, Enchantment.MENDING);
      } else if (matName.equals("FISHING_ROD")) {
         this.addIfNotNull(list, Enchantment.LUCK_OF_THE_SEA);
         this.addIfNotNull(list, Enchantment.LURE);
         this.addIfNotNull(list, Enchantment.UNBREAKING);
         this.addIfNotNull(list, Enchantment.MENDING);
      }

      return list;
   }

   private void addIfNotNull(List<Enchantment> list, Enchantment e) {
      if (e != null && !e.isCursed()) {
         list.add(e);
      }
   }

   private boolean isUniversalOrCustomAllowed(String matName, Enchantment ench) {
      if (ench != null && !ench.isCursed()) {
         String eName = ench.getKey().getKey().toLowerCase(Locale.ROOT);
         return eName.contains("unbreaking") || eName.contains("mending");
      } else {
         return false;
      }
   }

   public static String formatEnchantmentName(Enchantment ench) {
      if (ench == null) {
         return "Unknown";
      }

      String key = ench.getKey().getKey();
      if (!"sweeping_edge".equalsIgnoreCase(key) && !"sweeping".equalsIgnoreCase(key)) {
         if ("fire_aspect".equalsIgnoreCase(key)) {
            return "Fire Aspect";
         }

         if ("bane_of_arthropods".equalsIgnoreCase(key)) {
            return "Bane of Arthropods";
         }

         if ("silk_touch".equalsIgnoreCase(key)) {
            return "Silk Touch";
         }

         if ("feather_falling".equalsIgnoreCase(key)) {
            return "Feather Falling";
         }

         if ("fire_protection".equalsIgnoreCase(key)) {
            return "Fire Protection";
         }

         if ("blast_protection".equalsIgnoreCase(key)) {
            return "Blast Protection";
         }

         if ("projectile_protection".equalsIgnoreCase(key)) {
            return "Projectile Protection";
         }

         if ("aqua_affinity".equalsIgnoreCase(key)) {
            return "Aqua Affinity";
         }

         if ("depth_strider".equalsIgnoreCase(key)) {
            return "Depth Strider";
         }

         if ("frost_walker".equalsIgnoreCase(key)) {
            return "Frost Walker";
         }

         if ("soul_speed".equalsIgnoreCase(key)) {
            return "Soul Speed";
         }

         if ("swift_sneak".equalsIgnoreCase(key)) {
            return "Swift Sneak";
         }

         if ("quick_charge".equalsIgnoreCase(key)) {
            return "Quick Charge";
         }

         if ("luck_of_the_sea".equalsIgnoreCase(key)) {
            return "Luck of the Sea";
         }

         if ("wind_burst".equalsIgnoreCase(key)) {
            return "Wind Burst";
         }

         String[] parts = key.split("_");
         StringBuilder sb = new StringBuilder();

         for (String part : parts) {
            if (!part.isEmpty()) {
               sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT)).append(" ");
            }
         }

         return sb.toString().trim();
      } else {
         return "Sweeping Edge";
      }
   }

   public static String toRoman(int level) {
      return switch (level) {
         case 1 -> "I";
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         case 5 -> "V";
         case 6 -> "VI";
         case 7 -> "VII";
         case 8 -> "VIII";
         case 9 -> "IX";
         case 10 -> "X";
         default -> String.valueOf(level);
      };
   }

   public static Enchantment matchEnchantment(String name) {
      if (name != null && !name.isBlank()) {
         String clean = name.toLowerCase(Locale.ROOT).replace("minecraft:", "").trim();

         try {
            Enchantment e = Enchantment.getByKey(NamespacedKey.minecraft(clean));
            if (e != null) {
               return e;
            }
         } catch (Exception var7) {
         }

         try {
            Enchantment e = Enchantment.getByName(clean.toUpperCase(Locale.ROOT));
            if (e != null) {
               return e;
            }
         } catch (Exception var6) {
         }

         for (Enchantment e : Enchantment.values()) {
            if (e.getKey().getKey().equalsIgnoreCase(clean) || e.getName().equalsIgnoreCase(clean)) {
               return e;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static String encodeEnchantments(Map<Enchantment, Integer> map) {
      if (map != null && !map.isEmpty()) {
         StringJoiner sj = new StringJoiner(",");

         for (Entry<Enchantment, Integer> entry : map.entrySet()) {
            sj.add(((Enchantment)entry.getKey()).getKey().getKey().toUpperCase(Locale.ROOT) + ":" + entry.getValue());
         }

         return sj.toString();
      } else {
         return "";
      }
   }

   public static Map<Enchantment, Integer> decodeEnchantments(String raw) {
      Map<Enchantment, Integer> map = new LinkedHashMap();
      if (raw != null && !raw.isBlank()) {
         String[] tokens = raw.split(",");

         for (String token : tokens) {
            if (token.contains(":")) {
               String[] pair = token.split(":");

               try {
                  int lvl = Integer.parseInt(pair[1].trim());
                  Enchantment ench = matchEnchantment(pair[0]);
                  if (ench != null && lvl > 0) {
                     map.put(ench, lvl);
                  }
               } catch (Exception var10) {
               }
            }
         }

         return map;
      } else {
         return map;
      }
   }

   public static ItemStack applyEnchantments(ItemStack item, Map<Enchantment, Integer> map) {
      if (item == null) {
         return null;
      }

      if (map != null && !map.isEmpty()) {
         for (Entry<Enchantment, Integer> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && (Integer)entry.getValue() > 0) {
               item.addUnsafeEnchantment((Enchantment)entry.getKey(), (Integer)entry.getValue());
            }
         }
      }

      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ARMOR_TRIM});
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         item.setItemMeta(meta);
      }

      return item;
   }
}

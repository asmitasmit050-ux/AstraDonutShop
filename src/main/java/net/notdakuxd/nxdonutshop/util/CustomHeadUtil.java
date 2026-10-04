package net.notdakuxd.nxdonutshop.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class CustomHeadUtil {
   public static final String DONUT_HASH = "307e91bc884b5131f59466cfc1285eb1eaf5d50c190b43d6c87a057959e7af08";
   public static final String MONEY_BAG_HASH = "c86185b1d519ade58582bc4b2f56f25e4082c569d9de7879f71d688addeea";
   public static final String RED_CROSS_HASH = "beb588b21a6f98ad1ff4e085c572f1d532573965ab11eedd455ccf75646218";
   public static final String SEARCH_HASH = "10d751b048d31b43f15a5f345e2fa455caf3bcfe8336cf5ed16c2f847a6728f";
   public static final String PREV_ARROW_HASH = "bd69e06e5f982d95e8e91788b7678b2243ab802afaff2d868572adb8366";
   public static final String NEXT_ARROW_HASH = "19bf3292e126a105b54eba713aa1b152d541723a274489174dd1854a5b9d53";

   public static ItemStack createCustomHead(String textureHash, Component displayName, List<Component> lore) {
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)head.getItemMeta();
      if (meta != null) {
         try {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.getTextures().setSkin(new URL("http://textures.minecraft.net/texture/" + textureHash));
            meta.setPlayerProfile(profile);
         } catch (Exception e) {
            try {
               PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
               String base64 = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + textureHash + "\"}}}";
               String encoded = Base64.getEncoder().encodeToString(base64.getBytes());
               profile.setProperty(new ProfileProperty("textures", encoded));
               meta.setPlayerProfile(profile);
            } catch (Exception var9) {
            }
         }

         for (ItemFlag flag : ItemFlag.values()) {
            meta.addItemFlags(new ItemFlag[]{flag});
         }

         if (displayName != null) {
            meta.displayName(displayName);
         }

         if (lore != null) {
            meta.lore(lore);
         }

         head.setItemMeta(meta);
      }

      return head;
   }
}

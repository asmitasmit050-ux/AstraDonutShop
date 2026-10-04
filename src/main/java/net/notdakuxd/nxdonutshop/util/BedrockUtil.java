package net.notdakuxd.nxdonutshop.util;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BedrockUtil {
   public static boolean isBedrockPlayer(Player player) {
      if (player == null) {
         return false;
      }

      UUID uuid = player.getUniqueId();
      if (Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
         try {
            Class<?> clazz = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = clazz.getMethod("getInstance", new Class[0]);
            Object instance = getInstance.invoke(null, new Object[0]);
            Method isFloodgate = clazz.getMethod("isFloodgatePlayer", new Class[]{UUID.class});
            return (Boolean)isFloodgate.invoke(instance, new Object[]{uuid});
         } catch (Throwable var7) {
         }
      }

      if (Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot")) {
         try {
            Class<?> clazz = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Method apiMethod = clazz.getMethod("api", new Class[0]);
            Object api = apiMethod.invoke(null, new Object[0]);
            Method isBedrock = clazz.getMethod("isBedrockPlayer", new Class[]{UUID.class});
            return (Boolean)isBedrock.invoke(api, new Object[]{uuid});
         } catch (Throwable var6) {
         }
      }

      String name = player.getName();
      return name.startsWith(".") || name.startsWith("*");
   }
}

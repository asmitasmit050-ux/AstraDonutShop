package net.notdakuxd.nxdonutshop.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents.Builder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SkinRestorerHeadResolver {
   private static final Pattern VALUE_PATTERN = Pattern.compile("\"(?:Value|value)\"\\s*:\\s*\"([^\"]+)\"");
   private static final Pattern SIG_PATTERN = Pattern.compile("\"(?:Signature|signature)\"\\s*:\\s*\"([^\"]+)\"");

   public static Component resolvePlayerHeadComponent(Player player) {
      if (player == null) {
         return Component.text("\ud83d\udc64 ");
      }

      String name = player.getName();
      UUID uuid = player.getUniqueId();
      String[] texture = getLiveSkinTexture(player);

      try {
         Builder builder = ObjectContents.playerHead().name(name).id(uuid).hat(true);
         if (texture != null && texture[0] != null && !texture[0].isBlank()) {
            if (texture[1] != null && !texture[1].isBlank()) {
               builder.profileProperty(PlayerHeadObjectContents.property("textures", texture[0], texture[1]));
            } else {
               builder.profileProperty(PlayerHeadObjectContents.property("textures", texture[0]));
            }
         }

         return Component.object(builder.build());
      } catch (Throwable fallback) {
         try {
            return Component.object(ObjectContents.playerHead(name));
         } catch (Throwable err) {
            return Component.text("\ud83d\udc64 ");
         }
      }
   }

   public static String[] getLiveSkinTexture(Player player) {
      String name = player.getName();
      String lowerName = name.toLowerCase();

      try {
         PlayerProfile profile = player.getPlayerProfile();

         for (ProfileProperty prop : profile.getProperties()) {
            if ("textures".equalsIgnoreCase(prop.getName()) && prop.getValue() != null && !prop.getValue().isBlank()) {
               return new String[]{prop.getValue(), prop.getSignature()};
            }
         }
      } catch (Throwable var19) {
      }

      if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
         try {
            Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
            Object api = providerClass.getMethod("get", new Class[0]).invoke(null, new Object[0]);
            Object playerStorage = api.getClass().getMethod("getPlayerStorage", new Class[0]).invoke(api, new Object[0]);
            Optional<?> skinOpt = Optional.empty();

            try {
               skinOpt = (Optional<?>)playerStorage.getClass()
                  .getMethod("getSkinForPlayer", new Class[]{UUID.class, String.class})
                  .invoke(playerStorage, new Object[]{player.getUniqueId(), name});
            } catch (Throwable t1) {
               try {
                  skinOpt = (Optional<?>)playerStorage.getClass()
                     .getMethod("getSkinForPlayer", new Class[]{String.class})
                     .invoke(playerStorage, new Object[]{name});
               } catch (Throwable t2) {
                  try {
                     skinOpt = (Optional<?>)playerStorage.getClass()
                        .getMethod("getSkinOfPlayer", new Class[]{String.class})
                        .invoke(playerStorage, new Object[]{name});
                  } catch (Throwable var14) {
                  }
               }
            }

            if (skinOpt.isPresent()) {
               Object prop = skinOpt.get();
               String val = (String)prop.getClass().getMethod("getValue", new Class[0]).invoke(prop, new Object[0]);
               String sig = (String)prop.getClass().getMethod("getSignature", new Class[0]).invoke(prop, new Object[0]);
               if (val != null && !val.isBlank()) {
                  return new String[]{val, sig};
               }
            }
         } catch (Throwable var17) {
         }

         try {
            Class<?> legacyApiClass = Class.forName("net.skinsrestorer.api.SkinsRestorerAPI");
            Object api = legacyApiClass.getMethod("getApi", new Class[0]).invoke(null, new Object[0]);
            Object prop = api.getClass().getMethod("getSkinData", new Class[]{String.class}).invoke(api, new Object[]{name});
            if (prop != null) {
               String val = (String)prop.getClass().getMethod("getValue", new Class[0]).invoke(prop, new Object[0]);
               String sig = (String)prop.getClass().getMethod("getSignature", new Class[0]).invoke(prop, new Object[0]);
               if (val != null && !val.isBlank()) {
                  return new String[]{val, sig};
               }
            }
         } catch (Throwable var13) {
         }

         try {
            File[] checkFiles = new File[]{
               new File("plugins/SkinsRestorer/players/" + lowerName + ".json"),
               new File("plugins/SkinsRestorer/players/" + name + ".json"),
               new File("plugins/SkinsRestorer/players/" + player.getUniqueId() + ".json")
            };

            for (File file : checkFiles) {
               if (file.exists()) {
                  String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                  Matcher valMatcher = VALUE_PATTERN.matcher(content);
                  if (valMatcher.find()) {
                     String val = valMatcher.group(1);
                     String sig = null;
                     Matcher sigMatcher = SIG_PATTERN.matcher(content);
                     if (sigMatcher.find()) {
                        sig = sigMatcher.group(1);
                     }

                     if (val != null && !val.isBlank()) {
                        return new String[]{val, sig};
                     }
                  }
               }
            }
         } catch (Throwable var18) {
         }
      }

      return null;
   }
}

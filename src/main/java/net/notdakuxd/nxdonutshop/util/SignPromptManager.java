package net.notdakuxd.nxdonutshop.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.notdakuxd.nxdonutshop.NxDonutShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public class SignPromptManager implements Listener {
   private static final Map<UUID, SignPromptManager.SignSession> activeSessions = new HashMap();
   private static final MiniMessage MINI = MiniMessage.miniMessage();

   public static void openSignPrompt(Player player, String[] defaultLines, Consumer<String[]> callback) {
      cleanup(player.getUniqueId());
      player.closeInventory();
      Bukkit.getScheduler()
         .runTaskLater(
            NxDonutShop.getInstance(),
            () -> {
               if (player.isOnline()) {
                  Location loc = player.getLocation().getBlock().getLocation().add(0.0, 2.0, 0.0);
                  if (loc.getY() >= player.getWorld().getMaxHeight()) {
                     loc = player.getLocation().getBlock().getLocation().subtract(0.0, 2.0, 0.0);
                  }

                  Block block = loc.getBlock();
                  BlockData originalData = block.getBlockData();
                  block.setType(Material.OAK_SIGN, false);
                  if (!(block.getState() instanceof Sign sign)) {
                     block.setBlockData(originalData, false);
                  } else {
                     sign.setWaxed(false);
                     Side var10 = Side.FRONT;

                     for (int timeoutTask = 0; timeoutTask < 4; timeoutTask++) {
                        String lineText = defaultLines != null && timeoutTask < defaultLines.length && defaultLines[timeoutTask] != null
                           ? defaultLines[timeoutTask]
                           : "";
                        sign.getSide(var10).line(timeoutTask, Component.text(lineText));
                     }

                     sign.update(true, false);
                     BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(NxDonutShop.getInstance(), () -> cleanup(player.getUniqueId()), 600L);
                     activeSessions.put(player.getUniqueId(), new SignPromptManager.SignSession(loc, originalData, callback, timeoutTask));
                     Bukkit.getScheduler().runTaskLater(NxDonutShop.getInstance(), () -> {
                        try {
                           player.openSign(sign, var10);
                        } catch (Exception e) {
                           cleanup(player.getUniqueId());
                        }
                     }, 2L);
                  }
               }
            },
            4L
         );
   }

   public static void cleanup(UUID uuid) {
      SignPromptManager.SignSession session = (SignPromptManager.SignSession)activeSessions.remove(uuid);
      if (session != null) {
         if (session.timeoutTask() != null) {
            session.timeoutTask().cancel();
         }

         Location loc = session.loc();
         if (loc.getWorld() != null) {
            loc.getBlock().setBlockData(session.originalData(), false);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
               p.sendBlockChange(loc, session.originalData());
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onSignChange(SignChangeEvent event) {
      Player player = event.getPlayer();
      SignPromptManager.SignSession session = (SignPromptManager.SignSession)activeSessions.get(player.getUniqueId());
      if (session != null && event.getBlock().getLocation().equals(session.loc())) {
         event.setCancelled(true);
         activeSessions.remove(player.getUniqueId());
         if (session.timeoutTask() != null) {
            session.timeoutTask().cancel();
         }

         session.loc().getBlock().setBlockData(session.originalData(), false);
         player.sendBlockChange(session.loc(), session.originalData());
         String[] lines = new String[4];

         for (int i = 0; i < 4; i++) {
            Component comp = event.line(i);
            if (comp instanceof TextComponent tc) {
               lines[i] = tc.content().trim();
            } else if (comp != null) {
               lines[i] = MINI.stripTags((String)MINI.serialize(comp)).trim();
            } else {
               lines[i] = "";
            }
         }

         Bukkit.getScheduler().runTaskLater(NxDonutShop.getInstance(), () -> {
            if (player.isOnline()) {
               session.callback().accept(lines);
            }
         }, 5L);
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onBlockBreak(BlockBreakEvent event) {
      for (SignPromptManager.SignSession session : activeSessions.values()) {
         if (event.getBlock().getLocation().equals(session.loc())) {
            event.setCancelled(true);
            return;
         }
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      cleanup(event.getPlayer().getUniqueId());
   }

   public record SignSession(Location loc, BlockData originalData, Consumer<String[]> callback, BukkitTask timeoutTask) {
   }
}

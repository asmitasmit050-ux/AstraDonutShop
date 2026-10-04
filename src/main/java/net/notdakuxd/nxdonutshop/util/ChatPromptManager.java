package net.notdakuxd.nxdonutshop.util;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.notdakuxd.nxdonutshop.NxDonutShop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public class ChatPromptManager implements Listener {
   private static final Map<UUID, ChatPromptManager.ChatSession> activeSessions = new HashMap();
   private static final MiniMessage MINI = MiniMessage.miniMessage();

   public static void prompt(Player player, String promptMessage, Consumer<String> callback) {
      cancel(player.getUniqueId());
      player.closeInventory();
      player.sendMessage("");
      player.sendMessage(ChatColor.GOLD + "========================================");
      player.sendMessage(ChatColor.YELLOW + " \ud83d\udd0d " + ChatColor.BOLD + promptMessage);
      player.sendMessage(
         ChatColor.GRAY
            + " Type in chat and press "
            + ChatColor.WHITE
            + "[ENTER]"
            + ChatColor.GRAY
            + " (or type "
            + ChatColor.RED
            + "'cancel'"
            + ChatColor.GRAY
            + ")"
      );
      player.sendMessage(ChatColor.GOLD + "========================================");
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8F, 1.2F);
      BukkitTask timeout = Bukkit.getScheduler().runTaskLater(NxDonutShop.getInstance(), () -> {
         ChatPromptManager.ChatSession s = (ChatPromptManager.ChatSession)activeSessions.remove(player.getUniqueId());
         if (s != null) {
            player.sendMessage(ChatColor.RED + "Search prompt timed out.");
         }
      }, 600L);
      activeSessions.put(player.getUniqueId(), new ChatPromptManager.ChatSession(callback, timeout));
   }

   public static void cancel(UUID uuid) {
      ChatPromptManager.ChatSession s = (ChatPromptManager.ChatSession)activeSessions.remove(uuid);
      if (s != null && s.timeoutTask() != null) {
         s.timeoutTask().cancel();
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      ChatPromptManager.ChatSession session = (ChatPromptManager.ChatSession)activeSessions.remove(player.getUniqueId());
      if (session != null) {
         event.setCancelled(true);
         if (session.timeoutTask() != null) {
            session.timeoutTask().cancel();
         }

         String message = event.getMessage().trim();
         Bukkit.getScheduler().runTask(NxDonutShop.getInstance(), () -> {
            if ("cancel".equalsIgnoreCase(message)) {
               player.sendMessage(ChatColor.RED + "Action cancelled.");
               NxDonutShop.getInstance().openQuickBuyGUI(player);
            } else {
               session.callback().accept(message);
            }
         });
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onPaperChat(AsyncChatEvent event) {
      Player player = event.getPlayer();
      ChatPromptManager.ChatSession session = (ChatPromptManager.ChatSession)activeSessions.remove(player.getUniqueId());
      if (session != null) {
         event.setCancelled(true);
         if (session.timeoutTask() != null) {
            session.timeoutTask().cancel();
         }

         Component comp = event.message();
         String plain = comp instanceof TextComponent tc ? tc.content().trim() : MINI.stripTags((String)MINI.serialize(comp)).trim();
         Bukkit.getScheduler().runTask(NxDonutShop.getInstance(), () -> {
            if ("cancel".equalsIgnoreCase(plain)) {
               player.sendMessage(ChatColor.RED + "Action cancelled.");
               NxDonutShop.getInstance().openQuickBuyGUI(player);
            } else {
               session.callback().accept(plain);
            }
         });
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      cancel(event.getPlayer().getUniqueId());
   }

   public record ChatSession(Consumer<String> callback, BukkitTask timeoutTask) {
   }
}

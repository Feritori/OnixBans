package ru.onixmine.onixbans;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * Слушает подключение (проверяет бан) и чат (проверяет мут).
 */
public class BanListener implements Listener {

    private final OnixBansPlugin plugin;

    public BanListener(OnixBansPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Проверка бана при входе ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent e) {
        String name = e.getPlayer().getName();
        BanEntry ban = plugin.getBanManager().getBan(name);
        if (ban == null) return;

        String screen = plugin.msg("ban-screen",
                "{reason}", ban.reason,
                "{until}", ban.formattedDate());
        e.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacySection().deserialize(screen));
    }

    // ── Блокировка чата при муте ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent e) {
        String name = e.getPlayer().getName();
        BanEntry mute = plugin.getBanManager().getMute(name);
        if (mute == null) return;

        e.setCancelled(true);
        e.getPlayer().sendMessage(plugin.msg("mute-blocked",
                "{until}", mute.formattedDate(),
                "{reason}", mute.reason));
    }
}

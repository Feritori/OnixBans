package ru.onixmine.onixbans;

import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Отправляет embed-сообщения в Discord через Webhook.
 * Все HTTP-запросы выполняются асинхронно.
 */
public class DiscordLogger {

    private final OnixBansPlugin plugin;

    // Цвета embed (decimal)
    private static final int COLOR_KICK   = 0xFFFF00; // жёлтый
    private static final int COLOR_BAN    = 0xFF0000; // красный
    private static final int COLOR_MUTE   = 0x9B59B6; // фиолетовый
    private static final int COLOR_UNBAN  = 0x00FF00; // зелёный
    private static final int COLOR_UNMUTE = 0x00FF00;

    public DiscordLogger(OnixBansPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("discord.enabled", false);
    }

    private String webhookUrl() {
        return plugin.getConfig().getString("discord.webhook-url", "");
    }

    private String serverName() {
        return plugin.getConfig().getString("server-name", "Server");
    }

    // ── Публичные методы ──────────────────────────────────────────────────────

    public void logKick(String player, String by, String reason) {
        if (!isEnabled()) return;
        String json = buildEmbed(
                "🦵 Кик",
                COLOR_KICK,
                field("Игрок", player, true),
                field("Модератор", by, true),
                field("Причина", reason, false)
        );
        sendAsync(json);
    }

    public void logBan(String player, String by, String reason, String until) {
        if (!isEnabled()) return;
        String json = buildEmbed(
                "🔨 Временный бан",
                COLOR_BAN,
                field("Игрок", player, true),
                field("Модератор", by, true),
                field("До", until, true),
                field("Причина", reason, false)
        );
        sendAsync(json);
    }

    public void logMute(String player, String by, String reason, String until) {
        if (!isEnabled()) return;
        String json = buildEmbed(
                "🔇 Временный мут",
                COLOR_MUTE,
                field("Игрок", player, true),
                field("Модератор", by, true),
                field("До", until, true),
                field("Причина", reason, false)
        );
        sendAsync(json);
    }

    public void logUnban(String player, String by) {
        if (!isEnabled()) return;
        String json = buildEmbed(
                "✅ Разбан",
                COLOR_UNBAN,
                field("Игрок", player, true),
                field("Модератор", by, true)
        );
        sendAsync(json);
    }

    public void logUnmute(String player, String by) {
        if (!isEnabled()) return;
        String json = buildEmbed(
                "🔊 Размут",
                COLOR_UNMUTE,
                field("Игрок", player, true),
                field("Модератор", by, true)
        );
        sendAsync(json);
    }

    // ── Построение JSON ───────────────────────────────────────────────────────

    private String buildEmbed(String title, int color, String... fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"embeds\":[{");
        sb.append("\"title\":\"").append(escape(title)).append("\",");
        sb.append("\"color\":").append(color).append(",");
        sb.append("\"footer\":{\"text\":\"").append(escape(serverName())).append("\"},");
        sb.append("\"timestamp\":\"").append(java.time.Instant.now()).append("\",");
        sb.append("\"fields\":[");
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(fields[i]);
        }
        sb.append("]}]}");
        return sb.toString();
    }

    private String field(String name, String value, boolean inline) {
        return "{\"name\":\"" + escape(name) + "\","
                + "\"value\":\"" + escape(value) + "\","
                + "\"inline\":" + inline + "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    // ── Отправка ──────────────────────────────────────────────────────────────

    private void sendAsync(String json) {
        String url = webhookUrl();
        if (url == null || url.isBlank() || url.contains("YOUR_WEBHOOK")) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection con = (HttpURLConnection)
                        URI.create(url).toURL().openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                try (OutputStream os = con.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                int code = con.getResponseCode();
                if (code < 200 || code >= 300) {
                    plugin.getLogger().warning("Discord webhook вернул код: " + code);
                }
                con.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка отправки в Discord: " + e.getMessage());
            }
        });
    }
}

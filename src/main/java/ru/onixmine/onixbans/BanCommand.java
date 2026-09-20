package ru.onixmine.onixbans;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BanCommand implements CommandExecutor, TabCompleter {

    private final OnixBansPlugin plugin;

    public BanCommand(OnixBansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        return switch (cmd.getName().toLowerCase()) {
            case "kick"     -> cmdKick(sender, args);
            case "tempban"  -> cmdTempBan(sender, args);
            case "tempmute" -> cmdTempMute(sender, args);
            case "unban"    -> cmdUnban(sender, args);
            case "unmute"   -> cmdUnmute(sender, args);
            default         -> false;
        };
    }

    // ── /kick <игрок> [причина] ───────────────────────────────────────────────

    private boolean cmdKick(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.msg("usage-kick"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.msg("not-found", "{player}", args[0]));
            return true;
        }
        if (target.hasPermission("onixbans.bypass")) {
            sender.sendMessage(plugin.msg("bypass"));
            return true;
        }
        String reason = args.length > 1 ? joinFrom(args, 1) : "Не указана";
        String by     = sender.getName();

        String screen = plugin.msg("kick-screen", "{reason}", reason);
        target.kick(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(screen));

        sender.sendMessage(OnixBansPlugin.color("&aИгрок &e" + target.getName() + " &aкикнут. Причина: &f" + reason));
        plugin.getDiscordLogger().logKick(target.getName(), by, reason);
        return true;
    }

    // ── /tempban <игрок> <время> [причина] ───────────────────────────────────

    private boolean cmdTempBan(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("usage-tempban"));
            return true;
        }
        String targetName = args[0];
        long duration = TimeUtil.parse(args[1]);
        if (duration <= 0) {
            sender.sendMessage(plugin.msg("usage-tempban"));
            return true;
        }
        if (plugin.getBanManager().isBanned(targetName)) {
            sender.sendMessage(plugin.msg("already-banned", "{player}", targetName));
            return true;
        }

        // Проверяем bypass для онлайн-игроков
        Player online = Bukkit.getPlayer(targetName);
        if (online != null && online.hasPermission("onixbans.bypass")) {
            sender.sendMessage(plugin.msg("bypass"));
            return true;
        }

        String reason    = args.length > 2 ? joinFrom(args, 2) : "Не указана";
        String by        = sender.getName();
        long   expiresAt = System.currentTimeMillis() + duration;

        BanEntry entry = new BanEntry(BanEntry.Type.BAN, targetName, reason, by, expiresAt);
        plugin.getBanManager().addBan(entry);

        // Кикаем если онлайн
        if (online != null) {
            String screen = plugin.msg("ban-screen",
                    "{reason}", reason,
                    "{until}", entry.formattedDate());
            online.kick(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().deserialize(screen));
        }

        sender.sendMessage(OnixBansPlugin.color(
                "&aИгрок &e" + targetName + " &aзабанен на &e" + entry.formattedExpiry()
                + "&a. Причина: &f" + reason));
        plugin.getDiscordLogger().logBan(targetName, by, reason, entry.formattedDate());
        return true;
    }

    // ── /tempmute <игрок> <время> [причина] ──────────────────────────────────

    private boolean cmdTempMute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("usage-tempmute"));
            return true;
        }
        String targetName = args[0];
        long duration = TimeUtil.parse(args[1]);
        if (duration <= 0) {
            sender.sendMessage(plugin.msg("usage-tempmute"));
            return true;
        }
        if (plugin.getBanManager().isMuted(targetName)) {
            sender.sendMessage(plugin.msg("already-muted", "{player}", targetName));
            return true;
        }

        Player online = Bukkit.getPlayer(targetName);
        if (online != null && online.hasPermission("onixbans.bypass")) {
            sender.sendMessage(plugin.msg("bypass"));
            return true;
        }

        String reason    = args.length > 2 ? joinFrom(args, 2) : "Не указана";
        String by        = sender.getName();
        long   expiresAt = System.currentTimeMillis() + duration;

        BanEntry entry = new BanEntry(BanEntry.Type.MUTE, targetName, reason, by, expiresAt);
        plugin.getBanManager().addMute(entry);

        if (online != null) {
            online.sendMessage(plugin.msg("mute-notify",
                    "{until}", entry.formattedDate(),
                    "{reason}", reason));
        }

        sender.sendMessage(OnixBansPlugin.color(
                "&aИгрок &e" + targetName + " &aзамучен на &e" + entry.formattedExpiry()
                + "&a. Причина: &f" + reason));
        plugin.getDiscordLogger().logMute(targetName, by, reason, entry.formattedDate());
        return true;
    }

    // ── /unban <игрок> ────────────────────────────────────────────────────────

    private boolean cmdUnban(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(OnixBansPlugin.color("&eИспользование: &f/unban <игрок>"));
            return true;
        }
        String name = args[0];
        if (!plugin.getBanManager().removeBan(name)) {
            sender.sendMessage(plugin.msg("not-banned", "{player}", name));
            return true;
        }
        sender.sendMessage(plugin.msg("unban-success", "{player}", name));
        plugin.getDiscordLogger().logUnban(name, sender.getName());
        return true;
    }

    // ── /unmute <игрок> ───────────────────────────────────────────────────────

    private boolean cmdUnmute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(OnixBansPlugin.color("&eИспользование: &f/unmute <игрок>"));
            return true;
        }
        String name = args[0];
        if (!plugin.getBanManager().removeMute(name)) {
            sender.sendMessage(plugin.msg("not-muted", "{player}", name));
            return true;
        }
        Player online = Bukkit.getPlayer(name);
        if (online != null) online.sendMessage(plugin.msg("unmute-notify"));
        sender.sendMessage(plugin.msg("unmute-success", "{player}", name));
        plugin.getDiscordLogger().logUnmute(name, sender.getName());
        return true;
    }

    // ── Tab Complete ──────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String name = cmd.getName().toLowerCase();
            if (name.equals("tempban") || name.equals("tempmute")) {
                return Arrays.asList("10m", "1h", "12h", "1d", "7d", "30d");
            }
        }
        return List.of();
    }

    // ── Утилита ───────────────────────────────────────────────────────────────

    private String joinFrom(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }
}

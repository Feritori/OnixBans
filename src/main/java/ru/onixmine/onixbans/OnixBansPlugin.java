package ru.onixmine.onixbans;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class OnixBansPlugin extends JavaPlugin {

    private BanManager banManager;
    private DiscordLogger discordLogger;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        discordLogger = new DiscordLogger(this);
        banManager    = new BanManager(this);

        BanCommand handler = new BanCommand(this);

        registerCmd("kick",     handler);
        registerCmd("tempban",  handler);
        registerCmd("tempmute", handler);
        registerCmd("unban",    handler);
        registerCmd("unmute",   handler);

        getServer().getPluginManager().registerEvents(new BanListener(this), this);

        getLogger().info("OnixBans включён.");
    }

    @Override
    public void onDisable() {
        if (banManager != null) banManager.save();
        getLogger().info("OnixBans выключен.");
    }

    private void registerCmd(String name, BanCommand handler) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
    }

    public BanManager getBanManager()       { return banManager; }
    public DiscordLogger getDiscordLogger() { return discordLogger; }

    /** Перевести цветовые коды &x */
    public static String color(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Подставить плейсхолдеры в строку из конфига */
    public String msg(String key, String... placeholders) {
        String s = getConfig().getString("messages." + key, key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            s = s.replace(placeholders[i], placeholders[i + 1]);
        }
        return color(s);
    }
}

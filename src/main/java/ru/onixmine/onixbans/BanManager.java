package ru.onixmine.onixbans;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Хранит активные баны и муты в data.yml.
 * Ключ — нижний регистр имени игрока.
 */
public class BanManager {

    private final OnixBansPlugin plugin;
    private final File dataFile;
    private FileConfiguration cfg;

    // playerName.toLowerCase() -> entry
    private final Map<String, BanEntry> bans  = new HashMap<>();
    private final Map<String, BanEntry> mutes = new HashMap<>();

    public BanManager(OnixBansPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        cfg = YamlConfiguration.loadConfiguration(dataFile);
        load();
    }

    // ── Баны ─────────────────────────────────────────────────────────────────

    public boolean isBanned(String name) {
        BanEntry e = bans.get(name.toLowerCase());
        if (e == null) return false;
        if (e.isExpired()) { bans.remove(name.toLowerCase()); save(); return false; }
        return true;
    }

    public BanEntry getBan(String name) {
        BanEntry e = bans.get(name.toLowerCase());
        if (e != null && e.isExpired()) { bans.remove(name.toLowerCase()); save(); return null; }
        return e;
    }

    public void addBan(BanEntry entry) {
        bans.put(entry.playerName.toLowerCase(), entry);
        save();
    }

    public boolean removeBan(String name) {
        boolean had = bans.remove(name.toLowerCase()) != null;
        if (had) save();
        return had;
    }

    // ── Муты ─────────────────────────────────────────────────────────────────

    public boolean isMuted(String name) {
        BanEntry e = mutes.get(name.toLowerCase());
        if (e == null) return false;
        if (e.isExpired()) { mutes.remove(name.toLowerCase()); save(); return false; }
        return true;
    }

    public BanEntry getMute(String name) {
        BanEntry e = mutes.get(name.toLowerCase());
        if (e != null && e.isExpired()) { mutes.remove(name.toLowerCase()); save(); return null; }
        return e;
    }

    public void addMute(BanEntry entry) {
        mutes.put(entry.playerName.toLowerCase(), entry);
        save();
    }

    public boolean removeMute(String name) {
        boolean had = mutes.remove(name.toLowerCase()) != null;
        if (had) save();
        return had;
    }

    // ── Сохранение / загрузка ─────────────────────────────────────────────────

    public void save() {
        cfg.set("bans",  null);
        cfg.set("mutes", null);
        writeEntries("bans",  bans);
        writeEntries("mutes", mutes);
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("Ошибка сохранения data.yml: " + e.getMessage());
        }
    }

    private void writeEntries(String section, Map<String, BanEntry> map) {
        for (Map.Entry<String, BanEntry> e : map.entrySet()) {
            String path = section + "." + e.getKey();
            BanEntry be = e.getValue();
            cfg.set(path + ".playerName", be.playerName);
            cfg.set(path + ".reason",     be.reason);
            cfg.set(path + ".bannedBy",   be.bannedBy);
            cfg.set(path + ".expiresAt",  be.expiresAt);
        }
    }

    private void load() {
        loadSection("bans",  BanEntry.Type.BAN,  bans);
        loadSection("mutes", BanEntry.Type.MUTE, mutes);
        // Очищаем истёкшие при загрузке
        bans.entrySet().removeIf(e -> e.getValue().isExpired());
        mutes.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private void loadSection(String section, BanEntry.Type type, Map<String, BanEntry> map) {
        var sec = cfg.getConfigurationSection(section);
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                String name     = cfg.getString(section + "." + key + ".playerName", key);
                String reason   = cfg.getString(section + "." + key + ".reason",   "Не указана");
                String bannedBy = cfg.getString(section + "." + key + ".bannedBy", "Console");
                long expiresAt  = cfg.getLong(section + "." + key + ".expiresAt",  -1);
                map.put(key, new BanEntry(type, name, reason, bannedBy, expiresAt));
            } catch (Exception ignored) {}
        }
    }
}

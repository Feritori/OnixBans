package ru.onixmine.onixbans;

/**
 * Запись о временном бане или муте.
 */
public class BanEntry {

    public enum Type { BAN, MUTE }

    public final Type   type;
    public final String playerName;
    public final String reason;
    public final String bannedBy;
    public final long   expiresAt; // System.currentTimeMillis(), -1 = навсегда

    public BanEntry(Type type, String playerName, String reason, String bannedBy, long expiresAt) {
        this.type       = type;
        this.playerName = playerName;
        this.reason     = reason;
        this.bannedBy   = bannedBy;
        this.expiresAt  = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != -1 && System.currentTimeMillis() >= expiresAt;
    }

    /** Форматированное время окончания */
    public String formattedExpiry() {
        if (expiresAt == -1) return "навсегда";
        long diff = expiresAt - System.currentTimeMillis();
        if (diff <= 0) return "истёк";
        return TimeUtil.formatDuration(diff);
    }

    /** Абсолютная дата окончания */
    public String formattedDate() {
        if (expiresAt == -1) return "навсегда";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
        return sdf.format(new java.util.Date(expiresAt));
    }
}

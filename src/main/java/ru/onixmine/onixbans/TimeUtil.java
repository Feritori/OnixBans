package ru.onixmine.onixbans;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилита для парсинга и форматирования времени.
 * Поддерживаемые форматы: 30s, 10m, 2h, 7d, 1d12h30m
 */
public final class TimeUtil {

    private static final Pattern TOKEN = Pattern.compile("(\\d+)([smhd])");

    private TimeUtil() {}

    /**
     * Парсит строку вида "1d12h30m" в миллисекунды.
     * @return миллисекунды или -1 если строка не распознана
     */
    public static long parse(String input) {
        if (input == null || input.isBlank()) return -1;
        Matcher m = TOKEN.matcher(input.toLowerCase());
        long total = 0;
        boolean found = false;
        while (m.find()) {
            found = true;
            long val = Long.parseLong(m.group(1));
            total += switch (m.group(2)) {
                case "s" -> val * 1_000L;
                case "m" -> val * 60_000L;
                case "h" -> val * 3_600_000L;
                case "d" -> val * 86_400_000L;
                default  -> 0L;
            };
        }
        return found ? total : -1;
    }

    /**
     * Форматирует миллисекунды в читаемую строку: "7д 12ч 30м 5с"
     */
    public static String formatDuration(long ms) {
        if (ms <= 0) return "0с";
        long s = ms / 1000;
        long days  = s / 86400; s %= 86400;
        long hours = s / 3600;  s %= 3600;
        long mins  = s / 60;    s %= 60;
        long secs  = s;

        StringBuilder sb = new StringBuilder();
        if (days  > 0) sb.append(days).append("д ");
        if (hours > 0) sb.append(hours).append("ч ");
        if (mins  > 0) sb.append(mins).append("м ");
        if (secs  > 0 || sb.isEmpty()) sb.append(secs).append("с");
        return sb.toString().trim();
    }
}

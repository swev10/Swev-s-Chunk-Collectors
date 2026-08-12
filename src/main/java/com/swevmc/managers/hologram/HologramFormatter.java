package com.swevmc.managers.hologram;

import com.swevmc.models.ChunkCollector;
import com.swevmc.utils.ColorUtils;

import java.util.List;

public final class HologramFormatter {

    private HologramFormatter() {
    }

    public static List<String> format(List<String> lines, ChunkCollector collector) {
        String time = formatTime(collector.getTimeRemaining());
        return lines.stream()
                .map(line -> line.replace("${amount}", String.format("%.2f", collector.getTotalMoneyEarned()))
                        .replace("${owner}", collector.getOwnerName())
                        .replace("${time}", time)
                        .replace("${battery}", collector.getBatteryBars()))
                .map(ColorUtils::translateColors)
                .toList();
    }

    private static String formatTime(long seconds) {
        if (seconds <= 0) {
            return "&cNo Charge";
        }
        long hours = seconds / 3600;
        long minutes = seconds % 3600 / 60;
        long remainingSeconds = seconds % 60;
        if (hours > 0) {
            return String.format("&e%dh %dm %ds", hours, minutes, remainingSeconds);
        }
        if (minutes > 0) {
            return String.format("&e%dm %ds", minutes, remainingSeconds);
        }
        return String.format("&e%ds", remainingSeconds);
    }
}

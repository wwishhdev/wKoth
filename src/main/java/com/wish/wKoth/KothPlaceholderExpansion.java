package com.wish.wKoth;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KothPlaceholderExpansion extends PlaceholderExpansion {

    private final wKoth plugin;

    public KothPlaceholderExpansion(wKoth plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "koth";
    }

    @Override
    public @NotNull String getAuthor() {
        return "wwishh";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.7";
    }

    @Override
    public boolean persist() {
        return true; // Persiste a través de recargas
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        // %koth_active%
        if (identifier.equals("active")) {
            return plugin.getActivePlaceholder();
        }

        // %koth_name%
        if (identifier.equals("name")) {
            return plugin.getActiveKothName();
        }

        // %koth_time%
        if (identifier.equals("time")) {
            return plugin.getActiveKothTime();
        }

        // %koth_capturing%
        if (identifier.equals("capturing")) {
            return plugin.getActiveKothCapturing();
        }

        // %koth_specific_time_[nombre]%
        if (identifier.startsWith("specific_time_")) {
            String kothName = identifier.substring("specific_time_".length());
            return plugin.getSpecificKothTime(kothName);
        }

        // %koth_specific_active_[nombre]%
        if (identifier.startsWith("specific_active_")) {
            String kothName = identifier.substring("specific_active_".length());
            return plugin.getSpecificKothActive(kothName);
        }

        // %koth_specific_capturing_[nombre]%
        if (identifier.startsWith("specific_capturing_")) {
            String kothName = identifier.substring("specific_capturing_".length());
            return plugin.getSpecificKothCapturing(kothName);
        }

        return null; // Placeholder desconocido
    }
}
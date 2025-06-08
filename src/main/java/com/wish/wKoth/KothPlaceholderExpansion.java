package com.wish.wKoth;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
        return "1.0.9";
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
            for (Map.Entry<String, Boolean> entry : plugin.activeKoths.entrySet()) {
                if (entry.getValue()) {
                    return entry.getKey();
                }
            }
            return ""; // Retornar cadena vacía si no hay KoTH activo
        }

        // %koth_time%
        if (identifier.equals("time")) {
            for (Map.Entry<String, Boolean> entry : plugin.activeKoths.entrySet()) {
                if (entry.getValue()) {
                    Integer time = plugin.kothTimers.get(entry.getKey());
                    if (time != null) {
                        return plugin.formatTime(time);
                    }
                }
            }
            return ""; // Retornar cadena vacía si no hay KoTH activo
        }

        // %koth_capturing%
        if (identifier.equals("capturing")) {
            for (Map.Entry<String, Boolean> entry : plugin.activeKoths.entrySet()) {
                if (entry.getValue()) {
                    Player capturingPlayer = plugin.capturingPlayers.get(entry.getKey());
                    if (capturingPlayer != null) {
                        return capturingPlayer.getName();
                    }
                }
            }
            return ""; // Retornar cadena vacía si no hay KoTH activo o nadie capturando
        }

        // %koth_specific_time_[nombre]%
        if (identifier.startsWith("specific_time_")) {
            String kothName = identifier.substring("specific_time_".length());
            if (plugin.activeKoths.containsKey(kothName) && plugin.activeKoths.get(kothName) && plugin.kothTimers.containsKey(kothName)) {
                return plugin.formatTime(plugin.kothTimers.get(kothName));
            }
            return ""; // Retornar cadena vacía si no está activo
        }

        // %koth_specific_active_[nombre]%
        if (identifier.startsWith("specific_active_")) {
            String kothName = identifier.substring("specific_active_".length());
            return plugin.activeKoths.containsKey(kothName) && plugin.activeKoths.get(kothName) ? "Sí" : "";
        }

        // %koth_specific_capturing_[nombre]%
        if (identifier.startsWith("specific_capturing_")) {
            String kothName = identifier.substring("specific_capturing_".length());
            if (plugin.activeKoths.containsKey(kothName) && plugin.activeKoths.get(kothName) && plugin.capturingPlayers.containsKey(kothName)) {
                Player capturingPlayer = plugin.capturingPlayers.get(kothName); // Cambiado player a capturingPlayer
                return capturingPlayer != null ? capturingPlayer.getName() : "";
            }
            return ""; // Retornar cadena vacía si no está activo o nadie capturando
        }

        return null; // Placeholder desconocido
    }
}
package com.wish.wKoth;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.temporal.ChronoUnit;

public class KothScheduler {
    private final wKoth plugin;
    private final Map<String, Map<DayOfWeek, LocalTime>> kothSchedules;
    private BukkitRunnable schedulerTask;

    public KothScheduler(wKoth plugin) {
        this.plugin = plugin;
        this.kothSchedules = new HashMap<>();
        loadSchedules();
        startScheduler();
    }

    public void loadSchedules() {
        kothSchedules.clear();
        ConfigurationSection kothsSection = plugin.getConfig().getConfigurationSection("koths");

        if (kothsSection == null) return;

        for (String kothName : kothsSection.getKeys(false)) {
            ConfigurationSection scheduleSection = kothsSection.getConfigurationSection(kothName + ".schedule");
            if (scheduleSection == null) continue;

            String timezone = scheduleSection.getString("timezone", "UTC");
            List<Map<?, ?>> times = scheduleSection.getMapList("times");

            Map<DayOfWeek, LocalTime> schedules = new HashMap<>();

            for (Map<?, ?> schedule : times) {
                try {
                    DayOfWeek day = DayOfWeek.valueOf(((String) schedule.get("day")).toUpperCase());
                    LocalTime time = LocalTime.parse((String) schedule.get("time"));
                    schedules.put(day, time);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error loading schedule for " + kothName + ": " + e.getMessage());
                }
            }

            kothSchedules.put(kothName, schedules);
        }
    }

    private void startScheduler() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
        }

        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkSchedules();
            }
        };

        // Revisar cada minuto
        schedulerTask.runTaskTimer(plugin, 0L, 20L * 60L);
    }

    private void checkSchedules() {
        for (Map.Entry<String, Map<DayOfWeek, LocalTime>> entry : kothSchedules.entrySet()) {
            String kothName = entry.getKey();
            Map<DayOfWeek, LocalTime> schedules = entry.getValue();

            // Obtener la zona horaria configurada para este KoTH
            String timezone = plugin.getConfig().getString("koths." + kothName + ".schedule.timezone", "UTC");
            ZoneId zoneId = ZoneId.of(timezone);

            // Obtener la hora actual en la zona horaria configurada
            LocalDateTime now = LocalDateTime.now(zoneId);
            DayOfWeek currentDay = now.getDayOfWeek();
            LocalTime currentTime = now.toLocalTime();

            // Verificar si hay un horario programado para este día y hora
            LocalTime scheduledTime = schedules.get(currentDay);
            if (scheduledTime != null) {
                // Si la hora actual coincide con la hora programada (con un margen de 1 minuto)
                if (Math.abs(ChronoUnit.MINUTES.between(currentTime, scheduledTime)) < 1) {
                    // Iniciar el KoTH si no está activo
                    if (!plugin.isKothActive(kothName)) {
                        plugin.startKoth(kothName);
                    }
                }
            }
        }
    }

    public void stop() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
        }
    }
}

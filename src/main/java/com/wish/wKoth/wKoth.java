package com.wish.wKoth;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class wKoth extends JavaPlugin implements Listener {
    private HashMap<UUID, Location> pos1 = new HashMap<>();
    private HashMap<UUID, Location> pos2 = new HashMap<>();
    public HashMap<String, Location[]> koths = new HashMap<>(); // Almacena los KoTHs por nombre
    private HashMap<UUID, String> creatingKoth = new HashMap<>(); // Almacena qué KoTH está creando cada jugador

    // Variables para el sistema de captura
    private HashMap<String, Boolean> activeKoths = new HashMap<>();
    private HashMap<String, Player> capturingPlayers = new HashMap<>();
    private HashMap<String, Integer> kothTimers = new HashMap<>();
    private HashMap<String, BukkitRunnable> kothTasks = new HashMap<>();
    private int CAPTURE_TIME = 300; // 5 minutos en segundos
    private HashMap<String, Integer> kothSpecificTimes = new HashMap<>();
    private HashMap<String, BukkitRunnable> playerCheckTasks = new HashMap<>();

    private KothScheduler kothScheduler;

    // Método para obtener mensajes formateados
    private String getMessage(String path) {
        String message = getConfig().getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.prefix", "") + message);
    }

    private String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }

    @Override
    public void onEnable() {
        // Guardar config por defecto
        saveDefaultConfig();

        // Registrar eventos
        getServer().getPluginManager().registerEvents(this, this);

        loadKothConfigurations();

        // Cargar KoTHs guardados
        loadSavedKoths();

        // Iniciar el scheduler
        kothScheduler = new KothScheduler(this);

        // Actualizar CAPTURE_TIME desde config (Corregido para usar la clave correcta)
        CAPTURE_TIME = getConfig().getInt("settings.default-capture-time", 300);

        // Mostrar el ASCII art
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "\n" +
                "██╗    ██╗██╗  ██╗ ██████╗ ████████╗██╗  ██╗\n" +
                "██║    ██║██║ ██╔╝██╔═══██╗╚══██╔══╝██║  ██║\n" +
                "██║ █╗ ██║█████╔╝ ██║   ██║   ██║   ███████║\n" +
                "██║███╗██║██╔═██╗ ██║   ██║   ██║   ██╔══██║\n" +
                "╚███╔███╔╝██║  ██╗╚██████╔╝   ██║   ██║  ██║\n" +
                " ╚══╝╚══╝ ╚═╝  ╚═╝ ╚═════╝    ╚═╝   ╚═╝  ╚═╝\n");
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "wKoth ha sido activado! by wwishh <3");
    }

    @Override
    public void onDisable() {
        // Detener el scheduler
        if (kothScheduler != null) {
            kothScheduler.stop();
        }

        // Detener todos los KoTHs activos
        for (String kothName : activeKoths.keySet()) {
            stopKoth(kothName);
        }
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "wKoth ha sido desactivado! by wwishh <3");
    }

    private void loadKothConfigurations() {
        kothSpecificTimes.clear();
        CAPTURE_TIME = getConfig().getInt("settings.default-capture-time", 300);

        // Cargar tiempos específicos de cada KoTH
        if (getConfig().contains("koths")) {
            for (String kothName : getConfig().getConfigurationSection("koths").getKeys(false)) {
                int time = getConfig().getInt("koths." + kothName + ".capture-time", CAPTURE_TIME);
                kothSpecificTimes.put(kothName, time);
            }
        }
    }

    public boolean isKothActive(String kothName) {
        return activeKoths.containsKey(kothName) && activeKoths.get(kothName);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("koth")) {
            if (!player.hasPermission("wkoth.admin")) {
                player.sendMessage(getMessage("no-permission"));
                return true;
            }

            if (args.length == 0) {
                player.sendMessage(getMessage("help-title"));
                player.sendMessage(getMessage("help-create"));
                player.sendMessage(getMessage("help-set"));
                player.sendMessage(getMessage("help-list"));
                player.sendMessage(getMessage("help-delete"));
                player.sendMessage(getMessage("help-start"));
                player.sendMessage(getMessage("help-stop"));
                player.sendMessage(getMessage("help-reload"));
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                loadKothConfigurations();
                if (kothScheduler != null) {
                    kothScheduler.loadSchedules();
                }
                player.sendMessage(getMessage("reload-success"));
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 2) {
                    player.sendMessage(getMessage("create-usage"));
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-already-exists"));
                    return true;
                }

                creatingKoth.put(player.getUniqueId(), kothName);
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.STICK));
                player.sendMessage(getMessage("selection-tool-given"));
                return true;
            }

            if (args[0].equalsIgnoreCase("set")) {
                if (!creatingKoth.containsKey(player.getUniqueId())) {
                    player.sendMessage(getMessage("must-create-first"));
                    return true;
                }

                if (!pos1.containsKey(player.getUniqueId()) || !pos2.containsKey(player.getUniqueId())) {
                    player.sendMessage(getMessage("must-select-positions"));
                    return true;
                }

                String kothName = creatingKoth.get(player.getUniqueId());
                Location[] locations = new Location[]{pos1.get(player.getUniqueId()), pos2.get(player.getUniqueId())};
                koths.put(kothName, locations);

                // Guardar en la configuración
                saveKothToConfig(kothName, locations);

                // Limpiar datos temporales
                pos1.remove(player.getUniqueId());
                pos2.remove(player.getUniqueId());
                creatingKoth.remove(player.getUniqueId());

                player.sendMessage(getMessage("koth-created", "%koth%", kothName));
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                if (koths.isEmpty()) {
                    player.sendMessage(getMessage("no-koths"));
                    return true;
                }

                player.sendMessage(getMessage("koth-list-header"));
                for (String kothName : koths.keySet()) {
                    Location[] locs = koths.get(kothName);
                    player.sendMessage(getMessage("koth-list-entry",
                            "%koth%", kothName,
                            "%world%", locs[0].getWorld().getName(),
                            "%pos1%", formatLocation(locs[0]),
                            "%pos2%", formatLocation(locs[1])));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("start")) {
                if (args.length < 2) {
                    player.sendMessage(getMessage("start-usage"));
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-not-exist"));
                    return true;
                }

                if (activeKoths.containsKey(kothName) && activeKoths.get(kothName)) {
                    player.sendMessage(getMessage("koth-already-active"));
                    return true;
                }

                startKoth(kothName);
                player.sendMessage(getMessage("koth-started-success", "%koth%", kothName));
                return true;
            }

            if (args[0].equalsIgnoreCase("stop")) {
                if (args.length < 2) {
                    player.sendMessage(getMessage("stop-usage"));
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!activeKoths.containsKey(kothName) || !activeKoths.get(kothName)) {
                    player.sendMessage(getMessage("koth-not-active"));
                    return true;
                }

                stopKoth(kothName);
                player.sendMessage(getMessage("koth-stopped-success", "%koth%", kothName));
                return true;
            }

            if (args[0].equalsIgnoreCase("delete")) {
                if (args.length < 2) {
                    player.sendMessage(getMessage("delete-usage"));
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-not-exist"));
                    return true;
                }

                // Eliminar de la configuración
                removeKothFromConfig(kothName);
                koths.remove(kothName);
                player.sendMessage(getMessage("koth-deleted", "%koth%", kothName));
                return true;
            }
        }
        return false;
    }

    private void saveKothToConfig(String kothName, Location[] locations) {
        ConfigurationSection kothSection = getConfig().getConfigurationSection("saved-koths");
        if (kothSection == null) {
            kothSection = getConfig().createSection("saved-koths");
        }

        ConfigurationSection specificKoth = kothSection.createSection(kothName);
        specificKoth.set("world", locations[0].getWorld().getName());
        specificKoth.set("pos1.x", locations[0].getX());
        specificKoth.set("pos1.y", locations[0].getY());
        specificKoth.set("pos1.z", locations[0].getZ());
        specificKoth.set("pos2.x", locations[1].getX());
        specificKoth.set("pos2.y", locations[1].getY());
        specificKoth.set("pos2.z", locations[1].getZ());

        saveConfig();
    }

    private void removeKothFromConfig(String kothName) {
        ConfigurationSection kothSection = getConfig().getConfigurationSection("saved-koths");
        if (kothSection != null && kothSection.contains(kothName)) {
            kothSection.set(kothName, null);
            saveConfig();
        }
    }

    private void loadSavedKoths() {
        ConfigurationSection kothSection = getConfig().getConfigurationSection("saved-koths");
        if (kothSection == null) return;

        for (String kothName : kothSection.getKeys(false)) {
            ConfigurationSection specificKoth = kothSection.getConfigurationSection(kothName);
            if (specificKoth == null) continue;

            String worldName = specificKoth.getString("world");
            org.bukkit.World world = getServer().getWorld(worldName);

            if (world == null) {
                getLogger().warning("No se pudo cargar el KoTH " + kothName + ": mundo no encontrado");
                continue;
            }

            Location pos1 = new Location(
                    world,
                    specificKoth.getDouble("pos1.x"),
                    specificKoth.getDouble("pos1.y"),
                    specificKoth.getDouble("pos1.z")
            );

            Location pos2 = new Location(
                    world,
                    specificKoth.getDouble("pos2.x"),
                    specificKoth.getDouble("pos2.y"),
                    specificKoth.getDouble("pos2.z")
            );

            koths.put(kothName, new Location[]{pos1, pos2});
            getLogger().info("KoTH " + kothName + " cargado desde la configuración");
        }
    }

    private String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }

    private void giveRewards(Player player, String kothName) {
        List<String> commands;

        // Intentar obtener recompensas específicas del KoTH
        if (getConfig().contains("koths." + kothName)) {  // Cambiado de "rewards." a "koths."
            commands = getConfig().getStringList("koths." + kothName + ".commands");
        } else {
            // Usar recompensas por defecto
            commands = getConfig().getStringList("koths.default.commands");
        }

        // Ejecutar comandos de recompensa
        if (commands != null && !commands.isEmpty()) {
            for (String command : commands) {
                command = command.replace("%player%", player.getName());
                getServer().dispatchCommand(getServer().getConsoleSender(),
                        ChatColor.translateAlternateColorCodes('&', command));
            }
        }
    }

    public void startKoth(String kothName) {
        activeKoths.put(kothName, true);
        // Usar tiempo específico del KoTH si existe
        int captureTime = kothSpecificTimes.getOrDefault(kothName, CAPTURE_TIME);
        kothTimers.put(kothName, captureTime);

        getServer().broadcastMessage(getMessage("koth-started",
                "%koth%", kothName,
                "%time%", formatTime(captureTime)));

        // Tarea principal del KoTH
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                int timeLeft = kothTimers.get(kothName);

                if (timeLeft <= 0) {
                    Player winner = capturingPlayers.get(kothName);
                    if (winner != null) {
                        getServer().broadcastMessage(getMessage("koth-winner",
                                "%player%", winner.getName(),
                                "%koth%", kothName));
                        giveRewards(winner, kothName);
                    }
                    stopKoth(kothName);
                    return;
                }

                // Usar el intervalo configurado para los anuncios
                int announcementInterval = getConfig().getInt("settings.announcement-interval", 30);
                if (timeLeft % announcementInterval == 0) {
                    getServer().broadcastMessage(getMessage("koth-time-left",
                            "%koth%", kothName,
                            "%time%", formatTime(timeLeft)));
                }

                kothTimers.put(kothName, timeLeft - 1);
            }
        };
        task.runTaskTimer(this, 20L, 20L);
        kothTasks.put(kothName, task);

        // Tarea para verificar continuamente si los jugadores están en la zona
        BukkitRunnable playerCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeKoths.containsKey(kothName) || !activeKoths.get(kothName)) {
                    this.cancel();
                    return;
                }

                Player capturingPlayer = capturingPlayers.get(kothName);
                if (capturingPlayer != null) {
                    // Verificar si el jugador sigue en el servidor y en la zona
                    if (!capturingPlayer.isOnline() || !isInKoth(capturingPlayer, kothName)) {
                        capturingPlayers.remove(kothName);
                        if (getConfig().getBoolean("settings.broadcast-capture", true)) {
                            getServer().broadcastMessage(getMessage("player-lost-control",
                                    "%player%", capturingPlayer.getName(),
                                    "%koth%", kothName));
                        }
                    }
                }
            }
        };
        playerCheckTask.runTaskTimer(this, 10L, 10L); // Verificar cada 0.5 segundos
        playerCheckTasks.put(kothName, playerCheckTask);
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void stopKoth(String kothName) {
        activeKoths.put(kothName, false);
        capturingPlayers.remove(kothName);
        kothTimers.remove(kothName);

        if (kothTasks.containsKey(kothName)) {
            kothTasks.get(kothName).cancel();
            kothTasks.remove(kothName);
        }

        if (playerCheckTasks.containsKey(kothName)) {
            playerCheckTasks.get(kothName).cancel();
            playerCheckTasks.remove(kothName);
        }

        getServer().broadcastMessage(getMessage("koth-ended", "%koth%", kothName));
    }

    private boolean isInKoth(Player player, String kothName) {
        if (!koths.containsKey(kothName)) return false;

        Location[] locs = koths.get(kothName);
        Location loc = player.getLocation();

        double minX = Math.min(locs[0].getX(), locs[1].getX());
        double minY = Math.min(locs[0].getY(), locs[1].getY());
        double minZ = Math.min(locs[0].getZ(), locs[1].getZ());
        double maxX = Math.max(locs[0].getX(), locs[1].getX());
        double maxY = Math.max(locs[0].getY(), locs[1].getY());
        double maxZ = Math.max(locs[0].getZ(), locs[1].getZ());

        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        for (String kothName : activeKoths.keySet()) {
            if (!activeKoths.get(kothName)) continue;

            boolean wasInKoth = capturingPlayers.containsKey(kothName) &&
                    capturingPlayers.get(kothName).equals(player);
            boolean isInKoth = isInKoth(player, kothName);

            if (isInKoth && !wasInKoth) {
                if (!capturingPlayers.containsKey(kothName) &&
                        getConfig().getBoolean("settings.broadcast-capture", true)) {
                    capturingPlayers.put(kothName, player);
                    getServer().broadcastMessage(getMessage("player-capturing",
                            "%player%", player.getName(),
                            "%koth%", kothName));
                }
            } else if (!isInKoth && wasInKoth) {
                capturingPlayers.remove(kothName);
                if (getConfig().getBoolean("settings.broadcast-capture", true)) {
                    getServer().broadcastMessage(getMessage("player-lost-control",
                            "%player%", player.getName(),
                            "%koth%", kothName));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("wkoth.admin")) return;

        if (player.getItemInHand() != null && player.getItemInHand().getType() == Material.STICK &&
                creatingKoth.containsKey(player.getUniqueId())) {

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Usar el bloque exacto seleccionado
                Location loc = event.getClickedBlock().getLocation().clone();
                // Ajustar para cubrir todo el bloque
                loc.setX(loc.getBlockX());
                loc.setY(loc.getBlockY());
                loc.setZ(loc.getBlockZ());
                pos1.put(player.getUniqueId(), loc);
                player.sendMessage(getMessage("position-selected-1"));
                event.setCancelled(true);
            } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                // Usar el bloque exacto seleccionado
                Location loc = event.getClickedBlock().getLocation().clone();
                // Ajustar para cubrir todo el bloque
                loc.setX(loc.getBlockX() + 1); // +1 para incluir el bloque completo
                loc.setY(loc.getBlockY() + 1);
                loc.setZ(loc.getBlockZ() + 1);
                pos2.put(player.getUniqueId(), loc);
                player.sendMessage(getMessage("position-selected-2"));
                event.setCancelled(true);
            }
        }
    }
}
package com.wish.wKoth;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

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
    private HashMap<String, Integer> kothSpecificDurations = new HashMap<>();
    private HashMap<String, BukkitRunnable> playerCheckTasks = new HashMap<>();
    private HashMap<String, BukkitRunnable> kothTimeoutTasks = new HashMap<>();
    private HashMap<UUID, String> viewingLoot = new HashMap<>(); // Para rastrear quién está viendo un loot

    // Variables para el sistema de recompensas
    private HashMap<String, Location> chestLocations = new HashMap<>();
    private HashMap<String, ItemStack[]> chestContents = new HashMap<>();
    private HashMap<String, UUID> lastChestOpeners = new HashMap<>();
    private HashMap<String, Boolean> useChestRewards = new HashMap<>();
    private HashMap<String, Boolean> useCommandRewards = new HashMap<>();
    private HashMap<UUID, String> settingChestLocation = new HashMap<>();
    private HashMap<UUID, String> editingChestLoot = new HashMap<>();

    private KothScheduler kothScheduler;

    // Constante para la zona horaria argentina
    private static final String ARGENTINA_TIMEZONE = "America/Argentina/Buenos_Aires";

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

        // Cargar configuraciones de cofres
        loadChestData();

        // Iniciar el scheduler
        kothScheduler = new KothScheduler(this);

        // Mostrar el ASCII art
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "\n" +
                "██╗    ██╗██╗  ██╗ ██████╗ ████████╗██╗  ██╗\n" +
                "██║    ██║██║ ██╔╝██╔═══██╗╚══██╔══╝██║  ██║\n" +
                "██║ █╗ ██║█████╔╝ ██║   ██║   ██║   ███████║\n" +
                "██║███╗██║██╔═██╗ ██║   ██║   ██║   ██╔══██║\n" +
                "╚███╔███╔╝██║  ██╗╚██████╔╝   ██║   ██║  ██║\n" +
                " ╚══╝╚══╝ ╚═╝  ╚═╝ ╚═════╝    ╚═╝   ╚═╝  ╚═╝\n");
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "wKoth v1.0.5 ha sido activado! by wwishh <3");
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

        // Guardar datos de cofres
        saveChestData();

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "wKoth ha sido desactivado! by wwishh <3");
    }

    private void loadChestData() {
        File file = new File(getDataFolder(), "chests.yml");
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String kothName : config.getKeys(false)) {
            // Cargar ubicación del cofre
            if (config.contains(kothName + ".location")) {
                String worldName = config.getString(kothName + ".location.world");
                double x = config.getDouble(kothName + ".location.x");
                double y = config.getDouble(kothName + ".location.y");
                double z = config.getDouble(kothName + ".location.z");

                if (getServer().getWorld(worldName) != null) {
                    Location loc = new Location(getServer().getWorld(worldName), x, y, z);
                    chestLocations.put(kothName, loc);
                }
            }

            // Cargar contenido del cofre
            if (config.contains(kothName + ".contents")) {
                @SuppressWarnings("unchecked")
                List<ItemStack> items = (List<ItemStack>) config.getList(kothName + ".contents");
                if (items != null) {
                    ItemStack[] contents = items.toArray(new ItemStack[0]);
                    chestContents.put(kothName, contents);
                }
            }

            // Cargar preferencia de sistemas de recompensas
            if (config.contains(kothName + ".useChestRewards")) {
                useChestRewards.put(kothName, config.getBoolean(kothName + ".useChestRewards",
                        getConfig().getBoolean("settings.chest-rewards-enabled", true)));
            } else {
                useChestRewards.put(kothName, getConfig().getBoolean("settings.chest-rewards-enabled", true));
            }

            if (config.contains(kothName + ".useCommandRewards")) {
                useCommandRewards.put(kothName, config.getBoolean(kothName + ".useCommandRewards",
                        getConfig().getBoolean("settings.command-rewards-enabled", true)));
            } else {
                useCommandRewards.put(kothName, getConfig().getBoolean("settings.command-rewards-enabled", true));
            }
        }
    }

    private void saveChestData() {
        File file = new File(getDataFolder(), "chests.yml");
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Location> entry : chestLocations.entrySet()) {
            String kothName = entry.getKey();
            Location loc = entry.getValue();

            config.set(kothName + ".location.world", loc.getWorld().getName());
            config.set(kothName + ".location.x", loc.getX());
            config.set(kothName + ".location.y", loc.getY());
            config.set(kothName + ".location.z", loc.getZ());

            if (chestContents.containsKey(kothName)) {
                config.set(kothName + ".contents", Arrays.asList(chestContents.get(kothName)));
            }

            config.set(kothName + ".useChestRewards", useChestRewards.getOrDefault(kothName,
                    getConfig().getBoolean("settings.chest-rewards-enabled", true)));
            config.set(kothName + ".useCommandRewards", useCommandRewards.getOrDefault(kothName,
                    getConfig().getBoolean("settings.command-rewards-enabled", true)));
        }

        try {
            config.save(file);
        } catch (IOException e) {
            getLogger().severe("No se pudieron guardar los datos de los cofres: " + e.getMessage());
        }
    }

    private void loadKothConfigurations() {
        kothSpecificTimes.clear();
        kothSpecificDurations.clear();
        CAPTURE_TIME = getConfig().getInt("settings.default-capture-time", 300);
        int defaultDuration = getConfig().getInt("settings.default-koth-duration", 900);

        // Cargar tiempos específicos de cada KoTH
        if (getConfig().contains("koths")) {
            for (String kothName : getConfig().getConfigurationSection("koths").getKeys(false)) {
                int time = getConfig().getInt("koths." + kothName + ".capture-time", CAPTURE_TIME);
                int duration = getConfig().getInt("koths." + kothName + ".duration", defaultDuration);
                boolean cmdRewards = getConfig().getBoolean("koths." + kothName + ".command-rewards",
                        getConfig().getBoolean("settings.command-rewards-enabled", true));
                boolean chestRewards = getConfig().getBoolean("koths." + kothName + ".chest-rewards",
                        getConfig().getBoolean("settings.chest-rewards-enabled", true));

                kothSpecificTimes.put(kothName, time);
                kothSpecificDurations.put(kothName, duration);
                useCommandRewards.put(kothName, cmdRewards);
                useChestRewards.put(kothName, chestRewards);

                getLogger().info("Cargando configuración para KoTH " + kothName + ": tiempo de captura = " +
                        time + ", duración = " + duration + ", comandos = " + cmdRewards + ", cofres = " + chestRewards);
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
                player.sendMessage(ChatColor.YELLOW + "/koth set-capture-time <nombre> <segundos> - Establece el tiempo de captura");
                player.sendMessage(ChatColor.YELLOW + "/koth set-duration <nombre> <segundos> - Establece la duración máxima del KoTH");
                player.sendMessage(ChatColor.YELLOW + "/koth set-commands <nombre> <comando> - Añade un comando de recompensa");
                player.sendMessage(ChatColor.YELLOW + "/koth list-commands <nombre> - Muestra los comandos de recompensa");
                player.sendMessage(ChatColor.YELLOW + "/koth remove-command <nombre> <índice> - Elimina un comando de recompensa");
                player.sendMessage(ChatColor.YELLOW + "/koth set-chest <nombre> - Define la ubicación del cofre de recompensas");
                player.sendMessage(ChatColor.YELLOW + "/koth set-loot <nombre> - Configura el loot del cofre");
                player.sendMessage(ChatColor.YELLOW + "/koth view-loot <nombre> - Ver el contenido del cofre (solo lectura)");
                player.sendMessage(ChatColor.YELLOW + "/koth set-schedule <nombre> <día> <hora> - Programa un KoTH (ej: lunes 20:30)");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                loadKothConfigurations();
                loadChestData();
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
                chestLocations.remove(kothName);
                chestContents.remove(kothName);
                useChestRewards.remove(kothName);
                useCommandRewards.remove(kothName);
                player.sendMessage(getMessage("koth-deleted", "%koth%", kothName));
                return true;
            }

            // Comando para establecer el tiempo de captura
            if (args[0].equalsIgnoreCase("set-capture-time")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth set-capture-time <nombre> <tiempo en segundos>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-not-exist"));
                    return true;
                }

                int time;
                try {
                    time = Integer.parseInt(args[2]);
                    if (time <= 0) {
                        player.sendMessage(ChatColor.RED + "El tiempo debe ser mayor que 0.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "El tiempo debe ser un número válido.");
                    return true;
                }

                // Actualizar en la configuración
                ConfigurationSection kothsSection = getConfig().getConfigurationSection("koths");
                if (kothsSection == null) {
                    kothsSection = getConfig().createSection("koths");
                }

                if (!kothsSection.contains(kothName)) {
                    kothsSection.createSection(kothName);
                }

                kothsSection.set(kothName + ".capture-time", time);
                saveConfig();

                // Actualizar la caché de tiempos específicos
                kothSpecificTimes.put(kothName, time);

                player.sendMessage(ChatColor.GREEN + "Tiempo de captura para " + kothName + " actualizado a " + time + " segundos.");
                return true;
            }

            // Comando para establecer la duración máxima del KoTH
            if (args[0].equalsIgnoreCase("set-duration")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth set-duration <nombre> <tiempo en segundos>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-not-exist"));
                    return true;
                }

                int time;
                try {
                    time = Integer.parseInt(args[2]);
                    if (time <= 0) {
                        player.sendMessage(ChatColor.RED + "El tiempo debe ser mayor que 0.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "El tiempo debe ser un número válido.");
                    return true;
                }

                // Actualizar en la configuración
                ConfigurationSection kothsSection = getConfig().getConfigurationSection("koths");
                if (kothsSection == null) {
                    kothsSection = getConfig().createSection("koths");
                }

                if (!kothsSection.contains(kothName)) {
                    kothsSection.createSection(kothName);
                }

                kothsSection.set(kothName + ".duration", time);
                saveConfig();

                // Actualizar la caché
                kothSpecificDurations.put(kothName, time);

                player.sendMessage(getMessage("koth-duration-set", "%koth%", kothName, "%time%", String.valueOf(time)));
                return true;
            }

            // Comando para añadir comandos de recompensa
            if (args[0].equalsIgnoreCase("set-commands")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth set-commands <nombre> <comando>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-not-exist"));
                    return true;
                }

                StringBuilder commandBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    commandBuilder.append(args[i]).append(" ");
                }
                String commandToAdd = commandBuilder.toString().trim();

                // Obtener la sección de configuración
                ConfigurationSection kothsSection = getConfig().getConfigurationSection("koths");
                if (kothsSection == null) {
                    kothsSection = getConfig().createSection("koths");
                }

                if (!kothsSection.contains(kothName)) {
                    kothsSection.createSection(kothName);
                }

                List<String> commands = kothsSection.getStringList(kothName + ".commands");
                if (commands == null) {
                    commands = new ArrayList<>();
                }

                commands.add(commandToAdd);
                kothsSection.set(kothName + ".commands", commands);
                saveConfig();

                player.sendMessage(ChatColor.GREEN + "Comando añadido a las recompensas de " + kothName);
                return true;
            }

            // Comando para listar comandos de recompensa
            if (args[0].equalsIgnoreCase("list-commands")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth list-commands <nombre>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-not-exist"));
                    return true;
                }

                List<String> commands = getConfig().getStringList("koths." + kothName + ".commands");

                player.sendMessage(ChatColor.GOLD + "Comandos de recompensa para " + kothName + ":");
                if (commands.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "No hay comandos configurados.");
                } else {
                    for (int i = 0; i < commands.size(); i++) {
                        player.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ": " + commands.get(i));
                    }
                }
                return true;
            }

            // Comando para eliminar un comando de recompensa
            if (args[0].equalsIgnoreCase("remove-command")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth remove-command <nombre> <índice>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-not-exist"));
                    return true;
                }

                int index;
                try {
                    index = Integer.parseInt(args[2]) - 1;
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "El índice debe ser un número válido.");
                    return true;
                }

                List<String> commands = getConfig().getStringList("koths." + kothName + ".commands");

                if (index < 0 || index >= commands.size()) {
                    player.sendMessage(ChatColor.RED + "Índice fuera de rango. Usa /koth list-commands para ver los índices.");
                    return true;
                }

                String removedCommand = commands.remove(index);
                getConfig().set("koths." + kothName + ".commands", commands);
                saveConfig();

                player.sendMessage(ChatColor.GREEN + "Comando eliminado: " + removedCommand);
                return true;
            }

            // Comando para establecer la ubicación del cofre
            if (args[0].equalsIgnoreCase("set-chest")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth set-chest <nombre>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-not-exist"));
                    return true;
                }

                settingChestLocation.put(player.getUniqueId(), kothName);
                player.sendMessage(ChatColor.YELLOW + "Haz click en un bloque para establecer la ubicación del cofre de recompensas para " + kothName);
                return true;
            }

            // Comando para establecer el loot del cofre
            if (args[0].equalsIgnoreCase("set-loot")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth set-loot <nombre>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-not-exist"));
                    return true;
                }

                // Abrir un inventario para que el admin coloque los items
                Inventory inv = getServer().createInventory(null, 27, "Loot para " + kothName);

                // Si ya hay loot guardado, cargarlo
                if (chestContents.containsKey(kothName)) {
                    inv.setContents(chestContents.get(kothName));
                }

                player.openInventory(inv);
                editingChestLoot.put(player.getUniqueId(), kothName);

                return true;
            }

            // Comando para ver el loot del cofre (solo lectura)
            if (args[0].equalsIgnoreCase("view-loot")) {
                // Este comando puede ser usado por cualquier jugador, no necesita verificación de permiso wkoth.admin
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth view-loot <nombre>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-not-exist"));
                    return true;
                }

                // Abrir un inventario para ver los items (solo lectura)
                Inventory inv = getServer().createInventory(null, 27, "Loot de " + kothName + " (Solo vista)");

                if (chestContents.containsKey(kothName)) {
                    inv.setContents(chestContents.get(kothName));
                    // Registrar que el jugador está viendo este loot (para bloquear interacciones)
                    viewingLoot.put(player.getUniqueId(), kothName);
                } else {
                    player.sendMessage(ChatColor.RED + "No hay loot configurado para este KoTH.");
                    return true;
                }

                player.openInventory(inv);
                return true;
            }

            // Comando para programar un horario de KoTH
            if (args[0].equalsIgnoreCase("set-schedule")) {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth set-schedule <nombre> <día> <hora>");
                    player.sendMessage(ChatColor.YELLOW + "Ejemplo: /koth set-schedule evento lunes 20:30");
                    player.sendMessage(ChatColor.YELLOW + "Días válidos: lunes, martes, miércoles, jueves, viernes, sábado, domingo");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(getMessage("koth-not-exist"));
                    return true;
                }

                // Convertir el día a inglés (para DayOfWeek enum)
                String dayInput = args[2].toLowerCase();
                DayOfWeek day;

                switch (dayInput) {
                    case "lunes": day = DayOfWeek.MONDAY; break;
                    case "martes": day = DayOfWeek.TUESDAY; break;
                    case "miércoles":
                    case "miercoles": day = DayOfWeek.WEDNESDAY; break;
                    case "jueves": day = DayOfWeek.THURSDAY; break;
                    case "viernes": day = DayOfWeek.FRIDAY; break;
                    case "sábado":
                    case "sabado": day = DayOfWeek.SATURDAY; break;
                    case "domingo": day = DayOfWeek.SUNDAY; break;
                    default:
                        player.sendMessage(ChatColor.RED + "Día inválido. Use: lunes, martes, miércoles, jueves, viernes, sábado o domingo");
                        return true;
                }

                // Validar el formato de hora (HH:MM)
                String timeInput = args[3];
                LocalTime time;
                try {
                    time = LocalTime.parse(timeInput, DateTimeFormatter.ofPattern("H:mm"));
                } catch (DateTimeParseException e) {
                    player.sendMessage(ChatColor.RED + "Formato de hora inválido. Use HH:MM (ejemplo: 20:30)");
                    return true;
                }

                // Obtener/crear la sección de configuración
                ConfigurationSection kothsSection = getConfig().getConfigurationSection("koths");
                if (kothsSection == null) {
                    kothsSection = getConfig().createSection("koths");
                }

                if (!kothsSection.contains(kothName)) {
                    kothsSection.createSection(kothName);
                }

                // Crear o actualizar la sección de schedule
                ConfigurationSection scheduleSection = kothsSection.getConfigurationSection(kothName + ".schedule");
                if (scheduleSection == null) {
                    scheduleSection = kothsSection.createSection(kothName + ".schedule");
                }

                // Establecer la zona horaria de Argentina
                scheduleSection.set("timezone", ARGENTINA_TIMEZONE);

                // Obtener las programaciones existentes o crear una nueva lista
                List<Map<String, Object>> times = new ArrayList<>();
                if (scheduleSection.contains("times")) {
                    @SuppressWarnings("unchecked")
                    List<Map<?, ?>> existingTimes = scheduleSection.getMapList("times");

                    // Convertir la lista existente
                    for (Map<?, ?> existingMap : existingTimes) {
                        Map<String, Object> convertedMap = new HashMap<>();
                        for (Map.Entry<?, ?> entry : existingMap.entrySet()) {
                            convertedMap.put(entry.getKey().toString(), entry.getValue());
                        }
                        times.add(convertedMap);
                    }

                    // Eliminar horarios existentes para el mismo día
                    times.removeIf(map -> day.name().equalsIgnoreCase((String) map.get("day")));
                }

                // Añadir el nuevo horario
                Map<String, Object> newSchedule = new HashMap<>();
                newSchedule.put("day", day.name());
                newSchedule.put("time", time.format(DateTimeFormatter.ofPattern("HH:mm")));
                times.add(newSchedule);

                // Guardar la configuración actualizada
                scheduleSection.set("times", times);
                saveConfig();

                // Recargar schedules
                if (kothScheduler != null) {
                    kothScheduler.loadSchedules();
                }

                String formattedDay = dayInput.substring(0, 1).toUpperCase() + dayInput.substring(1).toLowerCase();
                player.sendMessage(ChatColor.GREEN + "Horario programado para el KoTH " + kothName + ": " +
                        formattedDay + " a las " + time.format(DateTimeFormatter.ofPattern("HH:mm")) +
                        " (Zona horaria: Argentina)");

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

        // Crear configuración en koths con valores predeterminados
        ConfigurationSection kothsSection = getConfig().getConfigurationSection("koths");
        if (kothsSection == null) {
            kothsSection = getConfig().createSection("koths");
        }

        if (!kothsSection.contains(kothName)) {
            ConfigurationSection newKothConfig = kothsSection.createSection(kothName);
            // Usar los valores predeterminados de "default" o valores generales
            newKothConfig.set("capture-time", getConfig().getInt("settings.default-capture-time", 300));
            newKothConfig.set("duration", getConfig().getInt("settings.default-koth-duration", 900));
            newKothConfig.set("command-rewards", getConfig().getBoolean("settings.command-rewards-enabled", true));
            newKothConfig.set("chest-rewards", getConfig().getBoolean("settings.chest-rewards-enabled", true));

            // Copiar comandos predeterminados si están disponibles
            if (kothsSection.contains("default") && kothsSection.isConfigurationSection("default")) {
                List<String> defaultCommands = kothsSection.getStringList("default.commands");
                if (defaultCommands != null && !defaultCommands.isEmpty()) {
                    newKothConfig.set("commands", defaultCommands);
                }
            } else {
                // Comandos predeterminados básicos
                List<String> defaultCommands = new ArrayList<>();
                defaultCommands.add("eco give %player% 1000");
                defaultCommands.add("broadcast &a¡%player% ha ganado el KoTH " + kothName + "!");
                newKothConfig.set("commands", defaultCommands);
            }

            // Configurar horario básico
            ConfigurationSection scheduleSection = newKothConfig.createSection("schedule");
            scheduleSection.set("timezone", ARGENTINA_TIMEZONE);
        }

        saveConfig();

        // Actualizar la caché de tiempos específicos
        loadKothConfigurations();
    }

    private void removeKothFromConfig(String kothName) {
        // Eliminar de saved-koths
        ConfigurationSection kothSection = getConfig().getConfigurationSection("saved-koths");
        if (kothSection != null && kothSection.contains(kothName)) {
            kothSection.set(kothName, null);
        }

        // Eliminar de koths
        ConfigurationSection kothsSection = getConfig().getConfigurationSection("koths");
        if (kothsSection != null && kothsSection.contains(kothName)) {
            kothsSection.set(kothName, null);
        }

        saveConfig();
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
        boolean commandEnabled = useCommandRewards.getOrDefault(kothName,
                getConfig().getBoolean("settings.command-rewards-enabled", true));
        boolean chestEnabled = useChestRewards.getOrDefault(kothName,
                getConfig().getBoolean("settings.chest-rewards-enabled", true));

        // Sistema de comandos
        if (commandEnabled) {
            List<String> commands;

            // Intentar obtener recompensas específicas del KoTH
            if (getConfig().contains("koths." + kothName)) {
                commands = getConfig().getStringList("koths." + kothName + ".commands");
            } else {
                // Usar recompensas por defecto
                commands = getConfig().getStringList("koths.default.commands");
            }

            // Ejecutar comandos de recompensa
            if (commands != null && !commands.isEmpty()) {
                for (String cmd : commands) {
                    cmd = cmd.replace("%player%", player.getName());
                    getServer().dispatchCommand(getServer().getConsoleSender(),
                            ChatColor.translateAlternateColorCodes('&', cmd));
                }
            }
        }

        // Sistema de cofres
        if (chestEnabled && chestLocations.containsKey(kothName)) {
            spawnRewardChest(player, kothName);
        }
    }

    private void spawnRewardChest(Player player, String kothName) {
        if (!chestLocations.containsKey(kothName)) {
            getLogger().warning("No hay ubicación de cofre definida para " + kothName);
            return;
        }

        Location chestLoc = chestLocations.get(kothName);
        Block block = chestLoc.getBlock();

        // Guardar el material original para restaurarlo después
        final Material originalMaterial = block.getType();

        // Colocar el cofre
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            Inventory inv = chest.getInventory();

            // Poner el loot en el cofre
            if (chestContents.containsKey(kothName)) {
                inv.setContents(chestContents.get(kothName));
            }

            // Guardar el jugador que puede abrir el cofre
            lastChestOpeners.put(kothName, player.getUniqueId());

            // Mensaje al jugador
            int chestDuration = getConfig().getInt("settings.chest-duration", 60);
            player.sendMessage(getMessage("chest-reward-spawned",
                    "%time%", String.valueOf(chestDuration)));

            // Programar la eliminación del cofre después de un tiempo
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (block.getType() == Material.CHEST) {
                        block.setType(originalMaterial);
                    }
                    lastChestOpeners.remove(kothName);
                }
            }.runTaskLater(this, 20L * chestDuration);
        }
    }

    public void startKoth(String kothName) {
        activeKoths.put(kothName, true);
        // Usar tiempo específico del KoTH si existe
        int captureTime = kothSpecificTimes.getOrDefault(kothName, CAPTURE_TIME);
        int kothDuration = kothSpecificDurations.getOrDefault(kothName,
                getConfig().getInt("settings.default-koth-duration", 900));

        // Log para depuración
        getLogger().info("Iniciando KoTH " + kothName + " con tiempo de captura: " +
                captureTime + " segundos, duración: " + kothDuration + " segundos");

        kothTimers.put(kothName, captureTime);

        getServer().broadcastMessage(getMessage("koth-started",
                "%koth%", kothName,
                "%time%", formatTime(captureTime),
                "%duration%", formatTime(kothDuration)));

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

        // Añadir un nuevo temporizador para la finalización por tiempo límite
        BukkitRunnable timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Verificar si el KoTH sigue activo
                if (activeKoths.containsKey(kothName) && activeKoths.get(kothName)) {
                    // Finalizar el KoTH por tiempo límite
                    getServer().broadcastMessage(getMessage("koth-timeout", "%koth%", kothName));
                    stopKoth(kothName);
                }
            }
        };
        timeoutTask.runTaskLater(this, 20L * kothDuration);
        kothTimeoutTasks.put(kothName, timeoutTask);
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

        // Cancelar el temporizador de finalización
        if (kothTimeoutTasks.containsKey(kothName)) {
            kothTimeoutTasks.get(kothName).cancel();
            kothTimeoutTasks.remove(kothName);
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

    // Métodos para placeholders
    public String getActivePlaceholder() {
        for (Map.Entry<String, Boolean> entry : activeKoths.entrySet()) {
            if (entry.getValue()) {
                return "Sí";
            }
        }
        return "No";
    }

    public String getActiveKothName() {
        for (Map.Entry<String, Boolean> entry : activeKoths.entrySet()) {
            if (entry.getValue()) {
                return entry.getKey();
            }
        }
        return "Ninguno";
    }

    public String getActiveKothTime() {
        for (Map.Entry<String, Boolean> entry : activeKoths.entrySet()) {
            if (entry.getValue()) {
                Integer time = kothTimers.get(entry.getKey());
                if (time != null) {
                    return formatTime(time);
                }
            }
        }
        return "N/A";
    }

    public String getActiveKothCapturing() {
        for (Map.Entry<String, Boolean> entry : activeKoths.entrySet()) {
            if (entry.getValue()) {
                Player player = capturingPlayers.get(entry.getKey());
                if (player != null) {
                    return player.getName();
                }
            }
        }
        return "Nadie";
    }

    public String getSpecificKothTime(String kothName) {
        if (activeKoths.containsKey(kothName) && activeKoths.get(kothName) && kothTimers.containsKey(kothName)) {
            return formatTime(kothTimers.get(kothName));
        }
        return "N/A";
    }

    public String getSpecificKothActive(String kothName) {
        return activeKoths.containsKey(kothName) && activeKoths.get(kothName) ? "Sí" : "No";
    }

    public String getSpecificKothCapturing(String kothName) {
        if (activeKoths.containsKey(kothName) && activeKoths.get(kothName) && capturingPlayers.containsKey(kothName)) {
            Player player = capturingPlayers.get(kothName);
            return player != null ? player.getName() : "Nadie";
        }
        return "Nadie";
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Verificar si el jugador está viendo un loot en modo visualización
        if (viewingLoot.containsKey(player.getUniqueId())) {
            // Cancelar cualquier interacción con el inventario
            event.setCancelled(true);
        }
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

        // Para selección de KoTH
        if (player.hasPermission("wkoth.admin") && player.getItemInHand() != null &&
                player.getItemInHand().getType() == Material.STICK &&
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

        // Para seleccionar ubicación del cofre
        if (player.hasPermission("wkoth.chest.set") && settingChestLocation.containsKey(player.getUniqueId())) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                String kothName = settingChestLocation.get(player.getUniqueId());
                Location loc = event.getClickedBlock().getLocation().clone();

                chestLocations.put(kothName, loc);
                saveChestData();

                player.sendMessage(ChatColor.GREEN + "Ubicación del cofre establecida para " + kothName);
                settingChestLocation.remove(player.getUniqueId());
                event.setCancelled(true);
            }
        }

        // Controlar la apertura de cofres de recompensa
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.CHEST) {
            // Verificar si es un cofre de recompensa
            for (String kothName : lastChestOpeners.keySet()) {
                if (chestLocations.containsKey(kothName) &&
                        chestLocations.get(kothName).equals(event.getClickedBlock().getLocation())) {

                    // Solo el ganador del KoTH puede abrirlo
                    if (!player.getUniqueId().equals(lastChestOpeners.get(kothName))) {
                        player.sendMessage(ChatColor.RED + "Este cofre solo puede ser abierto por el ganador del KoTH.");
                        event.setCancelled(true);
                    }
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();

        // Guardar el loot cuando el admin cierra el inventario de edición
        if (editingChestLoot.containsKey(player.getUniqueId())) {
            String kothName = editingChestLoot.get(player.getUniqueId());
            ItemStack[] contents = event.getInventory().getContents();

            chestContents.put(kothName, contents);
            saveChestData();

            player.sendMessage(ChatColor.GREEN + "Loot guardado para el KoTH " + kothName);
            editingChestLoot.remove(player.getUniqueId());
        }
    }
}
package com.wish.wKoth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import java.io.*;

public class wKoth extends JavaPlugin implements Listener {
    private final HashMap<UUID, Integer> playerTime = new HashMap<>();
    private HashMap<String, KothArena> koths;

    @Override
    public void onEnable() {
        // Mostrar el ASCII art
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "\n" +
                "██╗    ██╗██╗  ██╗ ██████╗ ████████╗██╗  ██╗\n" +
                "██║    ██║██║ ██╔╝██╔═══██╗╚══██╔══╝██║  ██║\n" +
                "██║ █╗ ██║█████╔╝ ██║   ██║   ██║   ███████║\n" +
                "██║███╗██║██╔═██╗ ██║   ██║   ██║   ██╔══██║\n" +
                "╚███╔███╔╝██║  ██╗╚██████╔╝   ██║   ██║  ██║\n" +
                " ╚══╝╚══╝ ╚═╝  ╚═╝ ╚═════╝    ╚═╝   ╚═╝  ╚═╝\n");
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "wKoth ha sido activado! by wwishh <3");

        // Inicialización del plugin
        koths = new HashMap<>();
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        loadKoths();
    }

    @Override
    public void onDisable() {
        // Detener todos los KoTHs activos
        for (KothArena arena : koths.values()) {
            if (arena.isActive()) {
                stopKoth(arena.getId());
            }
        }

        // Guardar configuración
        saveKoths();
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "wKoth ha sido desactivado! by wwishh <3");
    }

    private void loadKoths() {
        if (!getConfig().contains("koths")) return;

        for (String kothId : getConfig().getConfigurationSection("koths").getKeys(false)) {
            String name = getConfig().getString("koths." + kothId + ".name");
            int duration = getConfig().getInt("koths." + kothId + ".duration");

            KothArena arena = new KothArena(kothId, name, duration);

            // Cargar pos1
            if (getConfig().contains("koths." + kothId + ".pos1")) {
                try {
                    if (getConfig().get("koths." + kothId + ".pos1") instanceof Location) {
                        arena.setPos1((Location) getConfig().get("koths." + kothId + ".pos1"));
                    } else {
                        String locationString = getConfig().getString("koths." + kothId + ".pos1");
                        Location location = deserializeLocation(locationString);
                        if (location != null) {
                            arena.setPos1(location);
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Error al cargar pos1 para el KoTH " + kothId + ": " + e.getMessage());
                }
            }

            // Cargar pos2
            if (getConfig().contains("koths." + kothId + ".pos2")) {
                try {
                    if (getConfig().get("koths." + kothId + ".pos2") instanceof Location) {
                        arena.setPos2((Location) getConfig().get("koths." + kothId + ".pos2"));
                    } else {
                        String locationString = getConfig().getString("koths." + kothId + ".pos2");
                        Location location = deserializeLocation(locationString);
                        if (location != null) {
                            arena.setPos2(location);
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Error al cargar pos2 para el KoTH " + kothId + ": " + e.getMessage());
                }
            }

            koths.put(kothId, arena);
        }
    }

    private Location deserializeLocation(String locationString) {
        try {
            if (locationString == null || locationString.trim().isEmpty()) return null;

            // Extraer los valores del string de ubicación
            String worldName = locationString.substring(locationString.indexOf("world=") + 6, locationString.indexOf(","));
            worldName = worldName.substring(worldName.indexOf("name=") + 5, worldName.indexOf("}"));

            double x = Double.parseDouble(locationString.substring(locationString.indexOf("x=") + 2, locationString.indexOf(",y=")));
            double y = Double.parseDouble(locationString.substring(locationString.indexOf("y=") + 2, locationString.indexOf(",z=")));
            double z = Double.parseDouble(locationString.substring(locationString.indexOf("z=") + 2, locationString.indexOf(",pitch=")));
            float pitch = Float.parseFloat(locationString.substring(locationString.indexOf("pitch=") + 6, locationString.indexOf(",yaw=")));
            float yaw = Float.parseFloat(locationString.substring(locationString.indexOf("yaw=") + 4, locationString.indexOf("}")));

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                getLogger().warning("No se pudo encontrar el mundo: " + worldName);
                return null;
            }

            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            getLogger().warning("Error al deserializar ubicación: " + e.getMessage());
            return null;
        }
    }

    private void saveKoths() {
        for (Map.Entry<String, KothArena> entry : koths.entrySet()) {
            String path = "koths." + entry.getKey() + ".";
            KothArena arena = entry.getValue();

            getConfig().set(path + "name", arena.getName());
            getConfig().set(path + "duration", arena.getDuration());
            getConfig().set(path + "pos1", arena.getPos1());
            getConfig().set(path + "pos2", arena.getPos2());
        }
        saveConfigWithComments();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores!");
            return true;
        }

        Player player = (Player) sender;

        if (!command.getName().equalsIgnoreCase("koth")) {
            return false;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth create <nombre>");
                    return true;
                }
                createKoth(player, args[1]);
                break;
            case "delete":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth delete <nombre>");
                    return true;
                }
                deleteKoth(player, args[1]);
                break;
            case "list":
                listKoths(player);
                break;
            case "start":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth start <nombre>");
                    return true;
                }
                startKoth(args[1]);
                break;
            case "stop":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth stop <nombre>");
                    return true;
                }
                stopKoth(args[1]);
                break;
            case "setpos1":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth setpos1 <nombre>");
                    return true;
                }
                setPos1(player, args[1]);
                break;
            case "setpos2":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /koth setpos2 <nombre>");
                    return true;
                }
                setPos2(player, args[1]);
                break;
            case "reload":
                reloadConfiguration(player);
                break;
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    // Métodos para gestionar KoTHs
    private void createKoth(Player player, String id) {
        if (koths.containsKey(id)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("messages.koth-already-exists").replace("%koth%", id)));
            return;
        }

        // Crear el KoTH con la configuración por defecto
        getConfig().set("koths." + id + ".name", id);
        getConfig().set("koths." + id + ".duration", getConfig().getInt("settings.duration", 900));
        getConfig().set("koths." + id + ".rewards.commands", getConfig().getStringList("koths.example.rewards.commands"));

        // Crear la arena
        KothArena arena = new KothArena(id, id, getConfig().getInt("settings.duration", 900));
        koths.put(id, arena);

        // Guardar la configuración manteniendo el formato
        saveConfigWithComments();

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.koth-created").replace("%koth%", id)));
    }

    private void deleteKoth(Player player, String id) {
        if (!koths.containsKey(id)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("messages.koth-not-found").replace("%koth%", id)));
            return;
        }

        KothArena arena = koths.get(id);
        if (arena.isActive()) {
            stopKoth(id);
        }

        koths.remove(id);
        getConfig().set("koths." + id, null);
        saveConfig();

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.koth-deleted").replace("%koth%", id)));
    }

    private void listKoths(Player player) {
        StringBuilder kothList = new StringBuilder();
        for (KothArena arena : koths.values()) {
            kothList.append(arena.getName()).append(arena.isActive() ? " &a(Activo)" : " &c(Inactivo)").append("&7, ");
        }

        String list = kothList.length() > 0 ? kothList.substring(0, kothList.length() - 2) : "Ninguno";
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.koth-list").replace("%koths%", list)));
    }

    private void setPos1(Player player, String id) {
        KothArena arena = koths.get(id);
        if (arena == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("messages.koth-not-found").replace("%koth%", id)));
            return;
        }

        arena.setPos1(player.getLocation());
        saveKoths();

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.pos1-set").replace("%koth%", id)));
    }

    private void setPos2(Player player, String id) {
        KothArena arena = koths.get(id);
        if (arena == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("messages.koth-not-found").replace("%koth%", id)));
            return;
        }

        arena.setPos2(player.getLocation());
        saveKoths();

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.pos2-set").replace("%koth%", id)));
    }

    private void reloadConfiguration(Player player) {
        // Detener todos los KoTHs activos
        for (KothArena arena : koths.values()) {
            if (arena.isActive()) {
                stopKoth(arena.getId());
            }
        }

        // Guardar la configuración actual con comentarios
        saveConfigWithComments();

        // Recargar la configuración
        reloadConfig();

        // Recargar los KoTHs
        koths.clear();
        loadKoths();

        // Enviar mensaje de confirmación
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.config-reloaded")));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();

        for (KothArena arena : koths.values()) {
            if (!arena.isActive()) continue;

            if (arena.isInside(playerLoc)) {
                if (arena.getCurrentKingPlayer() == null || !arena.getCurrentKingPlayer().equals(player)) {
                    arena.setCurrentKingPlayer(player);
                    arena.setCaptureTime(0);
                    broadcastKothMessage(ChatColor.translateAlternateColorCodes('&',
                            getConfig().getString("messages.koth-control")
                                    .replace("%player%", player.getName())
                                    .replace("%koth%", arena.getName())
                                    .replace("%time%", "0")));
                }
            } else if (arena.getCurrentKingPlayer() != null && arena.getCurrentKingPlayer().equals(player)) {
                arena.setCurrentKingPlayer(null);
                arena.setCaptureTime(0);
            }
        }
    }

    private void startKoth(String id) {
        KothArena arena = koths.get(id);
        if (arena == null) {
            broadcastKothMessage(getConfig().getString("messages.koth-not-found").replace("%koth%", id));
            return;
        }

        if (arena.isActive()) return;
        if (!arena.isConfigured()) {
            broadcastKothMessage(ChatColor.RED + "¡El KoTH " + id + " no está configurado correctamente!");
            return;
        }

        arena.setActive(true);
        broadcastKothMessage(getConfig().getString("messages.koth-start").replace("%koth%", arena.getName()));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arena.isActive()) {
                    this.cancel();
                    return;
                }

                if (arena.getCurrentKingPlayer() != null) {
                    arena.setCaptureTime(arena.getCaptureTime() + 1);
                    if (arena.getCaptureTime() >= arena.getDuration()) {
                        playerWon(arena.getCurrentKingPlayer(), arena);
                        stopKoth(id);
                        this.cancel();
                    } else if (arena.getCaptureTime() % getConfig().getInt("settings.broadcast-interval") == 0) {
                        broadcastKothMessage(ChatColor.translateAlternateColorCodes('&',
                                getConfig().getString("messages.koth-control")
                                        .replace("%player%", arena.getCurrentKingPlayer().getName())
                                        .replace("%koth%", arena.getName())
                                        .replace("%time%", String.valueOf(arena.getCaptureTime()))));
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void stopKoth(String id) {
        KothArena arena = koths.get(id);
        if (arena == null || !arena.isActive()) return;

        arena.setActive(false);
        arena.setCurrentKingPlayer(null);
        arena.setCaptureTime(0);
        broadcastKothMessage(getConfig().getString("messages.koth-end").replace("%koth%", arena.getName()));
    }

    private void playerWon(Player player, KothArena arena) {
        broadcastKothMessage(getConfig().getString("messages.koth-captured")
                .replace("%player%", player.getName())
                .replace("%koth%", arena.getName()));

        // Verificar si existen recompensas para este KoTH
        String path = "koths." + arena.getId();
        if (getConfig().contains(path + ".rewards.commands")) {
            for (String command : getConfig().getStringList(path + ".rewards.commands")) {
                getServer().dispatchCommand(getServer().getConsoleSender(),
                        command.replace("%player%", player.getName()));
            }
        }
    }

    private void saveConfigWithComments() {
        try {
            // Guardar la configuración actual
            File configFile = new File(getDataFolder(), "config.yml");

            // Crear una nueva configuración YAML
            YamlConfiguration config = new YamlConfiguration();

            // Copiar todos los valores de la configuración actual
            for (String key : getConfig().getKeys(true)) {
                config.set(key, getConfig().get(key));
            }

            // Cargar el template con comentarios
            InputStream defaultStream = getResource("config.yml");
            if (defaultStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                        new FileOutputStream(configFile), StandardCharsets.UTF_8));

                // Escribir el header con comentarios
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#") || line.trim().isEmpty()) {
                        writer.println(line);
                    } else {
                        break;
                    }
                }
                reader.close();

                // Escribir la configuración actual
                writer.println("settings:");
                writer.println("  cooldown: " + config.getInt("settings.cooldown", 3600));
                writer.println("  broadcast-interval: " + config.getInt("settings.broadcast-interval", 60));
                writer.println();

                // Escribir mensajes
                writer.println("messages:");
                if (config.getConfigurationSection("messages") != null) {
                    for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                        writer.println("  " + key + ": '" + config.getString("messages." + key) + "'");
                    }
                }
                writer.println();

                // Escribir KoTHs
                writer.println("koths:");
                if (config.getConfigurationSection("koths") != null) {
                    for (String kothId : config.getConfigurationSection("koths").getKeys(false)) {
                        writer.println("  " + kothId + ":");
                        writer.println("    name: \"" + config.getString("koths." + kothId + ".name") + "\"");
                        writer.println("    duration: " + config.getInt("koths." + kothId + ".duration"));

                        // Escribir posiciones si existen
                        if (config.contains("koths." + kothId + ".pos1")) {
                            writer.println("    pos1: " + config.get("koths." + kothId + ".pos1"));
                        }
                        if (config.contains("koths." + kothId + ".pos2")) {
                            writer.println("    pos2: " + config.get("koths." + kothId + ".pos2"));
                        }

                        // Escribir recompensas
                        if (config.contains("koths." + kothId + ".rewards.commands")) {
                            writer.println("    rewards:");
                            writer.println("      commands:");
                            for (String command : config.getStringList("koths." + kothId + ".rewards.commands")) {
                                writer.println("        - \"" + command + "\"");
                            }
                        }
                    }
                }

                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastKothMessage(String message) {
        getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.prefix") + message));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== KoTH Commands ===");
        if (player.hasPermission("wkoth.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/koth create <nombre> " + ChatColor.GRAY + "- Crea un nuevo KoTH");
            player.sendMessage(ChatColor.YELLOW + "/koth delete <nombre> " + ChatColor.GRAY + "- Elimina un KoTH");
            player.sendMessage(ChatColor.YELLOW + "/koth start <nombre> " + ChatColor.GRAY + "- Inicia un KoTH");
            player.sendMessage(ChatColor.YELLOW + "/koth stop <nombre> " + ChatColor.GRAY + "- Detiene un KoTH");
            player.sendMessage(ChatColor.YELLOW + "/koth setpos1 <nombre> " + ChatColor.GRAY + "- Establece la primera posición");
            player.sendMessage(ChatColor.YELLOW + "/koth setpos2 <nombre> " + ChatColor.GRAY + "- Establece la segunda posición");
            player.sendMessage(ChatColor.YELLOW + "/koth list " + ChatColor.GRAY + "- Lista todos los KoTHs");
            player.sendMessage(ChatColor.YELLOW + "/koth reload " + ChatColor.GRAY + "- Recarga la configuración");
        }
        player.sendMessage(ChatColor.GOLD + "==================");
    }
}

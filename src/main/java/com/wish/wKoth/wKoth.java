package com.wish.wKoth;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
    private HashMap<String, Location[]> koths = new HashMap<>(); // Almacena los KoTHs por nombre
    private HashMap<UUID, String> creatingKoth = new HashMap<>(); // Almacena qué KoTH está creando cada jugador

    // Variables para el sistema de captura
    private HashMap<String, Boolean> activeKoths = new HashMap<>();
    private HashMap<String, Player> capturingPlayers = new HashMap<>();
    private HashMap<String, Integer> kothTimers = new HashMap<>();
    private HashMap<String, BukkitRunnable> kothTasks = new HashMap<>();
    private int CAPTURE_TIME = 300; // 5 minutos en segundos

    @Override
    public void onEnable() {
        // Guardar config por defecto
        saveDefaultConfig();

        // Registrar eventos
        getServer().getPluginManager().registerEvents(this, this);

        // Actualizar CAPTURE_TIME desde config
        CAPTURE_TIME = getConfig().getInt("settings.capture-time", 300);

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
        // Detener todos los KoTHs activos
        for (String kothName : activeKoths.keySet()) {
            stopKoth(kothName);
        }
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "wKoth ha sido desactivado! by wwishh <3");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por jugadores!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("koth")) {
            if (!player.hasPermission("wkoth.admin")) {
                player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando!");
                return true;
            }

            if (args.length == 0) {
                player.sendMessage(ChatColor.GOLD + "Comandos de wKoth:");
                player.sendMessage(ChatColor.YELLOW + "/koth create <nombre> - Obtén el stick para seleccionar la zona");
                player.sendMessage(ChatColor.YELLOW + "/koth set - Establece el KoTH en la zona seleccionada");
                player.sendMessage(ChatColor.YELLOW + "/koth list - Muestra la lista de KoTHs");
                player.sendMessage(ChatColor.YELLOW + "/koth delete <nombre> - Elimina un KoTH");
                player.sendMessage(ChatColor.YELLOW + "/koth start <nombre> - Inicia un KoTH");
                player.sendMessage(ChatColor.YELLOW + "/koth stop <nombre> - Detiene un KoTH");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("wkoth.admin")) {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando!");
                    return true;
                }

                reloadConfig();
                CAPTURE_TIME = getConfig().getInt("settings.capture-time", 300);
                sender.sendMessage(ChatColor.GREEN + "¡Configuración recargada exitosamente!");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso correcto: /koth create <nombre>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (koths.containsKey(kothName)) {
                    player.sendMessage(ChatColor.RED + "Ya existe un KoTH con ese nombre!");
                    return true;
                }

                creatingKoth.put(player.getUniqueId(), kothName);
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.STICK));
                player.sendMessage(ChatColor.GREEN + "¡Has recibido el stick de selección! Haz click derecho para seleccionar la primera posición y click izquierdo para la segunda.");
                return true;
            }

            if (args[0].equalsIgnoreCase("set")) {
                if (!creatingKoth.containsKey(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "¡Primero debes crear un KoTH con /koth create <nombre>!");
                    return true;
                }

                if (!pos1.containsKey(player.getUniqueId()) || !pos2.containsKey(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "¡Primero debes seleccionar ambas posiciones!");
                    return true;
                }

                String kothName = creatingKoth.get(player.getUniqueId());
                Location[] locations = new Location[]{pos1.get(player.getUniqueId()), pos2.get(player.getUniqueId())};
                koths.put(kothName, locations);

                // Limpiar datos temporales
                pos1.remove(player.getUniqueId());
                pos2.remove(player.getUniqueId());
                creatingKoth.remove(player.getUniqueId());

                player.sendMessage(ChatColor.GREEN + "¡KoTH '" + kothName + "' creado exitosamente!");
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                if (koths.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "No hay KoTHs creados.");
                    return true;
                }

                player.sendMessage(ChatColor.GOLD + "Lista de KoTHs:");
                for (String kothName : koths.keySet()) {
                    Location[] locs = koths.get(kothName);
                    player.sendMessage(ChatColor.YELLOW + "- " + kothName + " (" +
                            "Mundo: " + locs[0].getWorld().getName() + ", " +
                            "Pos1: " + formatLocation(locs[0]) + ", " +
                            "Pos2: " + formatLocation(locs[1]) + ")");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("start")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso correcto: /koth start <nombre>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(ChatColor.RED + "No existe un KoTH con ese nombre!");
                    return true;
                }

                if (activeKoths.containsKey(kothName) && activeKoths.get(kothName)) {
                    player.sendMessage(ChatColor.RED + "¡Este KoTH ya está activo!");
                    return true;
                }

                startKoth(kothName);
                player.sendMessage(ChatColor.GREEN + "¡KoTH '" + kothName + "' iniciado!");
                return true;
            }

            if (args[0].equalsIgnoreCase("stop")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso correcto: /koth stop <nombre>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!activeKoths.containsKey(kothName) || !activeKoths.get(kothName)) {
                    player.sendMessage(ChatColor.RED + "¡Este KoTH no está activo!");
                    return true;
                }

                stopKoth(kothName);
                player.sendMessage(ChatColor.GREEN + "¡KoTH '" + kothName + "' detenido!");
                return true;
            }

            if (args[0].equalsIgnoreCase("delete")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso correcto: /koth delete <nombre>");
                    return true;
                }

                String kothName = args[1].toLowerCase();
                if (!koths.containsKey(kothName)) {
                    player.sendMessage(ChatColor.RED + "No existe un KoTH con ese nombre!");
                    return true;
                }

                koths.remove(kothName);
                player.sendMessage(ChatColor.GREEN + "¡KoTH '" + kothName + "' eliminado exitosamente!");
                return true;
            }
        }
        return false;
    }

    private String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }

    private void giveRewards(Player player, String kothName) {
        List<String> commands;

        // Intentar obtener recompensas específicas del KoTH
        if (getConfig().contains("rewards." + kothName)) {
            commands = getConfig().getStringList("rewards." + kothName + ".commands");
        } else {
            // Usar recompensas por defecto
            commands = getConfig().getStringList("rewards.default.commands");
        }

        // Ejecutar comandos de recompensa
        for (String command : commands) {
            command = command.replace("%player%", player.getName());
            getServer().dispatchCommand(getServer().getConsoleSender(),
                    ChatColor.translateAlternateColorCodes('&', command));
        }
    }

    private void startKoth(String kothName) {
        activeKoths.put(kothName, true);
        kothTimers.put(kothName, CAPTURE_TIME);

        getServer().broadcastMessage(ChatColor.GOLD + "=========================");
        getServer().broadcastMessage(ChatColor.YELLOW + "¡El KoTH '" + kothName + "' ha comenzado!");
        getServer().broadcastMessage(ChatColor.YELLOW + "¡Tiempo restante: " + formatTime(CAPTURE_TIME) + "!");
        getServer().broadcastMessage(ChatColor.GOLD + "=========================");

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                int timeLeft = kothTimers.get(kothName);

                if (timeLeft <= 0) {
                    Player winner = capturingPlayers.get(kothName);
                    if (winner != null) {
                        getServer().broadcastMessage(ChatColor.GOLD + "=========================");
                        getServer().broadcastMessage(ChatColor.GREEN + "¡" + winner.getName() +
                                " ha ganado el KoTH '" + kothName + "'!");
                        getServer().broadcastMessage(ChatColor.GOLD + "=========================");

                        // Dar recompensas al ganador
                        giveRewards(winner, kothName);
                    }
                    stopKoth(kothName);
                    return;
                }

                if (timeLeft % 30 == 0) { // Mensaje cada 30 segundos
                    getServer().broadcastMessage(ChatColor.YELLOW + "Tiempo restante del KoTH '" + kothName + "': " + formatTime(timeLeft));
                }

                kothTimers.put(kothName, timeLeft - 1);
            }
        };

        task.runTaskTimer(this, 20L, 20L); // Ejecutar cada segundo
        kothTasks.put(kothName, task);
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

        getServer().broadcastMessage(ChatColor.RED + "¡El KoTH '" + kothName + "' ha terminado!");
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
                // Jugador entró al KoTH
                if (!capturingPlayers.containsKey(kothName)) {
                    capturingPlayers.put(kothName, player);
                    getServer().broadcastMessage(ChatColor.YELLOW + player.getName() +
                            " está capturando el KoTH '" + kothName + "'!");
                }
            } else if (!isInKoth && wasInKoth) {
                // Jugador salió del KoTH
                capturingPlayers.remove(kothName);
                getServer().broadcastMessage(ChatColor.RED + player.getName() +
                        " ha perdido el control del KoTH '" + kothName + "'!");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("wkoth.admin")) return;

        if (player.getItemInHand() != null && player.getItemInHand().getType() == Material.STICK) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                pos1.put(player.getUniqueId(), event.getClickedBlock().getLocation());
                player.sendMessage(ChatColor.GREEN + "Primera posición seleccionada!");
                event.setCancelled(true);
            } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                pos2.put(player.getUniqueId(), event.getClickedBlock().getLocation());
                player.sendMessage(ChatColor.GREEN + "Segunda posición seleccionada!");
                event.setCancelled(true);
            }
        }
    }
}
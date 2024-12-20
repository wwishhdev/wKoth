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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class wKoth extends JavaPlugin implements Listener {
    private HashMap<UUID, Location> pos1 = new HashMap<>();
    private HashMap<UUID, Location> pos2 = new HashMap<>();
    private HashMap<String, Location[]> koths = new HashMap<>(); // Almacena los KoTHs por nombre
    private HashMap<UUID, String> creatingKoth = new HashMap<>(); // Almacena qué KoTH está creando cada jugador

    @Override
    public void onEnable() {
        // Registrar eventos
        getServer().getPluginManager().registerEvents(this, this);

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
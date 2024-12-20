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
    private Location kothLocation = null;

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
                player.sendMessage(ChatColor.YELLOW + "/koth create - Obtén el stick para seleccionar la zona");
                player.sendMessage(ChatColor.YELLOW + "/koth set - Establece el KoTH en la zona seleccionada");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.STICK));
                player.sendMessage(ChatColor.GREEN + "¡Has recibido el stick de selección! Haz click derecho para seleccionar la primera posición y click izquierdo para la segunda.");
                return true;
            }

            if (args[0].equalsIgnoreCase("set")) {
                if (!pos1.containsKey(player.getUniqueId()) || !pos2.containsKey(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "¡Primero debes seleccionar ambas posiciones!");
                    return true;
                }

                Location loc1 = pos1.get(player.getUniqueId());
                Location loc2 = pos2.get(player.getUniqueId());
                kothLocation = new Location(loc1.getWorld(),
                        (loc1.getX() + loc2.getX()) / 2,
                        (loc1.getY() + loc2.getY()) / 2,
                        (loc1.getZ() + loc2.getZ()) / 2);

                player.sendMessage(ChatColor.GREEN + "¡KoTH creado exitosamente!");
                return true;
            }
        }
        return false;
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
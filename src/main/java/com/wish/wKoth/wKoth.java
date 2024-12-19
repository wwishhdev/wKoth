package com.wish.wKoth;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class wKoth extends JavaPlugin implements Listener {
    private Location pos1;
    private Location pos2;
    private boolean isActive = false;
    private Player currentKingPlayer = null;
    private int captureTime = 0;
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
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "wKoth ha sido activado!");

        // Inicialización del plugin
        koths = new HashMap<>();
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        loadKoths();
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "wKoth ha sido desactivado!");
        saveLocations();
    }

    private void loadLocations() {
        if (getConfig().contains("locations.pos1")) {
            pos1 = (Location) getConfig().get("locations.pos1");
        }
        if (getConfig().contains("locations.pos2")) {
            pos2 = (Location) getConfig().get("locations.pos2");
        }
    }

    private void saveLocations() {
        if (pos1 != null) {
            getConfig().set("locations.pos1", pos1);
        }
        if (pos2 != null) {
            getConfig().set("locations.pos2", pos2);
        }
        saveConfig();
    }

    private void loadKoths() {
        if (!getConfig().contains("koths")) return;

        for (String kothId : getConfig().getConfigurationSection("koths").getKeys(false)) {
            String name = getConfig().getString("koths." + kothId + ".name");
            int duration = getConfig().getInt("koths." + kothId + ".duration");

            KothArena arena = new KothArena(kothId, name, duration);

            if (getConfig().contains("koths." + kothId + ".pos1")) {
                arena.setPos1((Location) getConfig().get("koths." + kothId + ".pos1"));
            }
            if (getConfig().contains("koths." + kothId + ".pos2")) {
                arena.setPos2((Location) getConfig().get("koths." + kothId + ".pos2"));
            }

            koths.put(kothId, arena);
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
        saveConfig();
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

    private void reloadConfiguration(Player player) {
        // Guardar las ubicaciones actuales antes de recargar
        saveLocations();

        // Recargar la configuración
        reloadConfig();

        // Cargar las ubicaciones después de recargar
        loadLocations();

        // Enviar mensaje de confirmación
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.config-reloaded")));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isActive || pos1 == null || pos2 == null) return;

        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();

        if (isInside(playerLoc)) {
            if (currentKingPlayer == null || !currentKingPlayer.equals(player)) {
                currentKingPlayer = player;
                captureTime = 0;
                broadcastKothMessage(ChatColor.translateAlternateColorCodes('&',
                        getConfig().getString("messages.koth-control")
                                .replace("%player%", player.getName())
                                .replace("%time%", "0")));
            }
        } else if (currentKingPlayer != null && currentKingPlayer.equals(player)) {
            currentKingPlayer = null;
            captureTime = 0;
        }
    }

    private boolean isInside(Location loc) {
        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    private void startKoth() {
        if (isActive) return;
        if (pos1 == null || pos2 == null) {
            getServer().broadcastMessage(ChatColor.RED + "¡El KoTH no está configurado correctamente!");
            return;
        }

        isActive = true;
        broadcastKothMessage(getConfig().getString("messages.koth-start"));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive) {
                    this.cancel();
                    return;
                }

                if (currentKingPlayer != null) {
                    captureTime++;
                    if (captureTime >= getConfig().getInt("koth.duration")) {
                        playerWon(currentKingPlayer);
                        stopKoth();
                        this.cancel();
                    } else if (captureTime % getConfig().getInt("koth.broadcast-interval") == 0) {
                        broadcastKothMessage(ChatColor.translateAlternateColorCodes('&',
                                getConfig().getString("messages.koth-control")
                                        .replace("%player%", currentKingPlayer.getName())
                                        .replace("%time%", String.valueOf(captureTime))));
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void stopKoth() {
        isActive = false;
        currentKingPlayer = null;
        captureTime = 0;
        broadcastKothMessage(getConfig().getString("messages.koth-end"));
    }

    private void playerWon(Player player) {
        broadcastKothMessage(getConfig().getString("messages.koth-captured")
                .replace("%player%", player.getName()));

        for (String command : getConfig().getStringList("rewards.commands")) {
            getServer().dispatchCommand(getServer().getConsoleSender(),
                    command.replace("%player%", player.getName()));
        }
    }

    private void broadcastKothMessage(String message) {
        getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.prefix") + message));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== KoTH Commands ===");
        if (player.hasPermission("wkoth.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/koth start " + ChatColor.GRAY + "- Inicia el evento");
            player.sendMessage(ChatColor.YELLOW + "/koth stop " + ChatColor.GRAY + "- Detiene el evento");
            player.sendMessage(ChatColor.YELLOW + "/koth setpos1 " + ChatColor.GRAY + "- Establece la primera posición");
            player.sendMessage(ChatColor.YELLOW + "/koth setpos2 " + ChatColor.GRAY + "- Establece la segunda posición");
            player.sendMessage(ChatColor.YELLOW + "/koth reload " + ChatColor.GRAY + "- Recarga la configuración");
        }
        player.sendMessage(ChatColor.GOLD + "==================");
    }
}

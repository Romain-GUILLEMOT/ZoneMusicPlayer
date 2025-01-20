package fr.yotapaki.ZoneMusicPlayer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class CommandManager implements CommandExecutor {

    private final Main plugin;
    private final MusicManager musicManager;

    public CommandManager(Main plugin, MusicManager musicManager) {
        this.plugin = plugin;
        this.musicManager = musicManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                showHelp(sender);
                break;

            case "reload":
                if (!sender.hasPermission("zonemusic.reload")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to reload the configuration.");
                    return true;
                }
                musicManager.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "ZoneMusicPlayer configuration reloaded.");
                break;

            case "debug":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Debug mode can only be toggled by players.");
                    return true;
                }
                if (!sender.hasPermission("zonemusic.debug")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to toggle debug mode.");
                    return true;
                }
                Player player = (Player) sender;
                musicManager.toggleDebug(player);
                sender.sendMessage(ChatColor.YELLOW + "Debug mode toggled.");
                break;

            case "play":
                if (!sender.hasPermission("zonemusic.play")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to play custom sounds.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /zonemusic play <player> <sound> <category>");
                    sender.sendMessage(ChatColor.GRAY + "Example: /zonemusic play Yotapaki custom:my_sound music");
                    return true;
                }

                String playerName = args[1];
                String soundName = args[2];
                String categoryName = args[3].toUpperCase();

                // Trouver le joueur cible
                Player targetPlayer = Bukkit.getPlayer(playerName);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' not found.");
                    return true;
                }

                // Valider la catégorie du son
                SoundCategory category;
                try {
                    category = SoundCategory.valueOf(categoryName);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid sound category. Valid categories are:");
                    for (SoundCategory cat : SoundCategory.values()) {
                        sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + cat.name().toLowerCase());
                    }
                    return true;
                }

                // Jouer le son pour le joueur cible
                targetPlayer.playSound(targetPlayer.getLocation(), soundName, category, 1.0f, 1.0f);
                sender.sendMessage(ChatColor.GREEN + "Playing sound: " + ChatColor.AQUA + soundName + ChatColor.GREEN +
                        " in category: " + ChatColor.AQUA + category.name().toLowerCase() + ChatColor.GREEN +
                        " for player: " + ChatColor.YELLOW + playerName);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /zonemusic help for a list of commands.");
                break;
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "======== " + ChatColor.AQUA + "ZoneMusicPlayer Commands" + ChatColor.GOLD + " ========");
        sender.sendMessage(ChatColor.YELLOW + "/zonemusic help" + ChatColor.GRAY + " - Show this help message.");
        sender.sendMessage(ChatColor.YELLOW + "/zonemusic reload" + ChatColor.GRAY + " - Reload the plugin configuration.");
        sender.sendMessage(ChatColor.YELLOW + "/zonemusic debug" + ChatColor.GRAY + " - Toggle debug mode (players only).");
        sender.sendMessage(ChatColor.YELLOW + "/zonemusic play <player> <sound> <category>" + ChatColor.GRAY +
                " - Play a custom sound for a specific player.");
        sender.sendMessage(ChatColor.GRAY + "Example: " + ChatColor.GREEN + "/zonemusic play Yotapaki custom:my_sound music");
        sender.sendMessage(ChatColor.GOLD + "=======================================");
    }
}
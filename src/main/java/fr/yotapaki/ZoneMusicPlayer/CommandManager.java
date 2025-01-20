package fr.yotapaki.ZoneMusicPlayer;

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
            sender.sendMessage("Usage: /zonemusic <reload|debug>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("zonemusic.reload")) {
                    sender.sendMessage("You do not have permission to reload the configuration.");
                    return true;
                }
                musicManager.reloadConfig();
                sender.sendMessage("ZoneMusicPlayer configuration reloaded.");
                break;

            case "debug":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Debug mode can only be toggled by players.");
                    return true;
                }
                if (!sender.hasPermission("zonemusic.debug")) {
                    sender.sendMessage("You do not have permission to toggle debug mode.");
                    return true;
                }
                Player player = (Player) sender;
                musicManager.toggleDebug(player);
                break;

            default:
                sender.sendMessage("Unknown subcommand.");
                break;
        }
        return true;
    }
}
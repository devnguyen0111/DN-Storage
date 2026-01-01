package org.dnplugins.dNStorage;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý các lệnh của plugin
 */
public class CommandHandler implements CommandExecutor, TabCompleter {

    private final StorageGUI storageGUI;
    private final LanguageManager languageManager;

    public CommandHandler(StorageGUI storageGUI, LanguageManager languageManager) {
        this.storageGUI = storageGUI;
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getMessage("command.only_player"));
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("kho") ||
                command.getName().equalsIgnoreCase("storage")) {

            if (!player.hasPermission("dnstorage.use")) {
                player.sendMessage(languageManager.getMessage("command.no_permission"));
                return true;
            }

            storageGUI.openMainGUI(player);
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>(); // Không có tab completion
    }
}

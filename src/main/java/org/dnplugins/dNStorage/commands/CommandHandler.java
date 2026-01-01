package org.dnplugins.dNStorage.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.dnplugins.dNStorage.DNStorage;
import org.dnplugins.dNStorage.core.LanguageManager;
import org.dnplugins.dNStorage.gui.StorageGUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý các lệnh của plugin
 */
public class CommandHandler implements CommandExecutor, TabCompleter {

    private final StorageGUI storageGUI;
    private final LanguageManager languageManager;
    private final JavaPlugin plugin;

    public CommandHandler(StorageGUI storageGUI, LanguageManager languageManager, JavaPlugin plugin) {
        this.storageGUI = storageGUI;
        this.languageManager = languageManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("kho") ||
                command.getName().equalsIgnoreCase("storage")) {

            // Xử lý lệnh reload
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("dnstorage.reload")) {
                    sender.sendMessage(languageManager.getMessage("command.no_permission"));
                    return true;
                }

                if (plugin instanceof DNStorage) {
                    ((DNStorage) plugin).reloadPlugin();
                    sender.sendMessage(languageManager.getMessage("command.reload.success"));
                } else {
                    sender.sendMessage(languageManager.getMessage("command.reload.failed"));
                }
                return true;
            }

            // Xử lý lệnh sort
            if (args.length > 0 && args[0].equalsIgnoreCase("sort")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(languageManager.getMessage("command.only_player"));
                    return true;
                }

                Player player = (Player) sender;
                if (!player.hasPermission("dnstorage.use")) {
                    player.sendMessage(languageManager.getMessage("command.no_permission"));
                    return true;
                }

                storageGUI.sortPlayerInventory(player);
                return true;
            }

            // Lệnh mở GUI (chỉ dành cho player)
            if (!(sender instanceof Player)) {
                sender.sendMessage(languageManager.getMessage("command.only_player"));
                return true;
            }

            Player player = (Player) sender;
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
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("dnstorage.reload") && "reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            if (sender.hasPermission("dnstorage.use") && "sort".startsWith(args[0].toLowerCase())) {
                completions.add("sort");
            }
        }
        return completions;
    }
}

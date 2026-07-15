package com.example.plugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Base command abstraction handling common player checks and tab completion.
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be executed by in-game players.");
            return true;
        }
        if (label.equalsIgnoreCase("base") || label.equalsIgnoreCase("mybase")) {
            return execute(player, new String[]{"map"});
        }
        return execute(player, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        return tabComplete(player, args);
    }

    public abstract boolean execute(Player player, String[] args);

    public abstract List<String> tabComplete(Player player, String[] args);
}

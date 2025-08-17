package dev.keaneaudric.soulRealmsVoiceUtils.commands;

import dev.keaneaudric.soulRealmsVoiceUtils.SoulRealmsVoiceUtils;
import dev.keaneaudric.soulRealmsVoiceUtils.managers.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {
    
    private final SoulRealmsVoiceUtils plugin;
    private final MiniMessage miniMessage;
    
    public MainCommand(SoulRealmsVoiceUtils plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(miniMessage.deserialize("<red>Usage: <yellow>/" + label + " reload"));
            return true;
        }
        
        handleReload(sender);
        return true;
    }
    
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("soulrealmsvoiceutils.reload")) {
            sender.sendMessage(miniMessage.deserialize("<red>You don't have permission to use this command!"));
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            plugin.reloadConfig();
            
            if (plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().reload();
            }
            
            if (plugin.getVoiceChatMuteListener() != null) {
                plugin.getVoiceChatMuteListener().reloadConfiguration();
            }
            
            long timeTaken = System.currentTimeMillis() - startTime;
            
            LanguageManager langManager = plugin.getLanguageManager();
            String prefix = langManager != null ? langManager.getRawMessage("prefix") : "";
            Component successMessage = miniMessage.deserialize(
                prefix + "<green>Configuration reloaded successfully! <gray>(" + timeTaken + "ms)</gray>"
            );
            sender.sendMessage(successMessage);
            
            if (sender instanceof Player) {
                plugin.getSLF4JLogger().info("Configuration reloaded by {} ({}ms)", sender.getName(), timeTaken);
            }
            
        } catch (Exception e) {
            LanguageManager langManager = plugin.getLanguageManager();
            String prefix = langManager != null ? langManager.getRawMessage("prefix") : "";
            Component errorMessage = miniMessage.deserialize(
                prefix + "<red>Failed to reload configuration: " + e.getMessage()
            );
            sender.sendMessage(errorMessage);
            plugin.getSLF4JLogger().error("Failed to reload configuration", e);
        }
    }
    
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("soulrealmsvoiceutils.reload")) {
            List<String> completions = new ArrayList<>();
            completions.add("reload");
            if ("reload".startsWith(args[0].toLowerCase())) {
                return completions;
            }
        }
        return new ArrayList<>();
    }
}

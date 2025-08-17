package dev.keaneaudric.soulRealmsVoiceUtils;

import dev.keaneaudric.soulRealmsVoiceUtils.commands.MainCommand;
import dev.keaneaudric.soulRealmsVoiceUtils.hooks.PlaceholderAPIHook;
import dev.keaneaudric.soulRealmsVoiceUtils.listeners.VoiceChatListener;
import dev.keaneaudric.soulRealmsVoiceUtils.listeners.VoiceChatMuteListener;
import dev.keaneaudric.soulRealmsVoiceUtils.managers.VoiceChatManager;
import dev.keaneaudric.soulRealmsVoiceUtils.managers.MuteManager;
import dev.keaneaudric.soulRealmsVoiceUtils.managers.LanguageManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;

public final class SoulRealmsVoiceUtils extends JavaPlugin {
    
    private static SoulRealmsVoiceUtils instance;
    
    private VoiceChatManager voiceChatManager;
    private MuteManager muteManager;
    private LanguageManager languageManager;
    private VoiceChatListener voiceChatListener;
    private VoiceChatMuteListener voiceChatMuteListener;
    private PlaceholderAPIHook placeholderHook;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        languageManager = new LanguageManager(this);
        voiceChatManager = new VoiceChatManager();
        muteManager = new MuteManager(this);
        
        registerCommands();
        
        if (Bukkit.getPluginManager().getPlugin("voicechat") != null) {
            initializeVoiceChat();
        } else {
            getSLF4JLogger().warn("SimpleVoiceChat not found. Voice chat features will be limited.");
        }
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            registerPlaceholderAPI();
        } else {
            getSLF4JLogger().warn("PlaceholderAPI not found. Placeholders will not be available.");
        }
    }
    
    private void initializeVoiceChat() {
        BukkitVoicechatService service = Bukkit.getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            voiceChatListener = new VoiceChatListener(this, voiceChatManager);
            service.registerPlugin(voiceChatListener);
            
            voiceChatMuteListener = new VoiceChatMuteListener(this, muteManager, languageManager);
            service.registerPlugin(voiceChatMuteListener);
            
            getSLF4JLogger().info("SimpleVoiceChat integration initialized.");
        } else {
            Bukkit.getScheduler().runTaskLater(this, this::initializeVoiceChat, 20L);
        }
    }
    
    private void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        var command = getCommand("soulrealmsvoiceutils");
        if (command != null) {
            command.setExecutor(mainCommand);
            command.setTabCompleter(mainCommand);
        } else {
            getSLF4JLogger().error("Failed to register command!");
        }
    }
    
    private void registerPlaceholderAPI() {
        if (placeholderHook != null) {
            placeholderHook.unregister();
        }
        
        placeholderHook = new PlaceholderAPIHook(this, voiceChatManager, muteManager, languageManager);
        if (placeholderHook.register()) {
            getSLF4JLogger().info("PlaceholderAPI hook registered successfully!");
            getSLF4JLogger().info("Placeholder: %voicechat_status%");
        } else {
            getSLF4JLogger().error("Failed to register PlaceholderAPI hook!");
            Bukkit.getScheduler().runTaskLater(this, this::registerPlaceholderAPI, 20L);
        }
    }

    @Override
    public void onDisable() {
        if (placeholderHook != null) {
            placeholderHook.unregister();
        }
        
        if (voiceChatManager != null) {
            voiceChatManager.cleanup();
        }
        
        if (muteManager != null) {
            muteManager.cleanup();
        }
        
        instance = null;
    }
    
    public static SoulRealmsVoiceUtils getInstance() {
        return instance;
    }
    
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    public VoiceChatMuteListener getVoiceChatMuteListener() {
        return voiceChatMuteListener;
    }
}

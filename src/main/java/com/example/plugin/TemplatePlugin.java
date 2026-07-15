package com.example.plugin;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for PluginTemplate.
 * This is a clean template for Minecraft plugin development.
 */
public final class TemplatePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        getLogger().info("PluginTemplate has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PluginTemplate has been successfully disabled.");
    }
}

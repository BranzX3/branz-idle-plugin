package com.example.plugin.bootstrap;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.database.DatabaseManager;
import com.example.plugin.database.SaveQueueService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Centralized service locator and manual dependency injection container.
 * Avoids reflection and static singletons scattered throughout domain classes.
 */
public class ServiceRegistry {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final SaveQueueService saveQueueService;
    private final RegistryManager registryManager;
    private final ProviderManager providerManager;
    private final Map<Class<?>, Object> services = new HashMap<>();

    public ServiceRegistry(
        JavaPlugin plugin,
        DatabaseManager databaseManager,
        SaveQueueService saveQueueService,
        RegistryManager registryManager,
        ProviderManager providerManager
    ) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.saveQueueService = saveQueueService;
        this.registryManager = registryManager;
        this.providerManager = providerManager;
    }

    /**
     * Registers a domain service instance.
     *
     * @param clazz Service interface or class type
     * @param instance Service instance
     * @param <T> Service type
     */
    public <T> void registerService(Class<T> clazz, T instance) {
        services.put(clazz, instance);
    }

    /**
     * Retrieves a registered service instance.
     *
     * @param clazz Service interface or class type
     * @param <T> Service type
     * @return Optional containing the service if registered
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getService(Class<T> clazz) {
        return Optional.ofNullable((T) services.get(clazz));
    }

    /**
     * Retrieves a registered service instance or throws IllegalStateException if missing.
     *
     * @param clazz Service interface or class type
     * @param <T> Service type
     * @return Service instance
     */
    public <T> T getRequiredService(Class<T> clazz) {
        return getService(clazz).orElseThrow(() ->
            new IllegalStateException("Required service not registered: " + clazz.getName()));
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SaveQueueService getSaveQueueService() {
        return saveQueueService;
    }

    public RegistryManager getRegistryManager() {
        return registryManager;
    }

    public ProviderManager getProviderManager() {
        return providerManager;
    }
}

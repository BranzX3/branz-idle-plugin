package com.example.plugin.economy.service;

import com.example.plugin.database.SaveQueueService;
import com.example.plugin.economy.model.PlayerProfile;
import com.example.plugin.economy.repository.PlayerRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of EconomyService using Caffeine caching and SaveQueueService async writes.
 */
public class EconomyServiceImpl implements EconomyService {

    private final JavaPlugin plugin;
    private final PlayerRepository repository;
    private final SaveQueueService saveQueue;
    private final Cache<UUID, PlayerProfile> profileCache;

    public EconomyServiceImpl(JavaPlugin plugin, PlayerRepository repository, SaveQueueService saveQueue) {
        this.plugin = plugin;
        this.repository = repository;
        this.saveQueue = saveQueue;
        this.profileCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();
    }

    @Override
    public Optional<PlayerProfile> getProfile(UUID playerId) {
        PlayerProfile cached = profileCache.getIfPresent(playerId);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<PlayerProfile> loaded = repository.findById(playerId);
        loaded.ifPresent(profile -> profileCache.put(playerId, profile));
        return loaded;
    }

    @Override
    public PlayerProfile getOrCreateProfile(UUID playerId, String username) {
        PlayerProfile cached = profileCache.getIfPresent(playerId);
        if (cached != null) {
            if (!cached.getUsername().equals(username)) {
                cached.setUsername(username);
                queueSave(cached);
            }
            return cached;
        }

        Optional<PlayerProfile> loaded = repository.findById(playerId);
        if (loaded.isPresent()) {
            PlayerProfile profile = loaded.get();
            if (!profile.getUsername().equals(username)) {
                profile.setUsername(username);
                queueSave(profile);
            }
            profileCache.put(playerId, profile);
            return profile;
        }

        long now = System.currentTimeMillis();
        PlayerProfile newProfile = new PlayerProfile(playerId, username, 0.0, 0L, false, now, now);
        profileCache.put(playerId, newProfile);
        queueSave(newProfile);
        return newProfile;
    }

    @Override
    public void addCoins(UUID playerId, double amount) {
        getProfile(playerId).ifPresent(profile -> {
            profile.addCoins(amount);
            queueSave(profile);
        });
    }

    @Override
    public boolean removeCoins(UUID playerId, double amount) {
        Optional<PlayerProfile> opt = getProfile(playerId);
        if (opt.isPresent()) {
            PlayerProfile profile = opt.get();
            if (profile.removeCoins(amount)) {
                queueSave(profile);
                return true;
            }
        }
        return false;
    }

    @Override
    public void addDiamonds(UUID playerId, long amount) {
        getProfile(playerId).ifPresent(profile -> {
            profile.addDiamonds(amount);
            queueSave(profile);
        });
    }

    @Override
    public boolean removeDiamonds(UUID playerId, long amount) {
        Optional<PlayerProfile> opt = getProfile(playerId);
        if (opt.isPresent()) {
            PlayerProfile profile = opt.get();
            if (profile.removeDiamonds(amount)) {
                queueSave(profile);
                return true;
            }
        }
        return false;
    }

    @Override
    public void setOnboardingCompleted(UUID playerId, boolean completed) {
        getProfile(playerId).ifPresent(profile -> {
            profile.setOnboardingCompleted(completed);
            queueSave(profile);
        });
    }

    @Override
    public void unloadProfile(UUID playerId) {
        PlayerProfile cached = profileCache.getIfPresent(playerId);
        if (cached != null) {
            // Save synchronously or ensure final queue before unloading from cache
            repository.save(cached);
            profileCache.invalidate(playerId);
        }
    }

    @Override
    public void flushAll() {
        for (PlayerProfile profile : profileCache.asMap().values()) {
            repository.save(profile);
        }
    }

    private void queueSave(PlayerProfile profile) {
        saveQueue.queueTask(() -> repository.save(profile));
    }
}

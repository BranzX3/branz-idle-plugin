package com.example.plugin.economy.service;

import com.example.plugin.database.SaveQueueService;
import com.example.plugin.economy.model.PlayerProfile;
import com.example.plugin.economy.repository.PlayerRepository;
import com.example.plugin.integration.vault.VaultBridge;
import com.example.plugin.integration.points.PlayerPointsBridge;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of EconomyService using Caffeine caching and SaveQueueService async writes.
 * Supports hot-swapping synchronization with Vault and PlayerPoints.
 */
public class EconomyServiceImpl implements EconomyService {

    private final JavaPlugin plugin;
    private final PlayerRepository repository;
    private final SaveQueueService saveQueue;
    private final Cache<UUID, PlayerProfile> profileCache;

    private VaultBridge vaultBridge;
    private PlayerPointsBridge playerPointsBridge;
    private final boolean vaultSyncEnabled;
    private final boolean playerPointsSyncEnabled;

    public EconomyServiceImpl(JavaPlugin plugin, PlayerRepository repository, SaveQueueService saveQueue) {
        this.plugin = plugin;
        this.repository = repository;
        this.saveQueue = saveQueue;
        this.profileCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();
        this.vaultSyncEnabled = plugin.getConfig().getBoolean("economy.vault_sync", true);
        this.playerPointsSyncEnabled = plugin.getConfig().getBoolean("economy.playerpoints_sync", true);
    }

    public void setVaultBridge(VaultBridge vaultBridge) {
        this.vaultBridge = vaultBridge;
    }

    public void setPlayerPointsBridge(PlayerPointsBridge playerPointsBridge) {
        this.playerPointsBridge = playerPointsBridge;
    }

    private void syncProfileExternal(PlayerProfile profile) {
        if (profile == null) return;
        if (vaultSyncEnabled && vaultBridge != null && vaultBridge.hasVaultHook()) {
            profile.setCoins(vaultBridge.getVaultBalance(profile.getPlayerId()));
        }
        if (playerPointsSyncEnabled && playerPointsBridge != null && playerPointsBridge.hasHook()) {
            profile.setDiamonds(playerPointsBridge.getPoints(profile.getPlayerId()));
        }
    }

    @Override
    public Optional<PlayerProfile> getProfile(UUID playerId) {
        PlayerProfile cached = profileCache.getIfPresent(playerId);
        if (cached != null) {
            syncProfileExternal(cached);
            return Optional.of(cached);
        }

        Optional<PlayerProfile> loaded = repository.findById(playerId);
        loaded.ifPresent(profile -> {
            syncProfileExternal(profile);
            profileCache.put(playerId, profile);
        });
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
            syncProfileExternal(cached);
            return cached;
        }

        Optional<PlayerProfile> loaded = repository.findById(playerId);
        if (loaded.isPresent()) {
            PlayerProfile profile = loaded.get();
            if (!profile.getUsername().equals(username)) {
                profile.setUsername(username);
                queueSave(profile);
            }
            syncProfileExternal(profile);
            profileCache.put(playerId, profile);
            return profile;
        }

        long now = System.currentTimeMillis();
        PlayerProfile newProfile = new PlayerProfile(playerId, username, 0.0, 0L, false, now, now);
        syncProfileExternal(newProfile);
        profileCache.put(playerId, newProfile);
        queueSave(newProfile);
        return newProfile;
    }

    @Override
    public void addCoins(UUID playerId, double amount) {
        if (vaultSyncEnabled && vaultBridge != null && vaultBridge.hasVaultHook()) {
            vaultBridge.depositVault(playerId, amount);
            getProfile(playerId).ifPresent(profile -> {
                profile.setCoins(vaultBridge.getVaultBalance(playerId));
                queueSave(profile);
            });
        } else {
            getProfile(playerId).ifPresent(profile -> {
                profile.addCoins(amount);
                queueSave(profile);
            });
        }
    }

    @Override
    public boolean removeCoins(UUID playerId, double amount) {
        if (vaultSyncEnabled && vaultBridge != null && vaultBridge.hasVaultHook()) {
            boolean success = vaultBridge.withdrawVault(playerId, amount);
            if (success) {
                getProfile(playerId).ifPresent(profile -> {
                    profile.setCoins(vaultBridge.getVaultBalance(playerId));
                    queueSave(profile);
                });
            }
            return success;
        } else {
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
    }

    @Override
    public void addDiamonds(UUID playerId, long amount) {
        if (playerPointsSyncEnabled && playerPointsBridge != null && playerPointsBridge.hasHook()) {
            playerPointsBridge.givePoints(playerId, amount);
            getProfile(playerId).ifPresent(profile -> {
                profile.setDiamonds(playerPointsBridge.getPoints(playerId));
                queueSave(profile);
            });
        } else {
            getProfile(playerId).ifPresent(profile -> {
                profile.addDiamonds(amount);
                queueSave(profile);
            });
        }
    }

    @Override
    public boolean removeDiamonds(UUID playerId, long amount) {
        if (playerPointsSyncEnabled && playerPointsBridge != null && playerPointsBridge.hasHook()) {
            boolean success = playerPointsBridge.takePoints(playerId, amount);
            if (success) {
                getProfile(playerId).ifPresent(profile -> {
                    profile.setDiamonds(playerPointsBridge.getPoints(playerId));
                    queueSave(profile);
                });
            }
            return success;
        } else {
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

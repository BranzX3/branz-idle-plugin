package com.example.plugin.storage.service;

import com.example.plugin.database.SaveQueueService;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.storage.model.NodeStorage;
import com.example.plugin.storage.model.PlayerResourceWallet;
import com.example.plugin.storage.repository.StorageRepository;
import com.example.plugin.storage.repository.WalletRepository;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of StorageService using ConcurrentHashMap caching and async batch saves.
 */
public class StorageServiceImpl implements StorageService {

    private final JavaPlugin plugin;
    private final StorageRepository storageRepository;
    private final WalletRepository walletRepository;
    private final SaveQueueService saveQueue;
    private final NodeService nodeService;
    private final Map<UUID, NodeStorage> storageCache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerResourceWallet> walletCache = new ConcurrentHashMap<>();

    public StorageServiceImpl(
        JavaPlugin plugin,
        StorageRepository storageRepository,
        WalletRepository walletRepository,
        SaveQueueService saveQueue,
        NodeService nodeService
    ) {
        this.plugin = plugin;
        this.storageRepository = storageRepository;
        this.walletRepository = walletRepository;
        this.saveQueue = saveQueue;
        this.nodeService = nodeService;
    }

    @Override
    public void initialize() {
        storageCache.clear();
        for (NodeStorage s : storageRepository.loadAll()) {
            storageCache.put(s.getNodeId(), s);
        }
        walletCache.clear();
        for (PlayerResourceWallet w : walletRepository.loadAll()) {
            walletCache.put(w.getPlayerId(), w);
        }
        plugin.getLogger().info("[StorageService] Cached " + storageCache.size() + " node storages and " + walletCache.size() + " player wallets.");
    }

    @Override
    public NodeStorage getNodeStorage(UUID nodeId) {
        return storageCache.computeIfAbsent(nodeId, k -> new NodeStorage(nodeId, new HashMap<>()));
    }

    @Override
    public PlayerResourceWallet getPlayerWallet(UUID playerId) {
        return walletCache.computeIfAbsent(playerId, k -> new PlayerResourceWallet(playerId, new HashMap<>()));
    }

    @Override
    public long addResourceToNode(UUID nodeId, String resourceKey, long amount, long maxCapacity) {
        NodeStorage storage = getNodeStorage(nodeId);
        long actual = storage.addResource(resourceKey, amount, maxCapacity);
        saveQueue.queueTask(() -> storageRepository.save(storage));
        return actual;
    }

    @Override
    public boolean collectFromNode(Player player, UUID nodeId) {
        Optional<ProductionNode> nodeOpt = nodeService.getNode(nodeId);
        if (nodeOpt.isEmpty() || (!nodeOpt.get().getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("branzidle.admin"))) {
            player.sendMessage("§cYou do not own this production node!");
            return false;
        }

        NodeStorage storage = getNodeStorage(nodeId);
        Map<String, Long> resources = new HashMap<>(storage.getResourceQuantities());
        if (resources.isEmpty()) {
            player.sendMessage("§eNo accumulated resources to collect right now.");
            return false;
        }

        PlayerResourceWallet wallet = getPlayerWallet(player.getUniqueId());
        long totalCollected = 0;
        for (Map.Entry<String, Long> entry : resources.entrySet()) {
            if (entry.getValue() > 0) {
                wallet.addResource(entry.getKey(), entry.getValue());
                totalCollected += entry.getValue();
            }
        }

        storage.clear();
        saveQueue.queueTask(() -> {
            storageRepository.save(storage);
            walletRepository.save(wallet);
        });

        player.sendMessage("§aCollected " + totalCollected + " total resources directly into your virtual warehouse!");
        return true;
    }

    @Override
    public boolean spendResource(UUID playerId, String resourceKey, long amount) {
        PlayerResourceWallet wallet = getPlayerWallet(playerId);
        if (wallet.spendResource(resourceKey, amount)) {
            saveQueue.queueTask(() -> walletRepository.save(wallet));
            return true;
        }
        return false;
    }

    @Override
    public void addResourceToWallet(UUID playerId, String resourceKey, long amount) {
        PlayerResourceWallet wallet = getPlayerWallet(playerId);
        wallet.addResource(resourceKey, amount);
        saveQueue.queueTask(() -> walletRepository.save(wallet));
    }

    @Override
    public void deleteNodeStorage(UUID nodeId) {
        storageCache.remove(nodeId);
        saveQueue.queueTask(() -> storageRepository.deleteById(nodeId));
    }
}

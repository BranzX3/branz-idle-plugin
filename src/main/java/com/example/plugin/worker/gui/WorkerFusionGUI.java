package com.example.plugin.worker.gui;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.config.ResourceRegistry;
import com.example.plugin.config.WorkerRegistry;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.InventoryProvider;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.worker.biography.WorkerBiographyService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WorkerFusionGUI implements InventoryProvider {

    private final Player player;
    private final ProductionNode node;
    private final WorkerService workerService;
    private final RegistryManager registryManager;
    private final NodeService nodeService;
    private final StorageService storageService;
    private final EconomyService economyService;
    private final TerritoryService territoryService;
    private final OnboardingService onboardingService;
    private final Inventory inventory;

    // Selection Slots
    private WorkerInstance targetWorker = null;
    private WorkerInstance ingredient1 = null;
    private WorkerInstance ingredient2 = null;

    private final Map<Integer, UUID> slotToWorkerMap = new HashMap<>();

    public WorkerFusionGUI(
        Player player,
        ProductionNode node,
        WorkerService workerService,
        RegistryManager registryManager,
        NodeService nodeService,
        StorageService storageService,
        EconomyService economyService,
        TerritoryService territoryService,
        OnboardingService onboardingService
    ) {
        this.player = player;
        this.node = node;
        this.workerService = workerService;
        this.registryManager = registryManager;
        this.nodeService = nodeService;
        this.storageService = storageService;
        this.economyService = economyService;
        this.territoryService = territoryService;
        this.onboardingService = onboardingService;
        this.inventory = Bukkit.createInventory(this, 54, "§8Worker Fusion Chamber");
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToWorkerMap.clear();

        // Fill background borders
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§r").build();
        ItemStack separator = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();

        // Slots 0-9, 17, 45-53 as borders
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        inventory.setItem(9, border);
        inventory.setItem(17, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        // Separator row (Slots 18-26 border)
        for (int i = 18; i < 27; i++) {
            if (i != 22) inventory.setItem(i, separator);
        }

        // Render target slot (Slot 10)
        if (targetWorker == null) {
            inventory.setItem(10, new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("§c§l[+] Select Target")
                .lore("§7Click a worker from the deck below", "§7to place them as the primary upgrade target.").build());
        } else {
            inventory.setItem(10, buildWorkerHeadItem(targetWorker));
        }

        // Plus signs or decorations
        inventory.setItem(11, new ItemBuilder(Material.IRON_BARS).name("§d§l+").build());

        // Render Ingredient 1 (Slot 12)
        if (ingredient1 == null) {
            inventory.setItem(12, new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("§c§l[+] Select Ingredient 1")
                .lore("§7Click a worker from the deck below", "§7to place them as the first duplicate material.").build());
        } else {
            inventory.setItem(12, buildWorkerHeadItem(ingredient1));
        }

        inventory.setItem(13, new ItemBuilder(Material.IRON_BARS).name("§d§l+").build());

        // Render Ingredient 2 (Slot 14)
        if (ingredient2 == null) {
            inventory.setItem(14, new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("§c§l[+] Select Ingredient 2")
                .lore("§7Click a worker from the deck below", "§7to place them as the second duplicate material.").build());
        } else {
            inventory.setItem(14, buildWorkerHeadItem(ingredient2));
        }

        // Render equals sign
        inventory.setItem(15, new ItemBuilder(Material.IRON_BARS).name("§a§l=").build());

        // Render executing/status button (Slot 22)
        populateFusionButton();

        // Render player worker selection deck (Slots 27-44)
        List<WorkerInstance> ownedWorkers = workerService.getPlayerWorkers(player.getUniqueId());
        int slot = 27;
        for (WorkerInstance w : ownedWorkers) {
            if (slot >= 45) break;

            // Skip currently selected ingredients
            if (isWorkerSelected(w.getWorkerId())) continue;

            inventory.setItem(slot, buildWorkerHeadItem(w));
            slotToWorkerMap.put(slot, w.getWorkerId());
            slot++;
        }

        // Navigation Buttons
        inventory.setItem(45, new ItemBuilder(Material.ARROW).name("§c§lBack to Management").build());
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private void populateFusionButton() {
        if (targetWorker == null || ingredient1 == null || ingredient2 == null) {
            inventory.setItem(22, new ItemBuilder(Material.ANVIL)
                .name("§e§l⚠ INCOMPLETE SELECTION")
                .lore(
                    "§7Select 1 upgrade target and 2 duplicate ingredients",
                    "§7from the deck below to review the fusion product."
                ).build());
            return;
        }

        // Retrieve templates
        Optional<WorkerRegistry.WorkerTemplate> tTpl = registryManager.getWorkerRegistry().getTemplate(targetWorker.getTemplateId());
        Optional<WorkerRegistry.WorkerTemplate> w1Tpl = registryManager.getWorkerRegistry().getTemplate(ingredient1.getTemplateId());
        Optional<WorkerRegistry.WorkerTemplate> w2Tpl = registryManager.getWorkerRegistry().getTemplate(ingredient2.getTemplateId());

        if (tTpl.isEmpty() || w1Tpl.isEmpty() || w2Tpl.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                .name("§c§l✖ TEMPLATES NOT FOUND")
                .lore("§7Error resolving worker template data.").build());
            return;
        }

        WorkerRegistry.WorkerTemplate targetTemplate = tTpl.get();
        WorkerRegistry.WorkerTemplate w1Template = w1Tpl.get();
        WorkerRegistry.WorkerTemplate w2Template = w2Tpl.get();

        // Validation 1: Same Rarity
        if (targetTemplate.rarity() != w1Template.rarity() || targetTemplate.rarity() != w2Template.rarity()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                .name("§c§l✖ DIFFERENT RARITIES")
                .lore(
                    "§7Target and both ingredients must have",
                    "§7the exact same Rarity tier!",
                    "§7Current Target: §f" + targetTemplate.rarity().name(),
                    "§7Current w1: §f" + w1Template.rarity().name(),
                    "§7Current w2: §f" + w2Template.rarity().name()
                ).build());
            return;
        }

        // Validation 2: Same Profession
        if (!targetTemplate.profession().equalsIgnoreCase(w1Template.profession()) || !targetTemplate.profession().equalsIgnoreCase(w2Template.profession())) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                .name("§c§l✖ DIFFERENT PROFESSIONS")
                .lore(
                    "§7All three workers must have the",
                    "§7exact same Profession!",
                    "§7Current Target: §f" + targetTemplate.profession(),
                    "§7Current w1: §f" + w1Template.profession(),
                    "§7Current w2: §f" + w2Template.profession()
                ).build());
            return;
        }

        // Validation 3: Next Rarity tier exists
        ResourceRegistry.Rarity nextRarity = switch (targetTemplate.rarity()) {
            case COMMON -> ResourceRegistry.Rarity.UNCOMMON;
            case UNCOMMON -> ResourceRegistry.Rarity.RARE;
            case RARE -> ResourceRegistry.Rarity.EPIC;
            case EPIC -> ResourceRegistry.Rarity.LEGENDARY;
            case LEGENDARY -> ResourceRegistry.Rarity.MYTHIC;
            case MYTHIC -> null;
        };

        if (nextRarity == null) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                .name("§c§l✖ MAX TIER REACHED")
                .lore("§7Legendary workers cannot be fused further.").build());
            return;
        }

        // Genetic calculations preview (Average potential, no RNG)
        double avgSpeed = (targetWorker.getSpeedPotential() + ingredient1.getSpeedPotential() + ingredient2.getSpeedPotential()) / 3.0;
        double avgYield = (targetWorker.getYieldPotential() + ingredient1.getYieldPotential() + ingredient2.getYieldPotential()) / 3.0;
        double avgRare = (targetWorker.getRarePotential() + ingredient1.getRarePotential() + ingredient2.getRarePotential()) / 3.0;

        WorkerBiographyService bioService = workerService.getBiographyService();
        String spGrade = bioService.getPotentialGrade(avgSpeed);
        String ylGrade = bioService.getPotentialGrade(avgYield);
        String rrGrade = bioService.getPotentialGrade(avgRare);

        inventory.setItem(22, new ItemBuilder(Material.LIME_WOOL)
            .name("§a§l✔ CONFIRM FUSION")
            .lore(
                "§7Click to fuse the selected workers.",
                "§7Ingredients 1 and 2 will be consumed.",
                "",
                "§e§l【 TARGET PRODUCT PREVIEW 】",
                "§7Next Rarity Tier: §b" + nextRarity.name(),
                "§7Inherited Speed Potential: " + spGrade + " §8(" + String.format("%.2f", avgSpeed) + ")",
                "§7Inherited Yield Potential: " + ylGrade + " §8(" + String.format("%.2f", avgYield) + ")",
                "§7Inherited Rare Potential: " + rrGrade + " §8(" + String.format("%.2f", avgRare) + ")",
                "",
                "§a§l▶ Click to initiate Fusion Chamber!"
            ).build());
    }

    private boolean isWorkerSelected(UUID id) {
        return (targetWorker != null && targetWorker.getWorkerId().equals(id))
            || (ingredient1 != null && ingredient1.getWorkerId().equals(id))
            || (ingredient2 != null && ingredient2.getWorkerId().equals(id));
    }

    private ItemStack buildWorkerHeadItem(WorkerInstance w) {
        String rarityStr = "COMMON";
        String templateDisplayName = w.getTemplateId();
        if (registryManager != null) {
            var templateOpt = registryManager.getWorkerRegistry().getTemplate(w.getTemplateId());
            if (templateOpt.isPresent()) {
                rarityStr = templateOpt.get().rarity().name();
                templateDisplayName = org.bukkit.ChatColor.translateAlternateColorCodes('&', templateOpt.get().displayName());
            }
        }

        WorkerBiographyService bioService = workerService.getBiographyService();
        String speedGrade = bioService.getPotentialGrade(w.getSpeedPotential());
        String yieldGrade = bioService.getPotentialGrade(w.getYieldPotential());
        String rareGrade = bioService.getPotentialGrade(w.getRarePotential());

        String assignedStr = w.getAssignedNodeId() != null ? "§cAssigned to Node" : "§aIdle";

        return new ItemBuilder(Material.PLAYER_HEAD)
            .name((w.getCustomTitle() != null ? "§d" + w.getCustomTitle() + " " : "") + "§e" + templateDisplayName + " §8(Lv." + w.getLevel() + ")")
            .lore(
                "§7Rarity: §d" + rarityStr,
                "§7Serial: §bBID-" + String.format("%06d", w.getSerialId()),
                "§7Status: " + assignedStr,
                "",
                "§7Speed: " + speedGrade + " §8(" + String.format("%.2f", w.getSpeedPotential()) + ")",
                "§7Yield: " + yieldGrade + " §8(" + String.format("%.2f", w.getYieldPotential()) + ")",
                "§7Rare: " + rareGrade + " §8(" + String.format("%.2f", w.getRarePotential()) + ")"
            ).build();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 45) {
            player.openInventory(new WorkerManagementGUI(player, node, workerService, registryManager, nodeService, storageService, economyService, territoryService, onboardingService).getInventory());
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        // Handle deselecting slots
        if (slot == 10) {
            if (targetWorker != null) {
                targetWorker = null;
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.0f);
                populate();
            }
            return;
        }
        if (slot == 12) {
            if (ingredient1 != null) {
                ingredient1 = null;
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.0f);
                populate();
            }
            return;
        }
        if (slot == 14) {
            if (ingredient2 != null) {
                ingredient2 = null;
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.0f);
                populate();
            }
            return;
        }

        // Handle executing fusion
        if (slot == 22) {
            if (targetWorker == null || ingredient1 == null || ingredient2 == null) {
                player.sendMessage("§cPlease select all three workers first!");
                return;
            }
            
            // Execute fusion via service delegate
            boolean success = workerService.getFusionService().fuseWorkers(player, targetWorker, ingredient1, ingredient2);
            if (success) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                
                // Clear selection slots
                targetWorker = null;
                ingredient1 = null;
                ingredient2 = null;
                
                populate();
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            }
            return;
        }

        // Handle selecting worker from selection deck
        UUID clickedWorkerId = slotToWorkerMap.get(slot);
        if (clickedWorkerId != null) {
            Optional<WorkerInstance> workerOpt = workerService.getWorker(clickedWorkerId);
            if (workerOpt.isPresent()) {
                WorkerInstance w = workerOpt.get();
                
                // Seat in first open slot
                if (targetWorker == null) {
                    targetWorker = w;
                } else if (ingredient1 == null) {
                    ingredient1 = w;
                } else if (ingredient2 == null) {
                    ingredient2 = w;
                } else {
                    player.sendMessage("§cAll fusion slots are full! Deselect one by clicking it.");
                    return;
                }
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 1.0f, 1.0f);
                populate();
            }
        }
    }
}

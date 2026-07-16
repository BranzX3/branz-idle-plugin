package com.example.plugin.worker.fusion;

import com.example.plugin.worker.model.WorkerInstance;
import org.bukkit.entity.Player;

public interface WorkerFusionService {

    /**
     * Executes the fusion of three workers of the same rarity and profession.
     * Consumes w1 and w2 to upgrade target's rarity tier and inherit average potentials.
     */
    boolean fuseWorkers(Player player, WorkerInstance target, WorkerInstance w1, WorkerInstance w2);
}

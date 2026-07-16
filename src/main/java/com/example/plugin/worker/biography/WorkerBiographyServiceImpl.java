package com.example.plugin.worker.biography;

import com.example.plugin.worker.model.WorkerInstance;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WorkerBiographyServiceImpl implements WorkerBiographyService {

    private final JavaPlugin plugin;

    public WorkerBiographyServiceImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPotentialGrade(double potential) {
        if (potential >= 1.30) return "§d§lSS";
        if (potential >= 1.20) return "§6§lS";
        if (potential >= 1.10) return "§e§lA";
        if (potential >= 1.00) return "§a§lB";
        return "§7§lC";
    }

    @Override
    public String getPersonalityDisplayName(String personality) {
        String path = "worker_settings.personalities." + personality.toUpperCase() + ".display_name";
        return plugin.getConfig().getString(path, personality);
    }

    @Override
    public String getRandomDialogue(String personality) {
        String path = "worker_settings.personalities." + personality.toUpperCase() + ".dialogues";
        List<String> dialogues = plugin.getConfig().getStringList(path);
        if (dialogues.isEmpty()) {
            dialogues = new ArrayList<>();
            dialogues.add("Ready for work!");
        }
        int index = ThreadLocalRandom.current().nextInt(dialogues.size());
        return dialogues.get(index);
    }

    @Override
    public void updateWorkerTitle(WorkerInstance worker) {
        double sp = worker.getSpeedPotential();
        double yp = worker.getYieldPotential();
        double rp = worker.getRarePotential();

        if (sp >= rp && sp >= yp && sp >= 1.15) {
            worker.setCustomTitle("The Tireless");
        } else if (rp >= sp && rp >= yp && rp >= 1.30) {
            worker.setCustomTitle("The Lucky");
        } else if (worker.getLevel() >= 100) {
            worker.setCustomTitle("The Veteran");
        } else {
            worker.setCustomTitle(null);
        }
    }
}

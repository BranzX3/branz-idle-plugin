package com.example.plugin.worker.biography;

import com.example.plugin.worker.model.WorkerInstance;
import java.util.List;

public interface WorkerBiographyService {

    /**
     * Translates potential multipliers into cosmetic grades (SS, S, A, B, C).
     */
    String getPotentialGrade(double potential);

    /**
     * Retrieves the configured display name for an npc personality.
     */
    String getPersonalityDisplayName(String personality);

    /**
     * Picks a random dialogue line for an npc personality.
     */
    String getRandomDialogue(String personality);

    /**
     * Dynamically updates a worker's cosmetic title based on level and potentials.
     */
    void updateWorkerTitle(WorkerInstance worker);
}

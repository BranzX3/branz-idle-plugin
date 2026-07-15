package com.example.plugin.integration.provider;

import org.bukkit.Location;

/**
 * Abstraction layer for pasting architectural schematics representing node tiers onto the world.
 */
public interface StructureProvider {

    /**
     * Pastes a schematic corresponding to a style and tier level centered on the target location.
     *
     * @return true if successfully pasted, false if schematic missing or provider inactive
     */
    boolean pasteSchematic(String styleId, int level, Location targetLocation);
}

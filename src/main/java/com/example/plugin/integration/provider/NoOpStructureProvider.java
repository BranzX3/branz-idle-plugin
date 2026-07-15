package com.example.plugin.integration.provider;

import org.bukkit.Location;

/**
 * Fallback structure provider used when WorldEdit/FAWE is not installed.
 */
public class NoOpStructureProvider implements StructureProvider {

    @Override
    public boolean pasteSchematic(String styleId, int level, Location targetLocation) {
        return false;
    }
}

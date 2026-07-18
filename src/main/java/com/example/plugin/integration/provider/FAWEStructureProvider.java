package com.example.plugin.integration.provider;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;

/**
 * Structure provider implementation leveraging FastAsyncWorldEdit (FAWE) / WorldEdit to paste schematics.
 * Uses reflection for complete compile-time and runtime safety without direct class coupling.
 */
public class FAWEStructureProvider implements StructureProvider {

    private final JavaPlugin plugin;
    private final File schematicsDir;

    public FAWEStructureProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schematicsDir = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsDir.exists()) {
            schematicsDir.mkdirs();
        }
    }

    @Override
    public boolean pasteSchematic(String styleId, int level, Location targetLocation) {
        if (targetLocation == null || targetLocation.getWorld() == null) return false;

        String filename = level < 0 ? styleId : (styleId + "_lv" + level + ".schem");
        File schemFile = new File(schematicsDir, filename);
        if (!schemFile.exists()) {
            if (level >= 0) {
                // Check fallback .schematic
                schemFile = new File(schematicsDir, styleId + "_lv" + level + ".schematic");
            }
            if (!schemFile.exists()) {
                // Fallback to exact styleId string as filename match
                schemFile = new File(schematicsDir, styleId);
                if (!schemFile.exists()) {
                    return false;
                }
            }
        }

        try {
            Class<?> formatsClass = Class.forName("com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats");
            Method findByFileMethod = formatsClass.getMethod("findByFile", File.class);
            Object format = findByFileMethod.invoke(null, schemFile);
            if (format == null) return false;

            Method getReaderMethod = format.getClass().getMethod("getReader", java.io.InputStream.class);
            try (FileInputStream fis = new FileInputStream(schemFile)) {
                Object reader = getReaderMethod.invoke(format, fis);
                Method readMethod = reader.getClass().getMethod("read");
                Object clipboard = readMethod.invoke(reader);

                Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                Method adaptWorldMethod = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class);
                Object weWorld = adaptWorldMethod.invoke(null, targetLocation.getWorld());

                Class<?> blockVectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3");
                Method atMethod = blockVectorClass.getMethod("at", double.class, double.class, double.class);
                Object vector = atMethod.invoke(null, targetLocation.getX(), targetLocation.getY(), targetLocation.getZ());

                Class<?> sessionClass = Class.forName("com.sk89q.worldedit.EditSession");
                Class<?> weClass = Class.forName("com.sk89q.worldedit.WorldEdit");
                Object weInstance = weClass.getMethod("getInstance").invoke(null);
                Object editSessionFactory = weClass.getMethod("getEditSessionFactory").invoke(weInstance);
                Method createSessionMethod = editSessionFactory.getClass().getMethod("getEditSession", com.sk89q.worldedit.world.World.class, int.class);
                Object editSession = createSessionMethod.invoke(editSessionFactory, weWorld, -1);

                Class<?> holderClass = Class.forName("com.sk89q.worldedit.session.ClipboardHolder");
                Object holder = holderClass.getConstructor(clipboard.getClass()).newInstance(clipboard);

                Method createPasteMethod = holder.getClass().getMethod("createPaste", sessionClass);
                Object operationBuilder = createPasteMethod.invoke(holder, editSession);

                Method toMethod = operationBuilder.getClass().getMethod("to", blockVectorClass);
                operationBuilder = toMethod.invoke(operationBuilder, vector);

                Method ignoreAirMethod = operationBuilder.getClass().getMethod("ignoreAirBlocks", boolean.class);
                operationBuilder = ignoreAirMethod.invoke(operationBuilder, true);

                Method buildMethod = operationBuilder.getClass().getMethod("build");
                Object operation = buildMethod.invoke(operationBuilder);

                Class<?> operationsClass = Class.forName("com.sk89q.worldedit.function.operation.Operations");
                Method completeMethod = operationsClass.getMethod("complete", com.sk89q.worldedit.function.operation.Operation.class);
                completeMethod.invoke(null, operation);

                Method closeMethod = sessionClass.getMethod("close");
                closeMethod.invoke(editSession);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[FAWEStructureProvider] Failed to paste schematic " + filename + ": " + e.getMessage());
            return false;
        }
    }
}

package com.example.plugin.territory.model;

import java.util.Objects;

/**
 * Value object representing 2D chunk coordinates.
 * Supports efficient 64-bit bitwise packing for ConcurrentHashMap keys.
 */
public class ChunkCoord {

    private final int x;
    private final int z;

    public ChunkCoord(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    /**
     * Packs the X and Z chunk coordinates into a unique 64-bit long key.
     *
     * @return packed coordinate key
     */
    public long toPackedLong() {
        return toPackedLong(x, z);
    }

    /**
     * Static utility to pack X and Z chunk coordinates into a unique 64-bit long.
     */
    public static long toPackedLong(int x, int z) {
        return (((long) x) << 32) | (z & 0xFFFFFFFFL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkCoord that = (ChunkCoord) o;
        return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return "ChunkCoord{" + "x=" + x + ", z=" + z + '}';
    }
}

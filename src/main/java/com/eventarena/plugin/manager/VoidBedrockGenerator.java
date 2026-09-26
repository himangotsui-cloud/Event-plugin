package com.eventarena.plugin.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * Guarantees the event world is nothing but a single bedrock layer with pure
 * void above it - no grass, no stone, no caves, no structures, no mobs.
 *
 * We use this instead of the vanilla "flat" generator-settings JSON preset
 * because that string-based preset depends on the server correctly parsing
 * it into a FlatLevelSource, and in practice that can silently fall back to
 * normal terrain generation. A custom ChunkGenerator removes that ambiguity
 * entirely: every phase except generateBedrock() is explicitly turned off,
 * so there is nothing else that could generate.
 */
public class VoidBedrockGenerator extends ChunkGenerator {

    @Override
    public void generateBedrock(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        int floorY = worldInfo.getMinHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunkData.setBlock(x, floorY, z, Material.BEDROCK);
            }
        }
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, world.getMinHeight() + 2, 0.5);
    }
}

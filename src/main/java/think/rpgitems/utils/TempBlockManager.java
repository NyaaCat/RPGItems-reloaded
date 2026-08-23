package think.rpgitems.utils;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import think.rpgitems.RPGItems;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Tracks real blocks that a power places temporarily (e.g. Torch, Rainbow) so they can be:
 * <ul>
 *     <li>reverted back to whatever was there before, on demand;</li>
 *     <li>protected from being broken/exploded/burned/pushed while active, see the guards in
 *     {@code think.rpgitems.Events};</li>
 *     <li>cleaned up automatically if the server crashes before the power got a chance to revert
 *     them itself, via a small record persisted on the owning chunk's PersistentDataContainer.</li>
 * </ul>
 * The chunk PDC is written in the same disk write as the block change itself (both live in the same
 * region file), so the tracking record can never survive on disk without the block, or vice versa.
 */
public final class TempBlockManager {
    private static final NamespacedKey ROOT = new NamespacedKey(RPGItems.plugin, "tempBlocks");

    private static final Map<Location, BlockData> active = new HashMap<>();

    /**
     * Overwrites {@code block} with {@code data}, remembering its current contents so it can be
     * reverted later.
     */
    public static void place(Block block, BlockData data) {
        BlockData original = block.getBlockData();
        block.setBlockData(data, false);
        track(block, original);
    }

    /**
     * Starts tracking a block that has already been changed to its temporary appearance (e.g. by a
     * vanilla falling-block landing), remembering {@code original} as what it should revert to.
     */
    public static void track(Block block, BlockData original) {
        Location loc = key(block);
        active.put(loc, original);
        persist(block.getChunk(), loc, original);
    }

    public static boolean isTracked(Location loc) {
        return active.containsKey(normalize(loc));
    }

    /**
     * Reverts a tracked block back to its original contents. No-op if {@code loc} isn't tracked.
     */
    public static void revert(Location loc) {
        Location norm = normalize(loc);
        BlockData original = active.remove(norm);
        if (original == null) {
            return;
        }
        norm.getBlock().setBlockData(original, false);
        unpersist(norm.getChunk(), norm);
    }

    /**
     * Reverts any temp blocks left over in this chunk from a crash (or an unclean shutdown) that
     * happened before the owning power could revert them itself.
     */
    public static void cleanupChunk(Chunk chunk) {
        PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();
        if (!chunkPdc.has(ROOT, PersistentDataType.TAG_CONTAINER)) {
            return;
        }
        PersistentDataContainer root = chunkPdc.get(ROOT, PersistentDataType.TAG_CONTAINER);
        if (root == null) {
            chunkPdc.remove(ROOT);
            return;
        }
        World world = chunk.getWorld();
        for (NamespacedKey entryKey : new HashSet<>(root.getKeys())) {
            String dataString = root.get(entryKey, PersistentDataType.STRING);
            if (dataString == null) {
                continue;
            }
            int[] pos = parseEntryKey(entryKey.getKey());
            if (pos == null) {
                continue;
            }
            try {
                BlockData original = Bukkit.createBlockData(dataString);
                world.getBlockAt(pos[0], pos[1], pos[2]).setBlockData(original, false);
            } catch (IllegalArgumentException ignored) {
                // Original block data no longer parses (e.g. removed in a newer version); leave the
                // block as-is rather than crash the load.
            }
        }
        chunkPdc.remove(ROOT);
    }

    /**
     * Sweeps every already-loaded chunk in every world, in case the server crashed while chunks with
     * pending temp blocks were already loaded (so no {@code ChunkLoadEvent} will fire for them again).
     * Call once, when the server has finished starting up.
     */
    public static void cleanupLoadedChunks() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                cleanupChunk(chunk);
            }
        }
    }

    private static void persist(Chunk chunk, Location loc, BlockData original) {
        PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();
        PersistentDataContainer root = chunkPdc.has(ROOT, PersistentDataType.TAG_CONTAINER)
                ? chunkPdc.get(ROOT, PersistentDataType.TAG_CONTAINER)
                : chunkPdc.getAdapterContext().newPersistentDataContainer();
        root.set(entryKey(loc), PersistentDataType.STRING, original.getAsString());
        chunkPdc.set(ROOT, PersistentDataType.TAG_CONTAINER, root);
    }

    private static void unpersist(Chunk chunk, Location loc) {
        PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();
        if (!chunkPdc.has(ROOT, PersistentDataType.TAG_CONTAINER)) {
            return;
        }
        PersistentDataContainer root = chunkPdc.get(ROOT, PersistentDataType.TAG_CONTAINER);
        if (root == null) {
            return;
        }
        root.remove(entryKey(loc));
        if (root.getKeys().isEmpty()) {
            chunkPdc.remove(ROOT);
        } else {
            chunkPdc.set(ROOT, PersistentDataType.TAG_CONTAINER, root);
        }
    }

    private static NamespacedKey entryKey(Location loc) {
        return new NamespacedKey(RPGItems.plugin, loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ());
    }

    private static int[] parseEntryKey(String key) {
        String[] parts = key.split("_");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Location key(Block block) {
        return new Location(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    private static Location normalize(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}

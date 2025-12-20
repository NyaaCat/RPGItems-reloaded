package think.rpgitems.power;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for entity searches to reduce overhead in AOE powers.
 * Entity searches are expensive and often redundant when multiple powers
 * trigger in the same tick at similar locations.
 *
 * Cache entries expire after a configurable TTL (default: 2 ticks = 100ms).
 */
public class EntitySearchCache {
    private static final EntitySearchCache INSTANCE = new EntitySearchCache();

    /**
     * Default TTL in milliseconds (2 ticks at 20 TPS)
     */
    private static final long DEFAULT_TTL_MS = 100;

    /**
     * Cache key combines world, location (bucketed), and radius
     */
    private final Map<EntitySearchKey, CachedEntitySearch> entityCache = new ConcurrentHashMap<>();

    /**
     * Cache for line-of-sight checks. Key is player UUID + entity UUID.
     * Expires each tick.
     */
    private final Map<LOSKey, CachedLOS> losCache = new ConcurrentHashMap<>();

    /**
     * Last cleanup tick
     */
    private long lastCleanupTick = 0;

    private EntitySearchCache() {
    }

    public static EntitySearchCache getInstance() {
        return INSTANCE;
    }

    /**
     * Gets cached nearby entities or performs fresh search.
     *
     * @param world  the world to search in
     * @param center the center location
     * @param dx     x radius
     * @param dy     y radius
     * @param dz     z radius
     * @return collection of nearby entities
     */
    public Collection<Entity> getNearbyEntities(World world, Location center, double dx, double dy, double dz) {
        long currentTime = System.currentTimeMillis();
        maybeCleanup(currentTime);

        EntitySearchKey key = new EntitySearchKey(world.getUID(), center, dx, dy, dz);

        CachedEntitySearch cached = entityCache.get(key);
        if (cached != null && !cached.isExpired(currentTime)) {
            return cached.entities;
        }

        // Perform the actual search
        Collection<Entity> entities = world.getNearbyEntities(center, dx, dy, dz);
        List<Entity> entityList = new ArrayList<>(entities);

        entityCache.put(key, new CachedEntitySearch(entityList, currentTime + DEFAULT_TTL_MS));
        return entityList;
    }

    /**
     * Gets cached line-of-sight result or performs fresh check.
     *
     * @param player the player checking LOS
     * @param target the target entity
     * @return true if player has line of sight to target
     */
    public boolean hasLineOfSight(Player player, LivingEntity target) {
        long currentTime = System.currentTimeMillis();

        LOSKey key = new LOSKey(player.getUniqueId(), target.getUniqueId());

        CachedLOS cached = losCache.get(key);
        if (cached != null && !cached.isExpired(currentTime)) {
            return cached.hasLOS;
        }

        // Perform the actual LOS check
        boolean hasLOS = player.hasLineOfSight(target);

        // LOS cache expires faster (1 tick = 50ms) since entities move
        losCache.put(key, new CachedLOS(hasLOS, currentTime + 50));
        return hasLOS;
    }

    /**
     * Clears all cached data.
     * Call on plugin reload/disable.
     */
    public void clearAll() {
        entityCache.clear();
        losCache.clear();
    }

    /**
     * Cleanup expired entries periodically (every ~10 ticks)
     */
    private void maybeCleanup(long currentTime) {
        // Cleanup every ~500ms (10 ticks)
        if (currentTime - lastCleanupTick > 500) {
            lastCleanupTick = currentTime;
            entityCache.entrySet().removeIf(e -> e.getValue().isExpired(currentTime));
            losCache.entrySet().removeIf(e -> e.getValue().isExpired(currentTime));
        }
    }

    /**
     * Key for entity search cache.
     * Buckets location to grid cells for better cache hits.
     */
    private static class EntitySearchKey {
        private final UUID worldId;
        private final int bucketX;
        private final int bucketY;
        private final int bucketZ;
        private final int radiusBucket;

        EntitySearchKey(UUID worldId, Location center, double dx, double dy, double dz) {
            this.worldId = worldId;
            // Bucket locations to 4-block grid for better cache hits
            this.bucketX = (int) Math.floor(center.getX() / 4.0);
            this.bucketY = (int) Math.floor(center.getY() / 4.0);
            this.bucketZ = (int) Math.floor(center.getZ() / 4.0);
            // Bucket radius to nearest 2 blocks
            this.radiusBucket = (int) Math.ceil(Math.max(dx, Math.max(dy, dz)) / 2.0);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntitySearchKey that = (EntitySearchKey) o;
            return bucketX == that.bucketX &&
                    bucketY == that.bucketY &&
                    bucketZ == that.bucketZ &&
                    radiusBucket == that.radiusBucket &&
                    worldId.equals(that.worldId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldId, bucketX, bucketY, bucketZ, radiusBucket);
        }
    }

    /**
     * Cached entity search result.
     */
    private static class CachedEntitySearch {
        final List<Entity> entities;
        final long expiresAt;

        CachedEntitySearch(List<Entity> entities, long expiresAt) {
            this.entities = entities;
            this.expiresAt = expiresAt;
        }

        boolean isExpired(long currentTime) {
            return currentTime > expiresAt;
        }
    }

    /**
     * Key for LOS cache.
     */
    private static class LOSKey {
        private final UUID playerId;
        private final UUID targetId;

        LOSKey(UUID playerId, UUID targetId) {
            this.playerId = playerId;
            this.targetId = targetId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LOSKey losKey = (LOSKey) o;
            return playerId.equals(losKey.playerId) && targetId.equals(losKey.targetId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerId, targetId);
        }
    }

    /**
     * Cached LOS result.
     */
    private static class CachedLOS {
        final boolean hasLOS;
        final long expiresAt;

        CachedLOS(boolean hasLOS, long expiresAt) {
            this.hasLOS = hasLOS;
            this.expiresAt = expiresAt;
        }

        boolean isExpired(long currentTime) {
            return currentTime > expiresAt;
        }
    }
}

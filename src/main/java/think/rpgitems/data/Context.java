package think.rpgitems.data;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Context {
    private static final Context instance = new Context();
    private final HashMap<UUID, ExpiringMap<String, Object>> storage = new HashMap<>();

    public static Context instance() {
        return instance;
    }

    /**
     * @return monotonic current millis
     */
    public static long getCurrentMillis() {
        return System.nanoTime() / 1000000L;
    }

    public LivingEntity getLivingEntity(UUID context, String key) {
        ExpiringMap<String, Object> local = storage.get(context);
        if (local == null) return null;
        Object obj = local.get(key);
        if (obj instanceof LivingEntity) {
            return (LivingEntity) obj;
        }
        return null;
    }

    public Boolean getBoolean(UUID context, String key) {
        ExpiringMap<String, Object> local = storage.get(context);
        if (local == null) return null;
        Object obj = local.get(key);
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return null;
    }

    public Double getDouble(UUID context, String key) {
        ExpiringMap<String, Object> local = storage.get(context);
        if (local == null) return null;
        Object obj = local.get(key);
        if (obj instanceof Double) {
            return (Double) obj;
        }
        return null;
    }

    public Location getLocation(UUID context, String key) {
        ExpiringMap<String, Object> local = storage.get(context);
        if (local == null) return null;
        Object obj = local.get(key);
        if (obj instanceof Location) {
            return (Location) obj;
        }
        return null;
    }

    public Object get(UUID context, String key) {
        ExpiringMap<String, Object> local = storage.get(context);
        if (local == null) return null;
        return local.get(key);
    }

    public void put(UUID context, String key, Object obj) {
        storage.computeIfAbsent(context, (ignored) -> new ExpiringMap<>()).put(key, obj);
    }

    public void putTemp(UUID context, String key, Object obj) {
        storage.computeIfAbsent(context, (ignored) -> new ExpiringMap<>()).putTemp(key, obj);
    }

    public void removeTemp(UUID context, String key) {
        storage.computeIfAbsent(context, uuid -> new ExpiringMap<>()).remove(key);
    }

    public void put(UUID context, String key, Object obj, long expire) {
        storage.computeIfAbsent(context, (ignored) -> new ExpiringMap<>()).put(key, obj, expire);
    }

    public void putExpiringSeconds(UUID context, String key, Object obj, int expiringSeconds) {
        put(context, key, obj, getCurrentMillis() + expiringSeconds * 10);
    }

    public void cleanTemp(UUID context) {
        ExpiringMap<String, Object> local = storage.get(context);
        if (local == null) return;
        local.cleanupTemp();
    }

    public void cleanTick() {
        for (ExpiringMap<String, Object> local : storage.values()) {
            local.cleanup(null, null, ExpiringMap.TICK);
        }
    }

    public class ExpiringMap<K, V> implements Map<K, V> {
        private static final long TEMP = Long.MAX_VALUE;
        private static final long TICK = Long.MAX_VALUE - 1;
        private final int aliveAge;
        private final HashMap<K, Long> birth = new HashMap<>();
        private final HashMap<K, V> inner;

        public ExpiringMap() {
            this(0);
        }

        public ExpiringMap(int aliveAge) {
            inner = new HashMap<>();
            this.aliveAge = aliveAge;
        }

        public ExpiringMap(int initialCapacity, int aliveAge) {
            inner = new HashMap<>(initialCapacity);
            this.aliveAge = aliveAge;
        }

        private void rec(K i, long birth) {
            this.birth.put(i, birth);
            this.cleanup();
        }

        private void rec(K key) {
            long currentMillis = getCurrentMillis();
            rec(key, currentMillis);
        }

        public void cleanup() {
            cleanup(null, null, null);
        }

        public void cleanupTemp() {
            cleanup(null, null, TEMP);
        }

        @Nullable
        public Pair<K, V> cleanup(Object findKey, Object findValue, Long removing) {
            long currentMillis = getCurrentMillis();
            Iterator<Map.Entry<K, Long>> expireIter = this.birth.entrySet().iterator();

            while (expireIter.hasNext()) {
                Map.Entry<K, Long> entry = expireIter.next();
                K key = entry.getKey();
                V value = inner.get(key);
                Long birth = entry.getValue();

                if ((currentMillis - birth <= (long) this.aliveAge)
                        && !Objects.equals(birth, removing)) {
                    if (Objects.equals(findKey, key)) {
                        return Pair.of(key, value);
                    }
                    if (value != null && Objects.equals(findValue, value)) {
                        return Pair.of(key, value);
                    }
                    continue;
                }

                inner.remove(key);
                expireIter.remove();
            }
            return null;
        }

        @Override
        public int size() {
            this.cleanup();
            return inner.size();
        }

        @Override
        public boolean isEmpty() {
            this.cleanup();
            return inner.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return this.cleanup(key, null, null) == null;
        }

        @Override
        public boolean containsValue(Object value) {
            return this.cleanup(null, value, null) == null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V get(Object key) {
            Pair<K, V> cleanup = this.cleanup(key, null, null);
            if (cleanup == null) {
                return null;
            }
            return cleanup.getValue();
        }

        @Override
        public V put(K i, V value) {
            this.rec(i);
            return inner.put(i, value);
        }

        public V put(K i, V value, long birth) {
            this.rec(i, birth);
            return inner.put(i, value);
        }

        public V putTemp(K i, V value) {
            this.rec(i, TEMP);
            return inner.put(i, value);
        }

        @Override
        public V remove(Object key) {
            birth.remove(key);
            return inner.remove(key);
        }

        @Override
        public void putAll(@Nonnull Map<? extends K, ? extends V> map) {
            long currentMillis = getCurrentMillis();
            birth.putAll(map.keySet().stream().collect(Collectors.toMap(e -> e, e -> currentMillis)));
            inner.putAll(map);
            cleanup();
        }

        @Override
        public void clear() {
            birth.clear();
            inner.clear();
        }

        @Override
        @Nonnull
        public Set<K> keySet() {
            cleanup();
            return birth.keySet();
        }

        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> value) {
            this.birth.put(key, getCurrentMillis());
            return inner.computeIfAbsent(key, value);
        }

        @Override
        @Nonnull
        public Collection<V> values() {
            this.cleanup();
            return inner.values();
        }

        @Override
        @Nonnull
        public Set<Entry<K, V>> entrySet() {
            this.cleanup();
            return inner.entrySet();
        }
    }

}

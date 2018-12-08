package think.rpgitems.utils;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagAdapterContext;
import org.bukkit.inventory.meta.tags.ItemTagType;
import think.rpgitems.power.Utils;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ItemTagUtils {

    public static final ItemTagType<byte[], UUID> BA_UUID = new UUIDItemTagType();
    public static final ItemTagType<byte[], OfflinePlayer> BA_OFFLINE_PLAYER = new OfflinePlayerItemTagType();

    private ItemTagUtils() {
        throw new IllegalStateException();
    }

    public static <T, Z> Z computeIfAbsent(CustomItemTagContainer container, NamespacedKey key, ItemTagType<T, Z> type, Supplier<? extends Z> mappingFunction) {
        return computeIfAbsent(container, key, type, (ignored) -> mappingFunction.get());
    }

    public static <T, Z> Z computeIfAbsent(CustomItemTagContainer container, NamespacedKey key, ItemTagType<T, Z> type, Function<NamespacedKey, ? extends Z> mappingFunction) {
        Z value = container.getCustomTag(key, type);
        if (value == null) {
            value = mappingFunction.apply(key);
        }
        container.setCustomTag(key, type, value);
        return value;
    }

    public static <T, Z> Z putIfAbsent(CustomItemTagContainer container, NamespacedKey key, ItemTagType<T, Z> type, Supplier<? extends Z> mappingFunction) {
        return computeIfAbsent(container, key, type, (ignored) -> mappingFunction.get());
    }

    public static <T, Z> Z putIfAbsent(CustomItemTagContainer container, NamespacedKey key, ItemTagType<T, Z> type, Function<NamespacedKey, ? extends Z> mappingFunction) {
        Z old = container.getCustomTag(key, type);
        if (old == null) {
            container.setCustomTag(key, type, mappingFunction.apply(key));
            return null;
        }
        return old;
    }

    public static <T, Z> Z putValueIfAbsent(CustomItemTagContainer container, NamespacedKey key, ItemTagType<T, Z> type, Z value) {
        return putIfAbsent(container, key, type, (ignored) -> value);
    }

    public static Byte getByte(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, ItemTagType.BYTE);
    }

    public static Short getShort(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, ItemTagType.SHORT);
    }

    public static Integer getInt(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, ItemTagType.INTEGER);
    }

    public static Long getLong(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, ItemTagType.LONG);
    }

    public static Float getFloat(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, ItemTagType.FLOAT);
    }

    public static Double getDouble(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, ItemTagType.DOUBLE);
    }

    public static String getString(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, ItemTagType.STRING);
    }

    public static byte[] getByteArray(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, ItemTagType.BYTE_ARRAY);
    }

    public static int[] getIntArray(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, ItemTagType.INTEGER_ARRAY);
    }

    public static long[] getLongArray(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, ItemTagType.LONG_ARRAY);
    }

    public static UUID getUUID(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, BA_UUID);
    }

    public static CustomItemTagContainer getTag(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, ItemTagType.TAG_CONTAINER);
    }

    public static OfflinePlayer getPlayer(CustomItemTagContainer container, NamespacedKey key) {
        return container.getCustomTag(key, BA_OFFLINE_PLAYER);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, byte value) {
        container.setCustomTag(key, ItemTagType.BYTE, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, short value) {
        container.setCustomTag(key, ItemTagType.SHORT, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, int value) {
        container.setCustomTag(key, ItemTagType.INTEGER, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, long value) {
        container.setCustomTag(key, ItemTagType.LONG, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, float value) {
        container.setCustomTag(key, ItemTagType.FLOAT, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, double value) {
        container.setCustomTag(key, ItemTagType.DOUBLE, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, String value) {
        container.setCustomTag(key, ItemTagType.STRING, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, byte[] value) {
        container.setCustomTag(key, ItemTagType.BYTE_ARRAY, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, int[] value) {
        container.setCustomTag(key, ItemTagType.INTEGER_ARRAY, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, long[] value) {
        container.setCustomTag(key, ItemTagType.LONG_ARRAY, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, UUID value) {
        container.setCustomTag(key, BA_UUID, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, OfflinePlayer value) {
        container.setCustomTag(key, BA_OFFLINE_PLAYER, value);
    }

    public static CustomItemTagContainer makeTag(CustomItemTagContainer container, NamespacedKey key) {
        return computeIfAbsent(container, key, ItemTagType.TAG_CONTAINER, (k) -> container.getAdapterContext().newTagContainer());
    }

    public static CustomItemTagContainer makeTag(ItemMeta itemMeta, NamespacedKey key) {
        @SuppressWarnings("deprecation") CustomItemTagContainer container = itemMeta.getCustomTagContainer();
        return makeTag(container, key);
    }

    public static class UUIDItemTagType implements ItemTagType<byte[], UUID> {
        @Override
        public Class<byte[]> getPrimitiveType() {
            return byte[].class;
        }

        @Override
        public Class<UUID> getComplexType() {
            return UUID.class;
        }

        @Override
        public byte[] toPrimitive(UUID complex, ItemTagAdapterContext context) {
            return Utils.decodeUUID(complex);
        }

        @Override
        public UUID fromPrimitive(byte[] primitive, ItemTagAdapterContext context) {
            return Utils.encodeUUID(primitive);
        }
    }

    public static class OfflinePlayerItemTagType implements ItemTagType<byte[], OfflinePlayer> {
        @Override
        public Class<byte[]> getPrimitiveType() {
            return byte[].class;
        }

        @Override
        public Class<OfflinePlayer> getComplexType() {
            return OfflinePlayer.class;
        }

        @Override
        public byte[] toPrimitive(OfflinePlayer complex, ItemTagAdapterContext context) {
            return Utils.decodeUUID(complex.getUniqueId());
        }

        @Override
        public OfflinePlayer fromPrimitive(byte[] primitive, ItemTagAdapterContext context) {
            return Bukkit.getOfflinePlayer(Utils.encodeUUID(primitive));
        }
    }
}

package think.rpgitems.utils;

import com.google.common.base.FinalizablePhantomReference;
import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagAdapterContext;
import org.bukkit.inventory.meta.tags.ItemTagType;
import think.rpgitems.RPGItems;
import think.rpgitems.power.Utils;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

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

    public static void set(CustomItemTagContainer container, NamespacedKey key, CustomItemTagContainer value) {
        container.setCustomTag(key, ItemTagType.TAG_CONTAINER, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, UUID value) {
        container.setCustomTag(key, BA_UUID, value);
    }

    public static void set(CustomItemTagContainer container, NamespacedKey key, OfflinePlayer value) {
        container.setCustomTag(key, BA_OFFLINE_PLAYER, value);
    }

    public static SubItemTagContainer makeTag(CustomItemTagContainer container, NamespacedKey key) {
        SubItemTagContainer subItemTagContainer = new SubItemTagContainer(container, key, computeIfAbsent(container, key, ItemTagType.TAG_CONTAINER, (k) -> container.getAdapterContext().newTagContainer()));
        WeakReference<CustomItemTagContainer> weakParent = new WeakReference<>(container);
        FinalizablePhantomReference<SubItemTagContainer> reference = new FinalizablePhantomReference<SubItemTagContainer>(subItemTagContainer, SubItemTagContainer.frq) {

            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

            public void finalizeReferent() {
                if (SubItemTagContainer.references.remove(this)) {
                    RPGItems.logger.severe("Unhandled SubItemTagContainer found: " + key + "@" + weakParent.get());
                    for (StackTraceElement stackTraceElement : stackTrace) {
                        RPGItems.logger.warning(stackTraceElement.toString());
                    }
                }
            }
        };
        subItemTagContainer.setReference(reference);
        SubItemTagContainer.references.add(reference);
        return subItemTagContainer;
    }

    public static SubItemTagContainer makeTag(ItemMeta itemMeta, NamespacedKey key) {
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

    public static class SubItemTagContainer implements CustomItemTagContainer {
        private CustomItemTagContainer parent;
        private CustomItemTagContainer self;
        private NamespacedKey key;
        private PhantomReference<SubItemTagContainer> reference;

        private static FinalizableReferenceQueue frq = new FinalizableReferenceQueue();
        private static final Set<Reference<?>> references = Sets.newConcurrentHashSet();

        private SubItemTagContainer(CustomItemTagContainer parent, NamespacedKey key, CustomItemTagContainer self) {
            this.parent = parent;
            this.self = self;
            this.key = key;
        }

        @Override
        public <T, Z> void setCustomTag(NamespacedKey namespacedKey, ItemTagType<T, Z> itemTagType, Z z) {
            self.setCustomTag(namespacedKey, itemTagType, z);
        }

        @Override
        public <T, Z> boolean hasCustomTag(NamespacedKey namespacedKey, ItemTagType<T, Z> itemTagType) {
            return self.hasCustomTag(namespacedKey, itemTagType);
        }

        @Override
        public <T, Z> Z getCustomTag(NamespacedKey namespacedKey, ItemTagType<T, Z> itemTagType) {
            return self.getCustomTag(namespacedKey, itemTagType);
        }

        @Override
        public void removeCustomTag(NamespacedKey namespacedKey) {
            self.removeCustomTag(namespacedKey);
        }

        @Override
        public boolean isEmpty() {
            return self.isEmpty();
        }

        @Override
        public ItemTagAdapterContext getAdapterContext() {
            return self.getAdapterContext();
        }

        public void commit() {
            set(parent, key, self);
            if (parent instanceof SubItemTagContainer) {
                ((SubItemTagContainer) parent).commit();
            }
            dispose();
        }

        public void dispose() {
            self = null;
            if (!SubItemTagContainer.references.remove(reference)) {
                RPGItems.logger.log(Level.SEVERE, "Double handled SubItemTagContainer found: " + this + ": " + key + "@" + parent);
            }
        }

        private void setReference(FinalizablePhantomReference<SubItemTagContainer> reference) {
            this.reference = reference;
        }
    }
}

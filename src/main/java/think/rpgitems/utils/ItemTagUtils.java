package think.rpgitems.utils;

import cat.nyaa.nyaacore.utils.ItemStackUtils;
import com.google.common.base.FinalizablePhantomReference;
import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.collect.Sets;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import think.rpgitems.RPGItems;
import think.rpgitems.power.PowerManager;
import think.rpgitems.power.Utils;

public final class ItemTagUtils {

  public static final PersistentDataType<byte[], UUID> BA_UUID = new UUIDPersistentDataType();
  public static final PersistentDataType<Byte, Boolean> BYTE_BOOLEAN =
      new BooleanPersistentDataType();
  public static final PersistentDataType<byte[], OfflinePlayer> BA_OFFLINE_PLAYER =
      new OfflinePlayerPersistentDataType();
  public static final PersistentDataType<String, ItemStack> STRING_ITEMSTACK =
      new ItemStackPersistentDataType();

  private ItemTagUtils() {
    throw new IllegalStateException();
  }

  public static <T, Z> Z computeIfAbsent(
      PersistentDataContainer container,
      NamespacedKey key,
      PersistentDataType<T, Z> type,
      Supplier<? extends Z> mappingFunction) {
    return computeIfAbsent(container, key, type, (ignored) -> mappingFunction.get());
  }

  public static <T, Z> Z computeIfAbsent(
      PersistentDataContainer container,
      NamespacedKey key,
      PersistentDataType<T, Z> type,
      Function<NamespacedKey, ? extends Z> mappingFunction) {
    Z value = container.get(key, type);
    if (value == null) {
      value = mappingFunction.apply(key);
    }
    container.set(key, type, value);
    return value;
  }

  public static <T, Z> Z putIfAbsent(
      PersistentDataContainer container,
      NamespacedKey key,
      PersistentDataType<T, Z> type,
      Supplier<? extends Z> mappingFunction) {
    return computeIfAbsent(container, key, type, (ignored) -> mappingFunction.get());
  }

  public static <T, Z> Z putIfAbsent(
      PersistentDataContainer container,
      NamespacedKey key,
      PersistentDataType<T, Z> type,
      Function<NamespacedKey, ? extends Z> mappingFunction) {
    Z old = container.get(key, type);
    if (old == null) {
      container.set(key, type, mappingFunction.apply(key));
      return null;
    }
    return old;
  }

  public static <T, Z> Z putValueIfAbsent(
      PersistentDataContainer container,
      NamespacedKey key,
      PersistentDataType<T, Z> type,
      Z value) {
    return putIfAbsent(container, key, type, (ignored) -> value);
  }

  public static Boolean getBoolean(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, BYTE_BOOLEAN);
  }

  public static Optional<Boolean> optBoolean(PersistentDataContainer container, NamespacedKey key) {
    if (!container.has(key, BYTE_BOOLEAN)) return Optional.empty();
    return Optional.ofNullable(container.get(key, BYTE_BOOLEAN));
  }

  public static Byte getByte(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, PersistentDataType.BYTE);
  }

  public static Short getShort(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, PersistentDataType.SHORT);
  }

  public static Integer getInt(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, PersistentDataType.INTEGER);
  }

  public static OptionalInt optInt(PersistentDataContainer container, NamespacedKey key) {
    if (!container.has(key, PersistentDataType.INTEGER)) return OptionalInt.empty();
    return OptionalInt.of(container.get(key, PersistentDataType.INTEGER));
  }

  public static Long getLong(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, PersistentDataType.LONG);
  }

  public static Float getFloat(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, PersistentDataType.FLOAT);
  }

  public static Double getDouble(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, PersistentDataType.DOUBLE);
  }

  public static String getString(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, PersistentDataType.STRING);
  }

  public static String getString(PersistentDataContainer container, String key) {
    return container.get(PowerManager.parseKey(key), PersistentDataType.STRING);
  }

  public static byte[] getByteArray(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, PersistentDataType.BYTE_ARRAY);
  }

  public static int[] getIntArray(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, PersistentDataType.INTEGER_ARRAY);
  }

  public static long[] getLongArray(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, PersistentDataType.LONG_ARRAY);
  }

  public static UUID getUUID(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, BA_UUID);
  }

  public static Optional<UUID> optUUID(PersistentDataContainer container, NamespacedKey key) {
    return Optional.ofNullable(container.get(key, BA_UUID));
  }

  public static PersistentDataContainer getTag(
      PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, PersistentDataType.TAG_CONTAINER);
  }

  public static OfflinePlayer getPlayer(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, BA_OFFLINE_PLAYER);
  }

  public static ItemStack getItemStack(PersistentDataContainer container, NamespacedKey key) {
    return container.get(key, STRING_ITEMSTACK);
  }

  public static ItemStack getItemStack(PersistentDataContainer container, String key) {
    return getItemStack(container, PowerManager.parseKey(key));
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, boolean value) {
    container.set(key, BYTE_BOOLEAN, value);
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, byte value) {
    container.set(key, PersistentDataType.BYTE, value);
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, short value) {
    container.set(key, PersistentDataType.SHORT, value);
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, int value) {
    container.set(key, PersistentDataType.INTEGER, value);
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, long value) {
    container.set(key, PersistentDataType.LONG, value);
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, float value) {
    container.set(key, PersistentDataType.FLOAT, value);
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, double value) {
    container.set(key, PersistentDataType.DOUBLE, value);
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, String value) {
    container.set(key, PersistentDataType.STRING, value);
  }

  public static void set(PersistentDataContainer container, String key, String value) {
    container.set(PowerManager.parseKey(key), PersistentDataType.STRING, value);
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, byte[] value) {
    container.set(key, PersistentDataType.BYTE_ARRAY, value);
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, int[] value) {
    container.set(key, PersistentDataType.INTEGER_ARRAY, value);
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, long[] value) {
    container.set(key, PersistentDataType.LONG_ARRAY, value);
  }

  public static void set(
      PersistentDataContainer container, NamespacedKey key, PersistentDataContainer value) {
    container.set(key, PersistentDataType.TAG_CONTAINER, value);
  }

  public static void set(PersistentDataContainer container, NamespacedKey key, UUID value) {
    container.set(key, BA_UUID, value);
  }

  public static void set(
      PersistentDataContainer container, NamespacedKey key, OfflinePlayer value) {
    container.set(key, BA_OFFLINE_PLAYER, value);
  }

  public static SubItemTagContainer makeTag(PersistentDataContainer container, NamespacedKey key) {
    SubItemTagContainer subItemTagContainer =
        new SubItemTagContainer(
            container,
            key,
            computeIfAbsent(
                container,
                key,
                PersistentDataType.TAG_CONTAINER,
                (k) -> container.getAdapterContext().newPersistentDataContainer()));
    WeakReference<PersistentDataContainer> weakParent = new WeakReference<>(container);
    FinalizablePhantomReference<SubItemTagContainer> reference =
        new FinalizablePhantomReference<SubItemTagContainer>(
            subItemTagContainer, SubItemTagContainer.frq) {
          public void finalizeReferent() {
            if (SubItemTagContainer.references.remove(this)) {
              RPGItems.logger.severe(
                  "Unhandled SubItemTagContainer found: " + key + "@" + weakParent.get());
            }
          }
        };
    subItemTagContainer.setReference(reference);
    SubItemTagContainer.references.add(reference);
    return subItemTagContainer;
  }

  public static SubItemTagContainer makeTag(ItemMeta itemMeta, NamespacedKey key) {
    @SuppressWarnings("deprecation")
    PersistentDataContainer container = itemMeta.getPersistentDataContainer();
    return makeTag(container, key);
  }

  public static class UUIDPersistentDataType implements PersistentDataType<byte[], UUID> {
    @Override
    public Class<byte[]> getPrimitiveType() {
      return byte[].class;
    }

    @Override
    public Class<UUID> getComplexType() {
      return UUID.class;
    }

    @Override
    public byte[] toPrimitive(UUID complex, PersistentDataAdapterContext context) {
      return Utils.decodeUUID(complex);
    }

    @Override
    public UUID fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
      return Utils.encodeUUID(primitive);
    }
  }

  public static class BooleanPersistentDataType implements PersistentDataType<Byte, Boolean> {
    @Override
    public Class<Byte> getPrimitiveType() {
      return Byte.class;
    }

    @Override
    public Class<Boolean> getComplexType() {
      return Boolean.class;
    }

    @Override
    public Byte toPrimitive(Boolean complex, PersistentDataAdapterContext context) {
      return (byte) (complex == null ? 0b10101010 : complex ? 0b00000001 : 0b00000000);
    }

    @Override
    public Boolean fromPrimitive(Byte primitive, PersistentDataAdapterContext context) {
      switch (primitive) {
        case (byte) 0b10101010:
          return null;
        case (byte) 0b00000001:
          return true;
        case (byte) 0b00000000:
          return false;
        default:
          throw new IllegalArgumentException();
      }
    }
  }

  public static class OfflinePlayerPersistentDataType
      implements PersistentDataType<byte[], OfflinePlayer> {
    @Override
    public Class<byte[]> getPrimitiveType() {
      return byte[].class;
    }

    @Override
    public Class<OfflinePlayer> getComplexType() {
      return OfflinePlayer.class;
    }

    @Override
    public byte[] toPrimitive(OfflinePlayer complex, PersistentDataAdapterContext context) {
      return Utils.decodeUUID(complex.getUniqueId());
    }

    @Override
    public OfflinePlayer fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
      return Bukkit.getOfflinePlayer(Utils.encodeUUID(primitive));
    }
  }

  public static class ItemStackPersistentDataType implements PersistentDataType<String, ItemStack> {
    @Override
    public Class<String> getPrimitiveType() {
      return String.class;
    }

    @Override
    public Class<ItemStack> getComplexType() {
      return ItemStack.class;
    }

    @Override
    public String toPrimitive(ItemStack complex, PersistentDataAdapterContext context) {
      return ItemStackUtils.itemToBase64(complex);
    }

    @Override
    public ItemStack fromPrimitive(String primitive, PersistentDataAdapterContext context) {
      return ItemStackUtils.itemFromBase64(primitive);
    }
  }

  public static class SubItemTagContainer implements PersistentDataContainer {
    private PersistentDataContainer parent;
    private PersistentDataContainer self;
    private NamespacedKey key;
    private PhantomReference<SubItemTagContainer> reference;

    private static FinalizableReferenceQueue frq = new FinalizableReferenceQueue();
    private static final Set<Reference<?>> references = Sets.newConcurrentHashSet();

    private SubItemTagContainer(
        PersistentDataContainer parent, NamespacedKey key, PersistentDataContainer self) {
      this.parent = parent;
      this.self = self;
      this.key = key;
    }

    @Override
    public <T, Z> void set(
        NamespacedKey namespacedKey, PersistentDataType<T, Z> persistentDataType, Z z) {
      self.set(namespacedKey, persistentDataType, z);
    }

    @Override
    public <T, Z> boolean has(
        NamespacedKey namespacedKey, PersistentDataType<T, Z> persistentDataType) {
      return self.has(namespacedKey, persistentDataType);
    }

    @Override
    public <T, Z> Z get(NamespacedKey namespacedKey, PersistentDataType<T, Z> persistentDataType) {
      return self.get(namespacedKey, persistentDataType);
    }

    @Override
    public <T, Z> Z getOrDefault(NamespacedKey key, PersistentDataType<T, Z> type, Z defaultValue) {
      return self.getOrDefault(key, type, defaultValue);
    }

    @Override
    public Set<NamespacedKey> getKeys() {
      // todo check this
      return Collections.singleton(key);
    }

    @Override
    public void remove(NamespacedKey namespacedKey) {
      self.remove(namespacedKey);
    }

    @Override
    public boolean isEmpty() {
      return self.isEmpty();
    }

    @Override
    public PersistentDataAdapterContext getAdapterContext() {
      return self.getAdapterContext();
    }

    public void commit() {
      ItemTagUtils.set(parent, key, self);
      if (parent instanceof SubItemTagContainer) {
        ((SubItemTagContainer) parent).commit();
      }
      dispose();
    }

    public void dispose() {
      self = null;
      if (!SubItemTagContainer.references.remove(reference)) {
        RPGItems.logger.log(
            Level.SEVERE,
            "Double handled SubItemTagContainer found: " + this + ": " + key + "@" + parent);
        new Exception().printStackTrace();
      }
    }

    public void tryDispose() {
      if (self != null) {
        dispose();
      }
    }

    private void setReference(FinalizablePhantomReference<SubItemTagContainer> reference) {
      this.reference = reference;
    }
  }
}

package think.rpgitems.item;

import cat.nyaa.nyaacore.utils.ItemTagUtils;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import static think.rpgitems.item.RPGItem.NBT_ITEM_UUID;

public class ItemStackWrapper {
    private static final HashMap<String, ItemStackWrapper> wrapperMap = new HashMap<>();
    ItemStack handle;
    Optional<String> itemUUID;

    private ItemStackWrapper(ItemStack itemStack) {
        handle = itemStack;
        itemUUID = ItemTagUtils.getString(itemStack, NBT_ITEM_UUID);
    }

    public static ItemStackWrapper of(ItemStack itemStack) {
        if (itemStack == null) {
            throw new NullPointerException();
        }
        Optional<String> itemUuid = ItemTagUtils.getString(itemStack, NBT_ITEM_UUID);
        return itemUuid.map(s -> wrapperMap.computeIfAbsent(s, (u) -> new ItemStackWrapper(itemStack))).orElseGet(() -> new ItemStackWrapper(itemStack));
    }

    @Override
    public int hashCode() {
        return itemUUID.map(String::hashCode).orElseGet(() -> handle.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        Optional<String> toCmpUuid;
        if ((obj instanceof ItemStack toCmp)) {
            toCmpUuid = ItemTagUtils.getString(toCmp, NBT_ITEM_UUID);
        } else if (obj instanceof ItemStackWrapper) {
            toCmpUuid = ((ItemStackWrapper) obj).itemUUID;
        } else {
            return false;
        }

        if (itemUUID.isPresent()) {
            if (toCmpUuid.isEmpty()) {
                return false;
            }
            String uuid = itemUUID.get();
            return Objects.equals(uuid, toCmpUuid.get());
        }
        return Objects.equals(handle, obj);
    }
}

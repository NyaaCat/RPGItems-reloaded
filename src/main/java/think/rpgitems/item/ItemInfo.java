package think.rpgitems.item;

import java.util.Objects;
import java.util.UUID;

public class ItemInfo {
    public RPGItem item;
    public Integer durability;

    public UUID stackOwner;
    public UUID stackId;

    public ItemInfo(RPGItem rpgItem) {
        item = rpgItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemInfo itemInfo = (ItemInfo) o;
        return Objects.equals(item, itemInfo.item) &&
                Objects.equals(durability, itemInfo.durability) &&
                Objects.equals(stackOwner, itemInfo.stackOwner) &&
                Objects.equals(stackId, itemInfo.stackId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, durability, stackOwner, stackId);
    }
}

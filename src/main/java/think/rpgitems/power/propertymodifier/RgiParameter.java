package think.rpgitems.power.propertymodifier;

import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Pimpl;
import think.rpgitems.power.Power;

public class RgiParameter<T> {
    RPGItem item;
    Power power;
    ItemStack itemStack;
    private T value;

    public RgiParameter(RPGItem item, Power power, ItemStack itemStack, T value) {
        this.item = item;
        this.power = power;
        this.itemStack = itemStack;
        this.value = value;
    }

    public RPGItem getItem() {
        return item;
    }

    public Power getPowerClass() {
        return power;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public T getValue() {
        return value;
    }
}

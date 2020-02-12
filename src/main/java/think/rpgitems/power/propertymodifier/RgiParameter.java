package think.rpgitems.power.propertymodifier;

import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Pimpl;
import think.rpgitems.power.Power;

public class RgiParameter {
    RPGItem item;
    Power power;
    ItemStack itemStack;
    private double value;

    public RgiParameter(RPGItem item, Power power, ItemStack itemStack, double value) {
        this.item = item;
        this.power = power;
        this.itemStack = itemStack;
        this.value = value;
    }

    public RPGItem getItem() {
        return item;
    }

    public Power getPower() {
        return power;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Double getValue() {
        return value;
    }
}

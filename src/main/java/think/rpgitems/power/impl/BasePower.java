package think.rpgitems.power.impl;

import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;

import java.io.Serializable;

public abstract class BasePower implements Serializable, Power {
    RPGItem item;

    @Override
    public RPGItem getItem() {
        return item;
    }

    @Override
    public void setItem(RPGItem item) {
        this.item = item;
    }
}

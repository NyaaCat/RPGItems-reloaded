package think.rpgitems.power.impl;

import org.bukkit.configuration.ConfigurationSection;
import think.rpgitems.commands.PowerProperty;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerManager;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.SortedMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

    @Override
    public void save(ConfigurationSection section) {
        SortedMap<PowerProperty, Field> properties = PowerManager.propertyOrders.get(this.getClass());
        properties.forEach(
                (property, field) -> {
                    try {
                        Function<Object, String> getter = PowerManager.getters.get(this.getClass(), property.name());
                        if (getter != null) {
                            section.set(property.name(), getter.apply(this));
                        } else {
                            section.set(property.name(), field.get(this));
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @Override
    public void init(ConfigurationSection section) {
        SortedMap<PowerProperty, Field> properties = PowerManager.propertyOrders.get(this.getClass());
        properties.forEach(
                (property, field) -> {
                    try {
                        BiConsumer<Object, String> setter = PowerManager.setters.get(this.getClass(), property.name());
                        if (setter != null) {
                            setter.accept(this, section.getString(property.name()));
                        } else {
                            field.set(this, section.get(property.name(), field.get(this)));
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}

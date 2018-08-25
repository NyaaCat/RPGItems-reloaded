package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import think.rpgitems.Handler;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.*;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerManager;
import think.rpgitems.power.TriggerType;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * Base class containing common methods and fields.
 */
public abstract class BasePower implements Serializable, Power {
    RPGItem item;

    @Property
    @AcceptedValue(preset = Preset.TRIGGERS)
    protected Set<TriggerType> triggers = Power.getTriggerTypes(this.getClass());

    @Override
    public RPGItem getItem() {
        return item;
    }

    @Override
    public void setItem(RPGItem item) {
        this.item = item;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void save(ConfigurationSection section) {
        SortedMap<PowerProperty, Field> properties = PowerManager.propertyOrders.get(this.getClass());
        PowerMeta powerMeta = this.getClass().getAnnotation(PowerMeta.class);

        for (Map.Entry<PowerProperty, Field> entry : properties.entrySet()) {
            PowerProperty property = entry.getKey();
            Field field = entry.getValue();
            if (property.name().equals("triggers") && powerMeta.immutableTrigger()) {
                continue;
            }
            try {
                Serializer getter = field.getAnnotation(Serializer.class);
                if (getter != null) {
                    section.set(property.name(), Getter.from(getter.value()).get(field.get(this)));
                } else {
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        Collection c = (Collection) field.get(this);
                        section.set(property.name(), c.stream().map(Object::toString).collect(Collectors.joining(",")));
                    } else {
                        section.set(property.name(), field.get(this));
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void init(ConfigurationSection section) {
        PowerMeta powerMeta = this.getClass().getAnnotation(PowerMeta.class);
        SortedMap<PowerProperty, Field> properties = PowerManager.propertyOrders.get(this.getClass());
        for (Map.Entry<PowerProperty, Field> entry : properties.entrySet()) {
            PowerProperty property = entry.getKey();
            Field field = entry.getValue();
            if (property.name().equals("triggers") && powerMeta.immutableTrigger()) {
                continue;
            }
            String value = section.getString(property.name());
            if (property.name().equals("cost") && value == null) {
                value = section.getString("consumption");
            }
            if (value != null) {
                Handler.setPowerProperty(Bukkit.getConsoleSender(), this, field, value);
            }
        }
    }

    @Override
    public Set<TriggerType> getTriggers() {
        return triggers;
    }

    @Override
    public NamespacedKey getNamespacedKey() {
        return new NamespacedKey(RPGItems.plugin, getName());
    }
}

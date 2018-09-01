package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class containing common methods and fields.
 */
public abstract class BasePower implements Serializable, Power {
    RPGItem item;

    @Property
    @AcceptedValue(preset = Preset.TRIGGERS)
    public Set<TriggerType> triggers = Power.getDefaultTriggerTypes(this.getClass());

    @Property
    public Set<String> selectors = new HashSet<>();

    @Property
    public Set<String> conditions = new HashSet<>();

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
        SortedMap<PowerProperty, Field> properties = PowerManager.getProperties(this.getClass());
        PowerMeta powerMeta = this.getClass().getAnnotation(PowerMeta.class);

        for (Map.Entry<PowerProperty, Field> entry : properties.entrySet()) {
            PowerProperty property = entry.getKey();
            Field field = entry.getValue();
            if (property.name().equals("triggers") && powerMeta.immutableTrigger()) {
                continue;
            }
            try {
                Serializer getter = field.getAnnotation(Serializer.class);
                Object val = field.get(this);
                if (val == null) continue;
                if (getter != null) {
                    section.set(property.name(), Getter.from(getter.value()).get(val));
                } else {
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        Collection c = (Collection) val;
                        if (c.isEmpty()) continue;
                        section.set(property.name(), c.stream().map(Object::toString).collect(Collectors.joining(",")));
                    } else {
                        section.set(property.name(), val);
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
        SortedMap<PowerProperty, Field> properties = PowerManager.getProperties(this.getClass());
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
                try {
                    PowerManager.setPowerProperty(Bukkit.getConsoleSender(), this, field, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public Set<TriggerType> getTriggers() {
        return triggers;
    }

    @Override
    public Set<String> getConditions() {
        return conditions;
    }

    @Override
    public Set<String> getSelectors() {
        return selectors;
    }

    @Override
    public NamespacedKey getNamespacedKey() {
        return new NamespacedKey(RPGItems.plugin, getName());
    }
}

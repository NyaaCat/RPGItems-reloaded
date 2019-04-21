package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Base class containing common methods and fields.
 */
public abstract class BasePower implements Serializable, Power {
    RPGItem item;

    @Property
    public String displayName;

    @Property
    @AcceptedValue(preset = Preset.TRIGGERS)
    public Set<Trigger> triggers = Power.getDefaultTriggers(this.getClass());

    @Property
    public Set<String> selectors = new HashSet<>();

    @Property
    public Set<String> conditions = new HashSet<>();

    @Property
    public OffhandMode handMode = OffhandMode.MAINHAND;

    @Property
    public String requiredContext;

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
        Map<String, PowerProperty> properties = PowerManager.getProperties(this.getClass());
        PowerMeta powerMeta = this.getClass().getAnnotation(PowerMeta.class);

        for (Map.Entry<String, PowerProperty> entry : properties.entrySet()) {
            String name = entry.getKey();
            PowerProperty property = entry.getValue();
            Field field = property.field();
            if (name.equals("triggers") && powerMeta.immutableTrigger()) {
                continue;
            }
            try {
                Utils.saveProperty(this, section, name, field);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void init(ConfigurationSection section) {
        PowerMeta powerMeta = this.getClass().getAnnotation(PowerMeta.class);
        Map<String, PowerProperty> properties = PowerManager.getProperties(this.getClass());
        for (Map.Entry<String, PowerProperty> entry : properties.entrySet()) {
            String name = entry.getKey();
            PowerProperty property = entry.getValue();
            Field field = property.field();
            if (name.equals("triggers") && powerMeta.immutableTrigger()) {
                continue;
            }
            if (field.getType().isAssignableFrom(ItemStack.class)) {
                ItemStack itemStack = section.getItemStack(name);
                if (itemStack != null) {
                    try {
                        field.set(this, itemStack);
                        continue;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            String value = section.getString(name);
            if (value == null) {
                for (String alias : property.alias()) {
                    value = section.getString(alias);
                    if (value != null) break;
                }
            }
            if (name.equals("cost") && value == null) {
                value = section.getString("consumption");
            }
            if (value != null) {
                Utils.setPowerPropertyUnchecked(Bukkit.getConsoleSender(), this, field, value);
            }
        }
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public Set<Trigger> getTriggers() {
        return Collections.unmodifiableSet(triggers);
    }

    @Override
    public Set<String> getConditions() {
        return Collections.unmodifiableSet(conditions);
    }

    @Override
    public Set<String> getSelectors() {
        return Collections.unmodifiableSet(selectors);
    }

    @Override
    public NamespacedKey getNamespacedKey() {
        return new NamespacedKey(RPGItems.plugin, getName());
    }

    @Override
    public String getLocalizedName(String locale) {
        return I18n.format("power.properties." + getName() + ".main_name");
    }

    @Override
    public String requiredContext() {
        return requiredContext;
    }
}

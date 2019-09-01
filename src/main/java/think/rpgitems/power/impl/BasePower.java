package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base class containing common methods and fields.
 */
public abstract class BasePower extends BasePropertyHolder implements Serializable, Power {
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
    public String requiredContext;

    @Override
    public String displayName() {
        return displayName;
    }
    @Override
    public Set<Trigger> getTriggers() {
        return Collections.unmodifiableSet(triggers);
    }

    @Override
    public Set<String> getSelectors() {
        return Collections.unmodifiableSet(selectors);
    }

    @Override
    public Set<String> getConditions() {
        return Collections.unmodifiableSet(conditions);
    }

    @Override
    public String requiredContext() {
        return requiredContext;
    }
}
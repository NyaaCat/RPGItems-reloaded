package think.rpgitems.power;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public abstract class BasePropertyHolder implements PropertyHolder {

    RPGItem item;

    @Override
    public void init(ConfigurationSection section) {
        Meta meta = this.getClass().getAnnotation(Meta.class);
        Map<String, Pair<Method, PropertyInstance>> properties = PowerManager.getProperties(this.getClass());
        for (Map.Entry<String, Pair<Method, PropertyInstance>> entry : properties.entrySet()) {
            String name = entry.getKey();
            PropertyInstance property = entry.getValue().getValue();
            Field field = property.field();
            if (name.equals("triggers") && meta.immutableTrigger()) {
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
    public void save(ConfigurationSection section) {
        Map<String, Pair<Method, PropertyInstance>> properties = PowerManager.getProperties(this.getClass());
        Meta meta = this.getClass().getAnnotation(Meta.class);

        for (Map.Entry<String, Pair<Method, PropertyInstance>> entry : properties.entrySet()) {
            String name = entry.getKey();
            PropertyInstance property = entry.getValue().getValue();
            Field field = property.field();
            if (name.equals("triggers") && meta.immutableTrigger()) {
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
    public NamespacedKey getNamespacedKey() {
        return new NamespacedKey(RPGItems.plugin, getName());
    }

    @Override
    public String getLocalizedName(String locale) {
        return I18n.format("power.properties." + getName() + ".main_name");
    }

    @Override
    public RPGItem getItem() {
        return item;
    }

    @Override
    public void setItem(RPGItem item) {
        this.item = item;
    }
}

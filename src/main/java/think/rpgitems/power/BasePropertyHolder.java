package think.rpgitems.power;

import cat.nyaa.nyaacore.Pair;
import io.papermc.paper.datacomponent.item.CustomModelData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
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
            if (field.getType().equals(CustomModelData.Builder.class) && section.isConfigurationSection(name)) {
                try {
                    field.set(this, parseCustomModelDataSection(section.getConfigurationSection(name)));
                    continue;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
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

    private CustomModelData.Builder parseCustomModelDataSection(ConfigurationSection modelSection) {
        CustomModelData.Builder customData = CustomModelData.customModelData();
        if (modelSection == null) {
            return customData;
        }
        for (String sectionKey : modelSection.getKeys(false)) {
            switch (sectionKey) {
                case "floats":
                    if (modelSection.isList("floats")) {
                        for (double value : modelSection.getDoubleList("floats")) {
                            customData.addFloat((float) value);
                        }
                    }
                    break;
                case "strings":
                    if (modelSection.isList("strings")) {
                        for (String value : modelSection.getStringList("strings")) {
                            customData.addString(value);
                        }
                    }
                    break;
                case "colors":
                    if (modelSection.isList("colors")) {
                        for (String value : modelSection.getStringList("colors")) {
                            String[] parts = value.split(",");
                            if (parts.length != 3) {
                                throw new IllegalArgumentException("Invalid color format (expected R,G,B color format): " + value);
                            }
                            int r = Integer.parseInt(parts[0]);
                            int g = Integer.parseInt(parts[1]);
                            int b = Integer.parseInt(parts[2]);
                            customData.addColor(Color.fromRGB(r, g, b));
                        }
                    }
                    break;
                case "flags":
                    if (modelSection.isList("flags")) {
                        for (boolean value : modelSection.getBooleanList("flags")) {
                            customData.addFlag(value);
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown customModelData section key: " + sectionKey);
            }
        }
        return customData;
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
        return I18n.getInstance(locale).getFormatted("properties." + getName() + ".main_name");
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

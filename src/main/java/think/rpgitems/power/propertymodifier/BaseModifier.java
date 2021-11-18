package think.rpgitems.power.propertymodifier;

import cat.nyaa.nyaacore.Pair;
import com.google.common.base.Strings;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import think.rpgitems.power.*;
import think.rpgitems.utils.ItemTagUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static think.rpgitems.utils.ItemTagUtils.set;

public abstract class BaseModifier<T> extends BasePropertyHolder implements Modifier<T> {
    @Property(order = 0, required = true)
    public String id;
    @Property(order = 1, required = true)
    public String targetItem;
    @Property(order = 2, required = true)
    public String targetPower;
    @Property(order = 3, required = true)
    public String targetProperty;
    @Property(order = 4, required = true)
    public int priority;

    @Override
    public String getPropertyHolderType() {
        return "modifier";
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int priority() {
        return priority;
    }

    public void init(PersistentDataContainer section) {
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
                ItemStack itemStack = ItemTagUtils.getItemStack(section, name);
                if (itemStack != null) {
                    try {
                        field.set(this, itemStack);
                        continue;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            String value = ItemTagUtils.getString(section, name);
            if (value == null) {
                for (String alias : property.alias()) {
                    value = ItemTagUtils.getString(section, alias);
                    if (value != null) break;
                }
            }
            if (value != null) {
                Utils.setPowerPropertyUnchecked(Bukkit.getConsoleSender(), this, field, value);
            }
        }
    }

    // TODO
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void save(PersistentDataContainer section) {
        Map<String, Pair<Method, PropertyInstance>> properties = PowerManager.getProperties(this.getClass());
        set(section, PowerManager.parseKey("modifier_name"), getNamespacedKey().toString());
        for (Map.Entry<String, Pair<Method, PropertyInstance>> entry : properties.entrySet()) {
            String name = entry.getKey();
            PropertyInstance property = entry.getValue().getValue();
            Field field = property.field();
            try {
                Serializer getter = field.getAnnotation(Serializer.class);
                Object val = field.get(this);
                if (val == null) continue;
                if (getter != null) {
                    set(section, name, Getter.from(this, getter.value()).get(val));
                } else {
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        Collection c = (Collection) val;
                        if (c.isEmpty()) continue;
                        if (Set.class.isAssignableFrom(field.getType())) {
                            set(section, name, (String) c.stream().map(Object::toString).sorted().collect(Collectors.joining(",")));
                        } else {
                            set(section, name, (String) c.stream().map(Object::toString).collect(Collectors.joining(",")));
                        }
                    } else if (field.getType() == Enchantment.class) {
                        set(section, name, ((Enchantment) val).getKey().toString());
                    } else {
                        val = field.getType().isEnum() ? ((Enum<?>) val).name() : val;
                        set(section, name, val.toString());
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean match(Power orig, PropertyInstance propertyInstance) {
        if (!Strings.isNullOrEmpty(targetItem) && !orig.getItem().getName().equals(targetItem)) {
            return false;
        }
        if (!Strings.isNullOrEmpty(targetPower) && !orig.getNamespacedKey().equals(PowerManager.parseKey(targetPower))) {
            return false;
        }
        return Strings.isNullOrEmpty(targetProperty) || propertyInstance.name().equals(targetProperty);
    }
}

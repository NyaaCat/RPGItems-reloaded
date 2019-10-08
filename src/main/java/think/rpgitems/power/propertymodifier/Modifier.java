package think.rpgitems.power.propertymodifier;

import org.bukkit.persistence.PersistentDataContainer;
import think.rpgitems.power.Power;
import think.rpgitems.power.PropertyHolder;
import think.rpgitems.power.PropertyInstance;

import java.util.function.Function;

public interface Modifier<T> extends Function<T, T>, PropertyHolder {
    void init(PersistentDataContainer section);

    void save(PersistentDataContainer section);

    boolean match(Power orig, PropertyInstance propertyInstance);

    Class<T> getModifierTargetType();
}

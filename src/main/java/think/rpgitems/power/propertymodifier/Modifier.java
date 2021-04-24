package think.rpgitems.power.propertymodifier;

import java.util.function.Function;
import org.bukkit.persistence.PersistentDataContainer;
import think.rpgitems.power.Power;
import think.rpgitems.power.PropertyHolder;
import think.rpgitems.power.PropertyInstance;

public interface Modifier<T> extends Function<RgiParameter<T>, T>, PropertyHolder {
  void init(PersistentDataContainer section);

  void save(PersistentDataContainer section);

  boolean match(Power orig, PropertyInstance propertyInstance);

  String id();

  int priority();

  Class<T> getModifierTargetType();
}

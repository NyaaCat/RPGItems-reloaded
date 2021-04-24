package think.rpgitems.power.trigger;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerLivingEntity;
import think.rpgitems.power.PowerResult;

class LivingEntity<TPower extends Power>
    extends Trigger<Event, TPower, PowerLivingEntity<TPower>, Void, Void> {
  LivingEntity() {
    super(Event.class, PowerLivingEntity.class, Void.class, Void.class, "LIVINGENTITY");
  }

  public LivingEntity(String name) {
    super(name, "LIVINGENTITY", Event.class, PowerLivingEntity.class, Void.class, Void.class);
  }

  @Override
  public PowerResult<Void> run(
      TPower power, PowerLivingEntity<TPower> pimpl, Player player, ItemStack i, Event event) {
    throw new IllegalStateException();
  }

  @Override
  public PowerResult<Void> run(
      TPower power,
      PowerLivingEntity<TPower> pimpl,
      Player player,
      ItemStack i,
      Event event,
      Object data) {
    return pimpl.fire(
        power,
        player,
        i,
        (org.bukkit.entity.LivingEntity) ((Pair) data).getKey(),
        (Double) ((Pair) data).getValue());
  }
}

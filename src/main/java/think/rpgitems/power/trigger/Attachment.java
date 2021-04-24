package think.rpgitems.power.trigger;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerAttachment;
import think.rpgitems.power.PowerResult;

class Attachment<TPower extends Power>
    extends Trigger<Event, TPower, PowerAttachment<TPower>, Void, Void> {
  Attachment() {
    super(Event.class, PowerAttachment.class, Void.class, Void.class, "ATTACHMENT");
  }

  public Attachment(String name) {
    super(name, "ATTACHMENT", Event.class, PowerAttachment.class, Void.class, Void.class);
  }

  @Override
  public PowerResult<Void> run(
      TPower power, PowerAttachment<TPower> pimpl, Player player, ItemStack i, Event event) {
    throw new IllegalStateException();
  }

  @Override
  public PowerResult<Void> run(
      TPower power,
      PowerAttachment<TPower> pimpl,
      Player player,
      ItemStack i,
      Event event,
      Object data) {
    ItemStack originalItemstack = (ItemStack) ((Pair) data).getKey();
    Event originalEvent = (Event) ((Pair) data).getValue();
    return pimpl.attachment(
        power,
        player,
        i,
        ItemManager.toRPGItem(originalItemstack).orElseThrow(IllegalArgumentException::new),
        originalEvent,
        originalItemstack);
  }
}

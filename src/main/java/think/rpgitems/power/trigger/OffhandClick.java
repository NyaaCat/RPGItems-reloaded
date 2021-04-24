package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerOffhandClick;
import think.rpgitems.power.PowerResult;

class OffhandClick<TPower extends Power>
    extends Trigger<PlayerInteractEvent, TPower, PowerOffhandClick<TPower>, Void, Void> {
  OffhandClick() {
    super(
        PlayerInteractEvent.class,
        PowerOffhandClick.class,
        Void.class,
        Void.class,
        "OFFHAND_CLICK");
  }

  public OffhandClick(String name) {
    super(
        name,
        "OFFHAND_CLICK",
        PlayerInteractEvent.class,
        PowerOffhandClick.class,
        Void.class,
        Void.class);
  }

  @Override
  public PowerResult<Void> run(
      TPower power,
      PowerOffhandClick<TPower> pimpl,
      Player player,
      ItemStack i,
      PlayerInteractEvent event) {
    return pimpl.offhandClick(power, player, i, event);
  }
}

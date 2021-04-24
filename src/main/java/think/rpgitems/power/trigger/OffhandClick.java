package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerOffhandClick;
import think.rpgitems.power.PowerResult;

class OffhandClick extends Trigger<PlayerInteractEvent, PowerOffhandClick, Void, Void> {
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
      PowerOffhandClick power, Player player, ItemStack i, PlayerInteractEvent event) {
    return power.offhandClick(player, i, event);
  }
}

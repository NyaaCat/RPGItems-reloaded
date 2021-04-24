package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;

class RightClick extends Trigger<PlayerInteractEvent, PowerRightClick, Void, Void> {
  RightClick() {
    super(PlayerInteractEvent.class, PowerRightClick.class, Void.class, Void.class, "RIGHT_CLICK");
  }

  public RightClick(String name) {
    super(
        name,
        "RIGHT_CLICK",
        PlayerInteractEvent.class,
        PowerRightClick.class,
        Void.class,
        Void.class);
  }

  @Override
  public PowerResult<Void> run(
      PowerRightClick power, Player player, ItemStack i, PlayerInteractEvent event) {
    return power.rightClick(player, i, event);
  }
}

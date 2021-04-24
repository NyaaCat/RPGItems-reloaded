package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerSprint;

class Sprint extends Trigger<PlayerToggleSprintEvent, PowerSprint, Void, Void> {
  Sprint() {
    super(PlayerToggleSprintEvent.class, PowerSprint.class, Void.class, Void.class, "SPRINT");
  }

  public Sprint(String name) {
    super(name, "SPRINT", PlayerToggleSprintEvent.class, PowerSprint.class, Void.class, Void.class);
  }

  @Override
  public PowerResult<Void> run(
      PowerSprint power, Player player, ItemStack i, PlayerToggleSprintEvent event) {
    return power.sprint(player, i, event);
  }
}

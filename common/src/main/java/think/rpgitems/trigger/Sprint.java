package think.rpgitems.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerSprint;
import think.rpgitems.power.Trigger;

public class Sprint extends Trigger<PlayerToggleSprintEvent, PowerSprint, Void, Void> {
    public static final Trigger<PlayerToggleSprintEvent, PowerSprint, Void, Void> SPRINT = new Sprint();

    public Sprint() {
        super(PlayerToggleSprintEvent.class, PowerSprint.class, Void.class, Void.class, "SPRINT");
    }

    @Override
    public PowerResult<Void> run(PowerSprint power, Player player, ItemStack i, PlayerToggleSprintEvent event) {
        return power.sprint(player, i, event);
    }
}

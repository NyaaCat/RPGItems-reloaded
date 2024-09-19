package think.rpgitems.power.trigger;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerJump;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerSprint;

class Jump extends Trigger<PlayerJumpEvent, PowerJump, Void, Void> {
    Jump() {
        super(PlayerJumpEvent.class, PowerJump.class, Void.class, Void.class, "JUMP");
    }

    public Jump(String name) {
        super(name, "JUMP", PlayerJumpEvent.class, PowerJump.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerJump power, Player player, ItemStack i, PlayerJumpEvent event) {
        return power.jump(player, i, event);
    }
}

package think.rpgitems.power.trigger;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerJump;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerSprint;
import think.rpgitems.power.PowerSwim;

import javax.swing.border.EtchedBorder;

class Swim extends Trigger<EntityToggleSwimEvent, PowerSwim, Void, Void> {
    Swim() {
        super(EntityToggleSwimEvent.class, PowerSwim.class, Void.class, Void.class, "SWIM");
    }

    public Swim(String name) {
        super(name, "SWIM", EntityToggleSwimEvent.class, PowerSwim.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerSwim power, Player player, ItemStack i, EntityToggleSwimEvent event) {
        return power.swim(player, i, event);
    }
}

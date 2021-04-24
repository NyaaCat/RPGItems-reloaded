package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;

class RightClick<TPower extends Power>
        extends Trigger<PlayerInteractEvent, TPower, PowerRightClick<TPower>, Void, Void> {
    RightClick() {
        super(
                PlayerInteractEvent.class,
                PowerRightClick.class,
                Void.class,
                Void.class,
                "RIGHT_CLICK");
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
            TPower power,
            PowerRightClick<TPower> pimpl,
            Player player,
            ItemStack i,
            PlayerInteractEvent event) {
        return pimpl.rightClick(power, player, i, event);
    }
}

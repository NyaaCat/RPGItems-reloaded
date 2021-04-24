package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerLeftClick;
import think.rpgitems.power.PowerResult;

class LeftClick<TPower extends Power>
        extends Trigger<PlayerInteractEvent, TPower, PowerLeftClick<TPower>, Void, Void> {
    LeftClick() {
        super(
                PlayerInteractEvent.class,
                PowerLeftClick.class,
                Void.class,
                Void.class,
                "LEFT_CLICK");
    }

    public LeftClick(String name) {
        super(
                name,
                "LEFT_CLICK",
                PlayerInteractEvent.class,
                PowerLeftClick.class,
                Void.class,
                Void.class);
    }

    @Override
    public PowerResult<Void> run(
            TPower power,
            PowerLeftClick<TPower> pimpl,
            Player player,
            ItemStack i,
            PlayerInteractEvent event) {
        return pimpl.leftClick(power, player, i, event);
    }
}

/*
PowerResult<([A-Za-z]+)>[\n\s]*run\([\n\s]*([A-Za-z]+)[\n\s]+([A-Za-z]+),[\n\s]*Player[\n\s]+([A-Za-z]+),[\n\s]*ItemStack[\n\s]+([A-Za-z]+),[\n\s]*([A-Za-z]+)[\n\s]+([A-Za-z]+)\)
*/

package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;
import think.rpgitems.power.PowerRightClickAll;

class RightClickAll extends Trigger<PlayerInteractEvent, PowerRightClickAll, Void, Void> {
    RightClickAll() {
        super(PlayerInteractEvent.class, PowerRightClickAll.class, Void.class, Void.class, "RIGHT_CLICK_ALL");
    }

    public RightClickAll(String name) {
        super(name, "RIGHT_CLICK_ALL", PlayerInteractEvent.class, PowerRightClickAll.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerRightClickAll power, Player player, ItemStack i, PlayerInteractEvent event) {
        return power.rightClickAll(player, i, event);
    }
}

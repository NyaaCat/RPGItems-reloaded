package think.rpgitems.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;
import think.rpgitems.power.Trigger;

public class RightClick extends Trigger<PlayerInteractEvent, PowerRightClick, Void, Void> {
    public static final Trigger<PlayerInteractEvent, PowerRightClick, Void, Void> RIGHT_CLICK = new RightClick();

    public RightClick() {
        super(PlayerInteractEvent.class, PowerRightClick.class, Void.class, Void.class, "RIGHT_CLICK");
    }

    @Override
    public PowerResult<Void> run(PowerRightClick power, Player player, ItemStack i, PlayerInteractEvent event) {
        return power.rightClick(player, i, event);
    }
}

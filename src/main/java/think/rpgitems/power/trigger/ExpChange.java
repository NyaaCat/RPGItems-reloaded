package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerExpChange;
import think.rpgitems.power.PowerResult;

class ExpChange extends Trigger<PlayerExpChangeEvent, PowerExpChange, Void, Void> {
    ExpChange() {
        super(PlayerExpChangeEvent.class, PowerExpChange.class, Void.class, Void.class, "EXP_CHANGE");
    }

    public ExpChange(String name) {
        super(name, "EXP_CHANGE", PlayerExpChangeEvent.class, PowerExpChange.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerExpChange power, Player player, ItemStack i, PlayerExpChangeEvent event) {
        return power.expChange(player, i, event);
    }
}

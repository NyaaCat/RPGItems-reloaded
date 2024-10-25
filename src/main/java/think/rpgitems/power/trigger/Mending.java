package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerMending;
import think.rpgitems.power.PowerResult;

class Mending extends Trigger<PlayerItemMendEvent, PowerMending, Void, Void> {
    Mending() {
        super(PlayerItemMendEvent.class, PowerMending.class, Void.class, Void.class, "MENDING");
    }

    public Mending(String name) {
        super(name, "MENDING", PlayerItemMendEvent.class, PowerMending.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerMending power, Player player, ItemStack i, PlayerItemMendEvent event) {
        return power.mending(player, i, event);
    }
}

package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerSneak;

class Sneak extends Trigger<PlayerToggleSneakEvent, PowerSneak, Void, Void> {
    public Sneak() {
        super(PlayerToggleSneakEvent.class, PowerSneak.class, Void.class, Void.class, "SNEAK");
    }

    @Override
    public PowerResult<Void> run(PowerSneak power, Player player, ItemStack i, PlayerToggleSneakEvent event) {
        return power.sneak(player, i, event);
    }
}

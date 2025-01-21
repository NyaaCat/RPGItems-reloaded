package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerConsume;
import think.rpgitems.power.PowerOffhandClick;
import think.rpgitems.power.PowerResult;

import java.util.Optional;

class Consume extends Trigger<PlayerItemConsumeEvent, PowerConsume, Void, Void> {
    Consume() {
        super(PlayerItemConsumeEvent.class, PowerConsume.class, Void.class, Void.class, "CONSUME");
    }

    public Consume(String name) {
        super(name, "CONSUME", PlayerItemConsumeEvent.class, PowerConsume.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerConsume power, Player player, ItemStack i, PlayerItemConsumeEvent event) {
        return power.consume(player, i, event);
    }
}

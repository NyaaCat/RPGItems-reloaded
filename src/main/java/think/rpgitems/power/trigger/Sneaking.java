package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerSneaking;

class Sneaking extends Trigger<Event, PowerSneaking, Void, Void> {
    Sneaking() {
        super(Event.class, PowerSneaking.class, Void.class, Void.class, "SNEAKING");
    }

    public Sneaking(String name) {
        super(name, "SNEAKING", Event.class, PowerSneaking.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerSneaking power, Player player, ItemStack i, Event event) {
        return power.sneaking(player, i);
    }
}

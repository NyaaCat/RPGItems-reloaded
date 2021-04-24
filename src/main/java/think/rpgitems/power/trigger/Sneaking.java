package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerSneaking;

class Sneaking<TPower extends Power>
        extends Trigger<Event, TPower, PowerSneaking<TPower>, Void, Void> {
    Sneaking() {
        super(Event.class, PowerSneaking.class, Void.class, Void.class, "SNEAKING");
    }

    public Sneaking(String name) {
        super(name, "SNEAKING", Event.class, PowerSneaking.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(
            TPower power, PowerSneaking<TPower> pimpl, Player player, ItemStack i, Event event) {
        return pimpl.sneaking(power, player, i);
    }
}

package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerTick;

class TickOffhand<TPower extends Power>
        extends Trigger<Event, TPower, PowerTick<TPower>, Void, Void> {
    TickOffhand() {
        super(Event.class, PowerTick.class, Void.class, Void.class, "TICK_OFFHAND");
    }

    public TickOffhand(String name) {
        super(name, "TICK_OFFHAND", Event.class, PowerTick.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(
            TPower power, PowerTick<TPower> pimpl, Player player, ItemStack i, Event event) {
        return pimpl.tick(power, player, i);
    }
}

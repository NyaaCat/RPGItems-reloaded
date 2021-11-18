package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerTick;

class Tick extends Trigger<Event, PowerTick, Void, Void> {
    Tick() {
        super(Event.class, PowerTick.class, Void.class, Void.class, "TICK");
    }

    public Tick(String name) {
        super(name, "TICK", Event.class, PowerTick.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerTick power, Player player, ItemStack i, Event event) {
        return power.tick(player, i);
    }
}

package think.rpgitems.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerTick;
import think.rpgitems.power.Trigger;

public class TickOffhand extends Trigger<Event, PowerTick, Void, Void> {
    public static final Trigger<Event, PowerTick, Void, Void> TICK_OFFHAND = new TickOffhand();

    public TickOffhand() {
        super(Event.class, PowerTick.class, Void.class, Void.class, "TICK_OFFHAND");
    }

    @Override
    public PowerResult<Void> run(PowerTick power, Player player, ItemStack i, Event event) {
        return power.tick(player, i);
    }
}

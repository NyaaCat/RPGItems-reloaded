package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerLocation;
import think.rpgitems.power.PowerResult;

class Location extends Trigger<Event, PowerLocation, Void, Void> {
    Location() {
        super(Event.class, PowerLocation.class, Void.class, Void.class, "LOCATION");
    }

    public Location(String name) {
        super(name, "LOCATION", Event.class, PowerLocation.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerLocation power, Player player, ItemStack i, Event event) {
        throw new IllegalStateException();
    }

    @Override
    public PowerResult<Void> run(PowerLocation power, Player player, ItemStack i, Event event, Object data) {
        return power.fire(player, i, (org.bukkit.Location) data);
    }
}

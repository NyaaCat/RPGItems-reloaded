package cat.nyaa.rpgitems.trigger;

import cat.nyaa.rpgitems.power.PowerCrossbowLoad;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityLoadCrossbowEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Trigger;

public class CrossbowLoad extends Trigger<EntityLoadCrossbowEvent, PowerCrossbowLoad, Void, Void> {
    public static Trigger<EntityLoadCrossbowEvent, PowerCrossbowLoad, Void, Void> CROSSBOW_LOAD = new CrossbowLoad();

    public CrossbowLoad() {
        super(EntityLoadCrossbowEvent.class, PowerCrossbowLoad.class, Void.class, Void.class, "CROSSBOW_LOAD");
    }

    @Override
    public PowerResult<Void> run(PowerCrossbowLoad power, Player player, ItemStack i, EntityLoadCrossbowEvent event) {
        return power.crossbowLoad(player, i, event.getArrow(), event);
    }
}

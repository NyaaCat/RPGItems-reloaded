package think.rpgitems.trigger;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerLivingEntity;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Trigger;

public class LivingEntity extends Trigger<Event, PowerLivingEntity, Void, Void> {
    public static final Trigger<Event, PowerLivingEntity, Void, Void> LIVINGENTITY = new LivingEntity();

    public LivingEntity() {
        super(Event.class, PowerLivingEntity.class, Void.class, Void.class, "LIVINGENTITY");
    }

    @Override
    public PowerResult<Void> run(PowerLivingEntity power, Player player, ItemStack i, Event event) {
        throw new IllegalStateException();
    }

    @Override
    public PowerResult<Void> run(PowerLivingEntity power, Player player, ItemStack i, Event event, Object data) {
        return power.fire(player, i, (org.bukkit.entity.LivingEntity) ((Pair) data).getKey(), (Double) ((Pair) data).getValue());
    }
}

package think.rpgitems.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerHurt;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Trigger;

public class Hurt extends Trigger<EntityDamageEvent, PowerHurt, Void, Void> {
    public static final Trigger<EntityDamageEvent, PowerHurt, Void, Void> HURT = new Hurt();

    public Hurt() {
        super(EntityDamageEvent.class, PowerHurt.class, Void.class, Void.class, "HURT");
    }

    @Override
    public PowerResult<Void> run(PowerHurt power, Player player, ItemStack i, EntityDamageEvent event) {
        return power.hurt(player, i, event);
    }
}

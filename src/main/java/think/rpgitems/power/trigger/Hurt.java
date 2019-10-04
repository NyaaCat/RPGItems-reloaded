package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerHurt;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

class Hurt extends Trigger<EntityDamageEvent, PowerHurt, Void, Void> {

    @Property
    public double minDamage = Double.NEGATIVE_INFINITY;

    @Property
    public double maxDamage = Double.POSITIVE_INFINITY;

    Hurt() {
        super(EntityDamageEvent.class, PowerHurt.class, Void.class, Void.class, "HURT");
    }

    public Hurt(String name) {
        super(name, "HURT", EntityDamageEvent.class, PowerHurt.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerHurt power, Player player, ItemStack i, EntityDamageEvent event) {
        return power.hurt(player, i, event);
    }

    @Override
    public boolean check(Player player, ItemStack i, EntityDamageEvent event) {
        return event.getDamage() > minDamage && event.getDamage() < maxDamage;
    }
}

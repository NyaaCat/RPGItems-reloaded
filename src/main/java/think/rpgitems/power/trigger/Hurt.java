package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerHurt;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

class Hurt<TPower extends Power>
        extends Trigger<EntityDamageEvent, TPower, PowerHurt<TPower>, Void, Void> {

    @Property public double minDamage = Double.NEGATIVE_INFINITY;

    @Property public double maxDamage = Double.POSITIVE_INFINITY;

    Hurt() {
        super(EntityDamageEvent.class, PowerHurt.class, Void.class, Void.class, "HURT");
    }

    public Hurt(String name) {
        super(name, "HURT", EntityDamageEvent.class, PowerHurt.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(
            TPower power,
            PowerHurt<TPower> pimpl,
            Player player,
            ItemStack i,
            EntityDamageEvent event) {
        return pimpl.hurt(power, player, i, event);
    }

    @Override
    public boolean check(Player player, ItemStack i, EntityDamageEvent event) {
        return event.getDamage() > minDamage && event.getDamage() < maxDamage;
    }
}

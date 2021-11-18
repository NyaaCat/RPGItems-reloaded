package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerHitTaken;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

import java.util.Optional;

public class Dying extends Trigger<EntityDamageEvent, PowerHitTaken, Void, Optional<Void>> {

    @Property
    public double minDamage = Double.NEGATIVE_INFINITY;

    @Property
    public double maxDamage = Double.POSITIVE_INFINITY;

    Dying() {
        super(EntityDamageEvent.class, PowerHitTaken.class, Void.class, Optional.class, "DYING");
    }

    public Dying(String name) {
        super(name, "DYING", EntityDamageEvent.class, PowerHitTaken.class, Void.class, Void.class);
    }

    @Override
    public Optional<Void> def(Player player, ItemStack i, EntityDamageEvent event) {
        return Optional.empty();
    }

    @Override
    public Optional<Void> next(Optional<Void> a, PowerResult<Void> b) {
        return b.isOK() ? Optional.ofNullable(b.data()) : a;
    }

    @Override
    public PowerResult<Void> warpResult(PowerResult<Void> overrideResult, PowerHitTaken power, Player player, ItemStack i, EntityDamageEvent event) {
        return overrideResult;
    }

    @Override
    public PowerResult<Void> run(PowerHitTaken power, Player player, ItemStack i, EntityDamageEvent event) {
        return power.takeHit(player, i, event.getDamage(), event).with(null);
    }

    @Override
    public boolean check(Player player, ItemStack i, EntityDamageEvent event) {
        return event.getDamage() > minDamage && event.getDamage() < maxDamage;
    }
}

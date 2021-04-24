package think.rpgitems.power.trigger;

import static think.rpgitems.power.Utils.minWithCancel;

import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerHitTaken;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

class HitTaken<TPower extends Power>
        extends Trigger<
                EntityDamageEvent, TPower, PowerHitTaken<TPower>, Double, Optional<Double>> {

    @Property public double minDamage = Double.NEGATIVE_INFINITY;

    @Property public double maxDamage = Double.POSITIVE_INFINITY;

    HitTaken() {
        super(
                EntityDamageEvent.class,
                PowerHitTaken.class,
                Double.class,
                Optional.class,
                "HIT_TAKEN");
    }

    public HitTaken(String name) {
        super(
                name,
                "HIT_TAKEN",
                EntityDamageEvent.class,
                PowerHitTaken.class,
                Double.class,
                Optional.class);
    }

    @Override
    public Optional<Double> def(Player player, ItemStack i, EntityDamageEvent event) {
        return Optional.empty();
    }

    @Override
    public Optional<Double> next(Optional<Double> a, PowerResult<Double> b) {
        return b.isOK() ? Optional.ofNullable(minWithCancel(a.orElse(null), b.data())) : a;
    }

    @Override
    public PowerResult<Double> warpResult(
            PowerResult<Void> overrideResult,
            TPower power,
            PowerHitTaken<TPower> pimpl,
            Player player,
            ItemStack i,
            EntityDamageEvent event) {
        return overrideResult.with(event.getDamage());
    }

    @Override
    public PowerResult<Double> run(
            TPower power,
            PowerHitTaken<TPower> pimpl,
            Player player,
            ItemStack i,
            EntityDamageEvent event) {
        return pimpl.takeHit(power, player, i, event.getDamage(), event);
    }

    @Override
    public boolean check(Player player, ItemStack i, EntityDamageEvent event) {
        return event.getDamage() > minDamage && event.getDamage() < maxDamage;
    }
}

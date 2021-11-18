package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerHitTaken;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

import java.util.Optional;

import static think.rpgitems.power.Utils.minWithCancel;

class HitTaken extends Trigger<EntityDamageEvent, PowerHitTaken, Double, Optional<Double>> {

    @Property
    public double minDamage = Double.NEGATIVE_INFINITY;

    @Property
    public double maxDamage = Double.POSITIVE_INFINITY;

    HitTaken() {
        super(EntityDamageEvent.class, PowerHitTaken.class, Double.class, Optional.class, "HIT_TAKEN");
    }

    public HitTaken(String name) {
        super(name, "HIT_TAKEN", EntityDamageEvent.class, PowerHitTaken.class, Double.class, Optional.class);
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
    public PowerResult<Double> warpResult(PowerResult<Void> overrideResult, PowerHitTaken power, Player player, ItemStack i, EntityDamageEvent event) {
        return overrideResult.with(event.getDamage());
    }

    @Override
    public PowerResult<Double> run(PowerHitTaken power, Player player, ItemStack i, EntityDamageEvent event) {
        return power.takeHit(player, i, event.getDamage(), event);
    }

    @Override
    public boolean check(Player player, ItemStack i, EntityDamageEvent event) {
        return event.getDamage() > minDamage && event.getDamage() < maxDamage;
    }
}

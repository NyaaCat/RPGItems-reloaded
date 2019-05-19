package think.rpgitems.trigger;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerHit;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Trigger;

import java.util.Optional;

import static think.rpgitems.power.Utils.maxWithCancel;

public class Hit extends Trigger<EntityDamageByEntityEvent, PowerHit, Double, Optional<Double>> {
    public static final Trigger<EntityDamageByEntityEvent, PowerHit, Double, Optional<Double>> HIT = new Hit();

    public Hit() {
        super(EntityDamageByEntityEvent.class, PowerHit.class, Double.class, Optional.class, "HIT");
    }

    @Override
    public Optional<Double> def(Player player, ItemStack i, EntityDamageByEntityEvent event) {
        return Optional.empty();
    }

    @Override
    public Optional<Double> next(Optional<Double> a, PowerResult<Double> b) {
        return b.isOK() ? Optional.ofNullable(maxWithCancel(a.orElse(null), b.data())) : a;
    }

    @Override
    public PowerResult<Double> warpResult(PowerResult<Void> overrideResult, PowerHit power, Player player, ItemStack i, EntityDamageByEntityEvent event) {
        return overrideResult.with(event.getDamage());
    }

    @Override
    public PowerResult<Double> run(PowerHit power, Player player, ItemStack i, EntityDamageByEntityEvent event) {
        return power.hit(player, i, (LivingEntity) event.getEntity(), event.getDamage(), event);
    }
}

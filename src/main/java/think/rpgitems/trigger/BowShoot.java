package think.rpgitems.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerBowShoot;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Trigger;

import java.util.Optional;

import static think.rpgitems.power.Utils.maxWithCancel;

public class BowShoot extends Trigger<EntityShootBowEvent, PowerBowShoot, Float, Optional<Float>> {
    public static final Trigger<EntityShootBowEvent, PowerBowShoot, Float, Optional<Float>> BOW_SHOOT = new BowShoot();

    public BowShoot() {
        super(EntityShootBowEvent.class, PowerBowShoot.class, Float.class, Optional.class, "BOW_SHOOT");
    }

    @Override
    public Optional<Float> def(Player player, ItemStack i, EntityShootBowEvent event) {
        return Optional.empty();
    }

    @Override
    public Optional<Float> next(Optional<Float> a, PowerResult<Float> b) {
        return b.isOK() ? Optional.ofNullable(maxWithCancel(a.orElse(null), b.data())) : a;
    }

    @Override
    public PowerResult<Float> warpResult(PowerResult<Void> overrideResult, PowerBowShoot power, Player player, ItemStack i, EntityShootBowEvent event) {
        return overrideResult.with(event.getForce());
    }

    @Override
    public PowerResult<Float> run(PowerBowShoot power, Player player, ItemStack i, EntityShootBowEvent event) {
        return power.bowShoot(player, i, event);
    }
}

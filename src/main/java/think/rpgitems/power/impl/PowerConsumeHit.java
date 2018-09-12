package think.rpgitems.power.impl;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.PowerHit;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power consumehit.
 * <p>
 * The consume power will remove one item when player hits something. With {@link #cooldown cooldown} time (ticks).
 * </p>
 */
@PowerMeta(immutableTrigger = true)
public class PowerConsumeHit extends BasePower implements PowerHit {
    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public int cooldown = 0;

    @Override
    public PowerResult<Double> hit(final Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (!checkCooldown(this, player, cooldown, false)) return PowerResult.cd();
        int count = stack.getAmount() - 1;
        if (count == 0) {
            stack.setAmount(0);
            stack.setType(Material.AIR);
        } else {
            stack.setAmount(count);
        }
        return PowerResult.ok(damage);
    }

    @Override
    public String getName() {
        return "consumehit";
    }

    @Override
    public String displayText() {
        return I18n.format("power.consumehit");
    }
}

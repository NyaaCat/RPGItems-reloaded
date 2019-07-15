package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;
import think.rpgitems.power.Property;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power tntcannon.
 * <p>
 * The tntcannon power will fire active tnt on right click.
 * </p>
 */
@PowerMeta(immutableTrigger = true)
public class PowerTNTCannon extends BasePower implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 0;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        TNTPrimed tnt = player.getWorld().spawn(player.getLocation().add(0, 1.8, 0), TNTPrimed.class);
        tnt.setVelocity(player.getLocation().getDirection().multiply(2d));
        return PowerResult.ok();
    }

    @Override
    public String displayText() {
        return I18n.format("power.tntcannon", (double) cooldown / 20d);
    }

    @Override
    public String getName() {
        return "tntcannon";
    }
}

package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power arrow.
 * <p>
 * The arrow power will fire an arrow on right click.
 * </p>
 */
@PowerMeta(defaultTrigger = TriggerType.RIGHT_CLICK)
public class PowerArrow extends BasePower implements PowerRightClick, PowerLeftClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @SuppressWarnings("deprecation")
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        Events.rpgProjectiles.put(arrow.getEntityId(), getItem().getUID());
        Events.removeArrows.add(arrow.getEntityId());
        arrow.setPersistent(false);
        return PowerResult.ok();
    }

    @Override
    public String displayText() {
        return I18n.format("power.arrow", (double) cooldown / 20d);
    }

    @Override
    public String getName() {
        return "arrow";
    }
}

package think.rpgitems.power.impl;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power color.
 * <p>
 * The power allows changing the color of glass, clay and wool.
 * You can choose which blocks are paintable by providing three true/false statements
 * for the order: glass, clay, wool.
 * </p>
 */
@PowerMeta(defaultTrigger = {TriggerType.LEFT_CLICK, TriggerType.RIGHT_CLICK})
public class PowerColor extends BasePower implements PowerRightClick, PowerLeftClick {

    /**
     * Cooldown time of this power
     */
    public long cooldown = 0;
    /**
     * Whether enabled on glass.
     */
    public boolean glass = true;
    /**
     * Whether enabled on clay.
     */
    public boolean clay = true;
    /**
     * Whether enabled on wool.
     */
    public boolean wool = true;
    /**
     * Cost of this power
     */
    public int cost = 0;

    @SuppressWarnings("deprecation")
    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        // TODO
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        return PowerResult.noop();
    }

    @Override
    public String displayText() {
        return null;
    }

    @Override
    public String getName() {
        return "color";
    }
}

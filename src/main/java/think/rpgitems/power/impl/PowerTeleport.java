package think.rpgitems.power.impl;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power teleport.
 * <p>
 * The teleport power will teleport you
 * in the direction you're looking in
 * or to the place where the projectile hit
 * with maximum distance of {@link #distance} blocks
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = {"RIGHT_CLICK", "PROJECTILE_HIT"})
public class PowerTeleport extends BasePower implements PowerRightClick, PowerProjectileHit {

    /**
     * Maximum distance.
     */
    @Property(order = 1)
    public int distance = 5;
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
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        World world = player.getWorld();
        Location start = player.getLocation();
        start.setY(start.getY() + 1.6);
        Block lastSafe = world.getBlockAt(start);

        BlockIterator bi = new BlockIterator(player, distance);
        while (bi.hasNext()) {
            Block block = bi.next();
            if (!block.getType().isSolid() || (block.getType() == Material.AIR)) {
                lastSafe = block;
            } else {
                break;
            }
        }

        Location newLoc = lastSafe.getLocation();
        newLoc.setPitch(start.getPitch());
        newLoc.setYaw(start.getYaw());
        Vector velocity = player.getVelocity();
        boolean gliding = player.isGliding();
        player.teleport(newLoc);
        if (gliding) {
            player.setVelocity(velocity);
        }
        world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
        world.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
        return PowerResult.ok();
    }

    @Override
    public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        World world = player.getWorld();
        Location start = player.getLocation();
        Location newLoc = event.getEntity().getLocation();
        if (start.distanceSquared(newLoc) >= distance * distance) {
            player.sendMessage(I18n.format("message.too.far"));
            return PowerResult.noop();
        }
        newLoc.setPitch(start.getPitch());
        newLoc.setYaw(start.getYaw());
        player.teleport(newLoc);
        world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
        world.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
        return PowerResult.ok();
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String displayText() {
        return I18n.format("power.teleport", distance, (double) cooldown / 20d);
    }

}

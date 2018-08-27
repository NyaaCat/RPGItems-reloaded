/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.power.impl;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
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
@PowerMeta(defaultTrigger = {TriggerType.RIGHT_CLICK, TriggerType.PROJECTILE_HIT})
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
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        World world = player.getWorld();
        Location start = player.getLocation();
        start.setY(start.getY() + 1.6);
        Block lastSafe = world.getBlockAt(start);
        try {
            BlockIterator bi = new BlockIterator(player, distance);
            while (bi.hasNext()) {
                Block block = bi.next();
                if (!block.getType().isSolid() || (block.getType() == Material.AIR)) {
                    lastSafe = block;
                } else {
                    break;
                }
            }
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            RPGItems.logger.info("This exception may be harmless");
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
    public PowerResult<Void> projectileHit(Player player, ItemStack stack, Projectile p, ProjectileHitEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        World world = player.getWorld();
        Location start = player.getLocation();
        Location newLoc = p.getLocation();
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

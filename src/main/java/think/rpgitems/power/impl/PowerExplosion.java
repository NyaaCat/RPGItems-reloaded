package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import java.util.concurrent.ThreadLocalRandom;

@PowerMeta(defaultTrigger = {"PROJECTILE_HIT"})
public class PowerExplosion extends BasePower implements PowerLeftClick, PowerRightClick, PowerPlain, PowerHit, PowerProjectileHit, PowerLocation {

    @Property
    public int distance = 20;

    /**
     * Chance of triggering this power
     */
    @Property
    public double chance = 20;


    @Property
    public float power = 4.0f;

    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;


    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        Block targetBlock = player.getTargetBlock(null, distance);
        if (targetBlock == null) return PowerResult.noop();
        return fire(player, stack, targetBlock.getLocation());
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        Location location = entity.getLocation();
        Location start = player.getLocation();
        if (start.distanceSquared(location) >= distance * distance) {
            player.sendMessage(I18n.format("message.too.far"));
            return PowerResult.noop();
        }
        return fire(player, stack, location).with(damage);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack, Location location) {
        if (ThreadLocalRandom.current().nextDouble(100) < chance) {
            if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
            boolean explosion = NmsUtils.createExplosion(location.getWorld(), player, location.getX(), location.getY(), location.getZ(), power, false, false);
            return explosion ? PowerResult.ok() : PowerResult.fail();
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
        Projectile hit = event.getEntity();
        Location location = hit.getLocation();
        Location start = player.getLocation();
        if (start.distanceSquared(location) >= distance * distance) {
            player.sendMessage(I18n.format("message.too.far"));
            return PowerResult.noop();
        }
        return fire(player, stack, location);
    }

    @Override
    public String displayText() {
        return I18n.format("power.explosion", chance, power);
    }

    @Override
    public String getName() {
        return "explosion";
    }
}

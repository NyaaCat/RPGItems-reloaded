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

@PowerMeta(defaultTrigger = {"PROJECTILE_HIT"}, generalInterface = PowerPlain.class, implClass = PowerExplosion.Impl.class)
public class PowerExplosion extends BasePower {

    @Property
    private int distance = 20;

    @Property
    private double chance = 20;


    @Property(alias = "power")
    private float explosionPower = 4.0f;

    @Property
    private int cost = 0;

    public class Impl implements PowerLeftClick, PowerRightClick, PowerPlain, PowerHit, PowerProjectileHit, PowerLocation {

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            Block targetBlock = player.getTargetBlock(null, getDistance());
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
            if (start.distanceSquared(location) >= getDistance() * getDistance()) {
                player.sendMessage(I18n.format("message.too.far"));
                return PowerResult.noop();
            }
            return fire(player, stack, location).with(damage);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, Location location) {
            if (ThreadLocalRandom.current().nextDouble(100) < getChance()) {
                if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
                boolean explosion = NmsUtils.createExplosion(location.getWorld(), player, location.getX(), location.getY(), location.getZ(), getExplosionPower(), false, false);
                return explosion ? PowerResult.ok() : PowerResult.fail();
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
            Projectile hit = event.getEntity();
            Location location = hit.getLocation();
            Location start = player.getLocation();
            if (start.distanceSquared(location) >= getDistance() * getDistance()) {
                player.sendMessage(I18n.format("message.too.far"));
                return PowerResult.noop();
            }
            return fire(player, stack, location);
        }

        @Override
        public Power getPower() {
            return PowerExplosion.this;
        }
    }

    @Override
    public String displayText() {
        return I18n.format("power.explosion", getChance(), getExplosionPower());
    }

    /**
     * Chance of triggering this power
     */
    public double getChance() {
        return chance;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    public int getDistance() {
        return distance;
    }

    public float getExplosionPower() {
        return explosionPower;
    }

    @Override
    public String getName() {
        return "explosion";
    }
}

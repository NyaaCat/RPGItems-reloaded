package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.data.Context;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;

@Meta(defaultTrigger = "HIT", implClass = Headshot.Impl.class)
public class Headshot extends BasePower {

    @Property
    public double factor = 1.5;

    @Property
    public int cost = 0;

    @Property
    public boolean particleEnemy = true;

    @Property
    public boolean soundSelf = true;

    @Property
    public boolean soundEnemy = false;

    @Property
    public boolean setBaseDamage = false;

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    @Override
    public String getName() {
        return "headshot";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.headshot", getFactor());
    }

    public double getFactor() {
        return factor;
    }

    public boolean isParticleEnemy() {
        return particleEnemy;
    }

    public boolean isSetBaseDamage() {
        return setBaseDamage;
    }

    public boolean isSoundEnemy() {
        return soundEnemy;
    }

    public boolean isSoundSelf() {
        return soundSelf;
    }

    public class Impl implements PowerHit, PowerBeamHit{

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            if (!(event.getDamager() instanceof Projectile)) {
                return PowerResult.noop();
            }
            Projectile damager = (Projectile) event.getDamager();
            return check(player, entity, stack, damage, damager.getVelocity(), damager.getBoundingBox(), entity.getBoundingBox(), event);
        }

        private PowerResult<Double> check(Player player, LivingEntity entity, ItemStack stack, double damage, Vector velocity, BoundingBox damagerOrigBb, BoundingBox entityBb, EntityDamageByEntityEvent event){
            double maxAxis = Math.max(entityBb.getHeight(), Math.max(entityBb.getWidthX(), entityBb.getWidthZ()));
            double minAxis = Math.min(entityBb.getHeight(), Math.min(entityBb.getWidthX(), entityBb.getWidthZ()));
            double maximum = minAxis * minAxis;
            double ds = entityBb.getMax().distanceSquared(entityBb.getMin()) - maxAxis * maxAxis;
            Vector head;
            if (entityBb.getHeight() * entityBb.getHeight() > ds) {
                head = entity.getEyeLocation().toVector();
            } else {
                Vector lsd = entity.getEyeLocation().getDirection().multiply(maxAxis);
                Vector ls = entity.getEyeLocation().toVector().add(lsd);
                lsd.multiply(-1);
                head = entityBb.rayTrace(ls, lsd, maxAxis).getHitPosition();
            }

            Pair<Vector, Vector> sweep = Utils.sweep(damagerOrigBb, entityBb, velocity);
            if (sweep == null) return PowerResult.fail();
            BoundingBox damagerBb = damagerOrigBb.shift(sweep.getKey());
            boolean hs;
            if (sweep.getValue() == null) {
                hs = damagerBb.contains(head);
            } else {
                Vector hitPoint = Utils.hitPoint(damagerBb, sweep.getValue());
                double squared = hitPoint.distanceSquared(head);
                hs = squared <= maximum;
            }
            if (hs) {
                Context.instance().putExpiringSeconds(player.getUniqueId(), "headshot.target", entity, 3);
                if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
                if (isSoundSelf()) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 3);
                }
                if (isSoundEnemy()) {
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GHAST_HURT, 1, 3);
                }
                if (isParticleEnemy()) {
                    entity.getWorld().spawnParticle(Particle.REDSTONE, entity.getLocation(), 2, new Particle.DustOptions(Color.RED, 10));
                }
                if (isSetBaseDamage()) {
                    event.setDamage(damage * getFactor());
                }
                return PowerResult.ok(damage * getFactor());
            }
            return PowerResult.fail();
        }

        @Override
        public Power getPower() {
            return Headshot.this;
        }

        @Override
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            EntityDamageByEntityEvent fake = new EntityDamageByEntityEvent(player, player, EntityDamageEvent.DamageCause.CUSTOM, damage);
            PowerResult<Double> check = check(player, entity, stack, damage, event.getVelocity(), event.getBoundingBox(), entity.getBoundingBox(), fake);
            event.setDamage(fake.getDamage());
            return check;
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            return PowerResult.fail();
        }

        @Override
        public PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event) {
            return PowerResult.fail();
        }
    }
}
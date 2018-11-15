package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

@PowerMeta(immutableTrigger = true)
public class PowerHeadshot extends BasePower implements PowerHit {

    @Property
    public double factor = 1.5;

    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) {
            return PowerResult.noop();
        }
        Projectile damager = (Projectile) event.getDamager();
        Vector velocity = damager.getVelocity();
        BoundingBox damagerOrigBb = damager.getBoundingBox();
        BoundingBox entityBb = entity.getBoundingBox();
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
            if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
            damager.getWorld().playSound(damager.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 3);
            damager.getWorld().spawnParticle(Particle.REDSTONE, damager.getLocation(), 2, new Particle.DustOptions(Color.RED, 10));
            return PowerResult.ok(damage * factor);
        }
        return PowerResult.fail();
    }

    @Override
    public String displayText() {
        return I18n.format("power.headshot", factor);
    }

    @Override
    public String getName() {
        return "headshot";
    }
}

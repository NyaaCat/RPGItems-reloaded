package think.rpgitems.power.impl;

import com.udojava.evalex.Expression;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.math.BigDecimal;
import java.util.logging.Level;

/**
 * Power EvalDamage.
 */
@Meta(defaultTrigger = "HIT", implClass = EvalDamage.Impl.class)
public class EvalDamage extends BasePower {

    @Property
    public String display;

    @Property(required = true)
    public String expression = "";

    @Property
    public boolean setBaseDamage = false;

    public String getExpression() {
        return expression;
    }

    @Override
    public String getName() {
        return "evaldamage";
    }

    @Override
    public String displayText() {
        return getDisplay() != null ? getDisplay() : "Damage may vary based on environment";
    }

    public String getDisplay() {
        return display;
    }

    public boolean isSetBaseDamage() {
        return setBaseDamage;
    }

    public class Impl implements PowerHit, PowerHitTaken {
        // Feel free to add variable below
        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            try {
                Expression ex = new Expression(getExpression());
                boolean byProjectile = false;
                Entity damager = event.getDamager();
                if (damager instanceof Projectile) {
                    byProjectile = true;
                }
                ex
                        .and("damage", BigDecimal.valueOf(damage))
                        .and("damagerType", damager.getType().name())
                        .and("isDamageByProjectile", byProjectile ? BigDecimal.ONE : BigDecimal.ZERO)
                        .and("damagerTicksLived", Utils.lazyNumber(() -> (double) damager.getTicksLived()))
                        .and("finalDamage", Utils.lazyNumber(event::getFinalDamage))
                        .and("distance", Utils.lazyNumber(() -> player.getLocation().distance(entity.getLocation())))
                        .and("playerYaw", Utils.lazyNumber(() -> (double) player.getLocation().getYaw()))
                        .and("playerPitch", Utils.lazyNumber(() -> (double) player.getLocation().getPitch()))
                        .and("playerX", Utils.lazyNumber(() -> player.getLocation().getX()))
                        .and("playerY", Utils.lazyNumber(() -> player.getLocation().getY()))
                        .and("playerZ", Utils.lazyNumber(() -> player.getLocation().getZ()))
                        .and("entityType", entity.getType().name())
                        .and("entityYaw", Utils.lazyNumber(() -> (double) entity.getLocation().getYaw()))
                        .and("entityPitch", Utils.lazyNumber(() -> (double) entity.getLocation().getPitch()))
                        .and("entityX", Utils.lazyNumber(() -> entity.getLocation().getX()))
                        .and("entityY", Utils.lazyNumber(() -> entity.getLocation().getY()))
                        .and("entityZ", Utils.lazyNumber(() -> entity.getLocation().getZ()))
                        .and("entityLastDamage", Utils.lazyNumber(entity::getLastDamage))
                        .and("cause", event.getCause().name())
                        .addLazyFunction(Utils.scoreBoard(player))
                ;

                BigDecimal result = ex.eval();
                double ret = result.doubleValue();
                if (isSetBaseDamage()) {
                    event.setDamage(ret);
                }
                return PowerResult.ok(ret);
            } catch (Expression.ExpressionException ex) {
                RPGItems.plugin.getLogger().log(Level.WARNING, "bad expression: " + getExpression(), ex);
                if (player.isOp() || player.hasPermission("rpgitem")) {
                    player.sendMessage("bad expression: " + getExpression());
                    player.sendMessage(ex.getMessage());
                }
                return PowerResult.fail();
            }
        }

        @Override
        public PowerResult<Double> takeHit(Player player, ItemStack stack, double damage, EntityDamageEvent event) {
            boolean byEntity = event instanceof EntityDamageByEntityEvent;
            try {

                Expression ex = new Expression(getExpression());
                ex
                        .and("damage", BigDecimal.valueOf(damage))
                        .and("finalDamage", Utils.lazyNumber(event::getFinalDamage))
                        .and("isDamageByEntity", byEntity ? BigDecimal.ONE : BigDecimal.ZERO)
                        .and("playerYaw", Utils.lazyNumber(() -> (double) player.getLocation().getYaw()))
                        .and("playerPitch", Utils.lazyNumber(() -> (double) player.getLocation().getPitch()))
                        .and("playerX", Utils.lazyNumber(() -> player.getLocation().getX()))
                        .and("playerY", Utils.lazyNumber(() -> player.getLocation().getY()))
                        .and("playerZ", Utils.lazyNumber(() -> player.getLocation().getZ()))
                        .and("playerLastDamage", Utils.lazyNumber(player::getLastDamage))
                        .and("cause", event.getCause().name());
                ex.addLazyFunction(Utils.scoreBoard(player));
                ex.addLazyFunction(Utils.context(player));
                ex.addLazyFunction(Utils.now());

                if (byEntity) {
                    boolean byProjectile = false;
                    Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                    Entity ent = damager;
                    if (ent instanceof Projectile) {
                        ProjectileSource shooter = ((Projectile) ent).getShooter();
                        if (shooter instanceof Entity) {
                            ent = (Entity) shooter;
                            byProjectile = true;
                        }
                    }
                    Entity entity = ent;
                    ex
                            .and("damagerType", damager.getType().name())
                            .and("isDamageByProjectile", byProjectile ? BigDecimal.ONE : BigDecimal.ZERO)
                            .and("damagerTicksLived", Utils.lazyNumber(() -> (double) damager.getTicksLived()))
                            .and("distance", Utils.lazyNumber(() -> player.getLocation().distance(entity.getLocation())))
                            .and("entityType", entity.getType().name())
                            .and("entityYaw", Utils.lazyNumber(() -> (double) entity.getLocation().getYaw()))
                            .and("entityPitch", Utils.lazyNumber(() -> (double) entity.getLocation().getPitch()))
                            .and("entityX", Utils.lazyNumber(() -> entity.getLocation().getX()))
                            .and("entityY", Utils.lazyNumber(() -> entity.getLocation().getY()))
                            .and("entityZ", Utils.lazyNumber(() -> entity.getLocation().getZ()));
                }

                BigDecimal result = ex.eval();
                double ret = result.doubleValue();
                if (isSetBaseDamage()) {
                    event.setDamage(ret);
                }
                return PowerResult.ok(result.doubleValue());
            } catch (Expression.ExpressionException ex) {
                RPGItems.plugin.getLogger().log(Level.WARNING, "bad expression: " + getExpression(), ex);
                if (player.isOp() || player.hasPermission("rpgitem")) {
                    player.sendMessage("bad expression: " + getExpression());
                    player.sendMessage(ex.getMessage());
                }
                return PowerResult.fail();
            }
        }

        @Override
        public Power getPower() {
            return EvalDamage.this;
        }
    }
}

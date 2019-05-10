package think.rpgitems.power.impl;

import com.udojava.evalex.Expression;
import com.udojava.evalex.LazyFunction;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scoreboard.Objective;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Power EvalDamage.
 */
@PowerMeta(defaultTrigger = "HIT")
public class PowerEvalDamage extends BasePower implements PowerHit, PowerHitTaken {

    @Property
    public String display;

    @Property(required = true)
    public String expression = "";

    // Feel free to add variable below

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        try {
            Expression ex = new Expression(expression);
            boolean byProjectile = false;
            Entity damager = event.getDamager();
            if (damager instanceof Projectile) {
                byProjectile = true;
            }
            ex
                    .and("damage", BigDecimal.valueOf(damage))
                    .and("damagerType", damager.getType().name())
                    .and("isDamageByProjectile", byProjectile ? BigDecimal.ONE : BigDecimal.ZERO)
                    .and("damagerTicksLived", lazyNumber(() -> (double) damager.getTicksLived()))
                    .and("finalDamage", lazyNumber(event::getFinalDamage))
                    .and("distance", lazyNumber(() -> player.getLocation().distance(entity.getLocation())))
                    .and("playerYaw", lazyNumber(() -> (double) player.getLocation().getYaw()))
                    .and("playerPitch", lazyNumber(() -> (double) player.getLocation().getPitch()))
                    .and("playerX", lazyNumber(() -> player.getLocation().getX()))
                    .and("playerY", lazyNumber(() -> player.getLocation().getY()))
                    .and("playerZ", lazyNumber(() -> player.getLocation().getZ()))
                    .and("entityType", entity.getType().name())
                    .and("entityYaw", lazyNumber(() -> (double) entity.getLocation().getYaw()))
                    .and("entityPitch", lazyNumber(() -> (double) entity.getLocation().getPitch()))
                    .and("entityX", lazyNumber(() -> entity.getLocation().getX()))
                    .and("entityY", lazyNumber(() -> entity.getLocation().getY()))
                    .and("entityZ", lazyNumber(() -> entity.getLocation().getZ()))
                    .and("entityLastDamage", lazyNumber(entity::getLastDamage))
                    .and("cause", event.getCause().name())
                    .addLazyFunction(scoreBoard(player))
            ;

            BigDecimal result = ex.eval();
            return PowerResult.ok(result.doubleValue());
        } catch (Expression.ExpressionException ex) {
            RPGItem.getPlugin().getLogger().log(Level.WARNING, "bad expression: " + expression, ex);
            if (player.isOp() || player.hasPermission("rpgitem")) {
                player.sendMessage("bad expression: " + expression);
                player.sendMessage(ex.getMessage());
            }
            return PowerResult.fail();
        }
    }

    private static Expression.LazyNumber lazyNumber(Supplier<Double> f) {
        return new Expression.LazyNumber() {
            @Override
            public BigDecimal eval() {
                return BigDecimal.valueOf(f.get());
            }

            @Override
            public String getString() {
                return null;
            }
        };
    }

    @Override
    public PowerResult<Double> takeHit(Player player, ItemStack stack, double damage, EntityDamageEvent event) {
        boolean byEntity = event instanceof EntityDamageByEntityEvent;
        try {

            Expression ex = new Expression(expression);
            ex
                    .and("damage", BigDecimal.valueOf(damage))
                    .and("finalDamage", lazyNumber(event::getFinalDamage))
                    .and("isDamageByEntity", byEntity ? BigDecimal.ONE : BigDecimal.ZERO)
                    .and("playerYaw", lazyNumber(() -> (double) player.getLocation().getYaw()))
                    .and("playerPitch", lazyNumber(() -> (double) player.getLocation().getPitch()))
                    .and("playerX", lazyNumber(() -> player.getLocation().getX()))
                    .and("playerY", lazyNumber(() -> player.getLocation().getY()))
                    .and("playerZ", lazyNumber(() -> player.getLocation().getZ()))
                    .and("playerLastDamage", lazyNumber(player::getLastDamage))
                    .and("cause", event.getCause().name())
                    .addLazyFunction(scoreBoard(player));

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
                        .and("damagerTicksLived", lazyNumber(() -> (double) damager.getTicksLived()))
                        .and("distance", lazyNumber(() -> player.getLocation().distance(entity.getLocation())))
                        .and("entityType", entity.getType().name())
                        .and("entityYaw", lazyNumber(() -> (double) entity.getLocation().getYaw()))
                        .and("entityPitch", lazyNumber(() -> (double) entity.getLocation().getPitch()))
                        .and("entityX", lazyNumber(() -> entity.getLocation().getX()))
                        .and("entityY", lazyNumber(() -> entity.getLocation().getY()))
                        .and("entityZ", lazyNumber(() -> entity.getLocation().getZ()));
            }

            BigDecimal result = ex.eval();
            return PowerResult.ok(result.doubleValue());
        } catch (Expression.ExpressionException ex) {
            RPGItem.getPlugin().getLogger().log(Level.WARNING, "bad expression: " + expression, ex);
            if (player.isOp() || player.hasPermission("rpgitem")) {
                player.sendMessage("bad expression: " + expression);
                player.sendMessage(ex.getMessage());
            }
            return PowerResult.fail();
        }
    }

    @Override
    public String getName() {
        return "evaldamage";
    }

    @Override
    public String displayText() {
        return display != null ? display : "Damage may vary based on environment";
    }

    private LazyFunction scoreBoard(Player player) {
        return new LazyFunction() {
            @Override
            public String getName() {
                return "playerScoreBoard";
            }

            @Override
            public int getNumParams() {
                return 2;
            }

            @Override
            public boolean numParamsVaries() {
                return false;
            }

            @Override
            public boolean isBooleanFunction() {
                return false;
            }

            @Override
            public Expression.LazyNumber lazyEval(List<Expression.LazyNumber> lazyParams) {
                Objective objective = player.getScoreboard().getObjective(lazyParams.get(0).getString());
                if (objective == null) {
                    return lazyParams.get(1);
                }
                return lazyNumber(() -> (double) objective.getScore(player.getName()).getScore());
            }
        };
    }
}

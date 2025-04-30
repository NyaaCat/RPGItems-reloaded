package think.rpgitems.power.impl;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.checkCooldown;

@Meta(defaultTrigger = "RIGHT_CLICK", implClass = Dash.Impl.class)
public class Dash extends BasePower {
    public enum Direction {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        UP,
        DOWN,
        RANDOM,
        RANDOM_HORIZONTAL,
        RANDOM_VERTICAL,
        WEST,
        EAST,
        NORTH,
        SOUTH
    }

    @Property
    public Direction direction = Direction.FORWARD;

    @Property
    public int cooldown = 0;

    @Property
    public int cost = 0;

    @Property
    public double speed = 1;

    @Override
    public String getName() {
        return "dash";
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getCost() {
        return cost;
    }

    public Direction getDirection() {
        return direction;
    }

    public double getSpeed() {
        return speed;
    }


    @Override
    public String displayText() {
        return I18n.formatDefault("power.dash",getDirection(), (double) getCooldown() / 20d);
    }

    public class Impl implements PowerRightClick, PowerPlain, PowerLeftClick, PowerSneak, PowerHurt, PowerHit, PowerHitTaken, PowerSwim, PowerJump, PowerBowShoot, PowerOffhandClick, PowerTick {

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public Power getPower() {
            return Dash.this;
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack).with(damage);
        }

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            return fire(target, stack).with(damage);
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            return fire(target, stack);
        }

        @Override
        public PowerResult<Void> jump(Player player, ItemStack stack, PlayerJumpEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> offhandClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack s) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player, s, getPower());
            if (!powerEvent.callEvent())
                return PowerResult.fail();
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(s, getCost())) return PowerResult.cost();
            double speed = getSpeed();
            switch (getDirection()) {
                case FORWARD:
                    player.setVelocity(player.getLocation().getDirection().multiply(speed));
                    break;
                case BACKWARD:
                    player.setVelocity(player.getLocation().getDirection().multiply(-speed));
                    break;
                case LEFT:
                    player.setVelocity(player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(-speed));
                    break;
                case RIGHT:
                    player.setVelocity(player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed));
                    break;
                case UP:
                    player.setVelocity(player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setY(speed));
                    break;
                case DOWN:
                    player.setVelocity(player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setY(-speed));
                    break;
                case RANDOM:
                    player.setVelocity(player.getLocation().getDirection().add(new Vector(Math.random(), Math.random(), Math.random())).normalize().multiply(speed));
                    break;
                case RANDOM_HORIZONTAL:
                    player.setVelocity(player.getLocation().getDirection().add(new Vector(Math.random(), 0, Math.random())).normalize().multiply(speed));
                    break;
                case RANDOM_VERTICAL:
                    player.setVelocity(player.getLocation().getDirection().add(new Vector(0, Math.random(), 0)).normalize().multiply(speed));
                    break;
                case EAST:
                    player.setVelocity(player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setX(speed));
                    break;
                case WEST:
                    player.setVelocity(player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setX(-speed));
                    break;
                case NORTH:
                    player.setVelocity(player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setZ(-speed));
                    break;
                case SOUTH:
                    player.setVelocity(player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setZ(speed));
                    break;
                default:
                    return PowerResult.fail();
            }

            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> swim(Player player, ItemStack stack, EntityToggleSwimEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return fire(player, stack);
        }
    }

}

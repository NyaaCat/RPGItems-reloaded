package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.BaseTriggers;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

import static think.rpgitems.power.Utils.checkAndSetCooldown;

/**
 * Power throw.
 * <p>
 * Spawn and throw an entity
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = Throw.Impl.class)
public class Throw extends BasePower {
    @Property(order = 5, required = true)
    public String entityData = "";
    @Property(order = 4)
    public String entityName = "";
    @Property(order = 1)
    public int cooldown = 0;
    @Property(order = 3)
    public double speed = 3;
    @Property(order = 0)
    public String display = "throw entity";
    @Property(order = 6)
    public boolean isPersistent;
    @Property
    public int cost = 0;

    @Property
    public boolean requireHurtByEntity = true;

    @Override
    public void init(ConfigurationSection section) {
        boolean isRight = section.getBoolean("isRight", true);
        triggers = Collections.singleton(isRight ? BaseTriggers.RIGHT_CLICK : BaseTriggers.LEFT_CLICK);
        super.init(section);
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getCost() {
        return cost;
    }

    public String getEntityData() {
        return entityData;
    }

    public String getEntityName() {
        return entityName;
    }

    @Override
    public String getName() {
        return "throw";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + getDisplay();
    }

    public String getDisplay() {
        return display;
    }

    public double getSpeed() {
        return speed;
    }

    public boolean isPersistent() {
        return isPersistent;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerPlain, PowerBowShoot, PowerHurt, PowerHitTaken {

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("entity",getEntityName());
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (checkAndSetCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true, getItem().getUid() + "." + getEntityName() + getEntityData()) && getItem().consumeDurability(stack, getCost())) {
                summonEntity(player);
                return PowerResult.ok();
            }
            return PowerResult.noop();
        }

        @Override
        public Power getPower() {
            return Throw.this;
        }

        private void summonEntity(Player player) {
            Location loc = player.getEyeLocation().clone();
//            String s = getEntityData().replaceAll("\\{player}", player.getName()).replaceAll("\\{playerUUID}", player.getUniqueId().toString());
            Consumer<Entity> entityConsumer = (Entity entity) ->{
//                NmsUtils.setEntityTag(entity, s);
                entity.setRotation(loc.getYaw(), loc.getPitch());
                if (entity instanceof Projectile) {
                    ((Projectile) entity).setShooter(player);
                }
                entity.setVelocity(loc.getDirection().multiply(getSpeed()));
                entity.setPersistent(isPersistent());
            };
            player.getWorld().spawnEntity(loc, EntityType.valueOf(getEntityName()), CreatureSpawnEvent.SpawnReason.CUSTOM,entityConsumer);
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(player, itemStack).with(e.getForce());
        }
    }
}
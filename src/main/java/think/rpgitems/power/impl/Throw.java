package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.utils.NmsUtils;
import cat.nyaa.nyaacore.utils.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.BaseTriggers;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;

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
    @Property(order = 3)
    public double speed = 3;
    @Property(order = 0)
    public String display = "throw entity";
    @Property(order = 6)
    public boolean isPersistent;

    @Property
    public boolean requireHurtByEntity = true;

    @Override
    public void init(ConfigurationSection section) {
        boolean isRight = section.getBoolean("isRight", true);
        triggers = Collections.singleton(isRight ? BaseTriggers.RIGHT_CLICK : BaseTriggers.LEFT_CLICK);
        super.init(section);
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

    public static class Impl implements PowerRightClick<Throw>, PowerLeftClick<Throw>, PowerPlain<Throw>, PowerBowShoot<Throw>, PowerHurt<Throw>, PowerHitTaken<Throw> {

        @Override
        public PowerResult<Double> takeHit(Throw power, Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Throw power, Player player, ItemStack stack) {
            summonEntity(power, player);
            return PowerResult.ok();
        }

        @Override
        public Class<? extends Throw> getPowerClass() {
            return Throw.class;
        }

        @SuppressWarnings("deprecation")
        private void summonEntity(Throw power, Player player) {
            try {
                Location loc = player.getEyeLocation().clone();
                Class<?> mojangsonParser = ReflectionUtils.getNMSClass("MojangsonParser");
                Method getTagFromJson = mojangsonParser.getMethod("parse", String.class);
                Class<?> nbtTagCompound = ReflectionUtils.getNMSClass("NBTTagCompound");
                Method setString = nbtTagCompound.getMethod("setString", String.class, String.class);
                Entity entity = player.getWorld().spawnEntity(loc, EntityType.valueOf(power.getEntityName()));
                Object nbt;
                String s = power.getEntityData().replaceAll("\\{player}", player.getName()).replaceAll("\\{playerUUID}", player.getUniqueId().toString());
                try {
                    nbt = getTagFromJson.invoke(null, s);
                } catch (Exception e) {
                    player.sendMessage(e.getCause().getMessage());
                    return;
                }
//                setString.invoke(nbt, "id", getEntityName());
                if (entity != null) {
                    NmsUtils.setEntityTag(entity, (String) s);
                    entity.setRotation(loc.getYaw(), loc.getPitch());
//                    setPositionRotation.invoke(entity, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                    UUID uuid = entity.getUniqueId();
                    Entity e = Bukkit.getEntity(uuid);
                    if (e != null) {
                        if (e instanceof Projectile) {
                            ((Projectile) e).setShooter(player);
                        }
                        e.setVelocity(loc.getDirection().multiply(power.getSpeed()));
                        e.setPersistent(power.isPersistent());
                    }
                }
            } catch (NoSuchMethodException e) {
                RPGItems.plugin.getLogger().log(Level.WARNING, "Execption spawning entity in " + power.getItem().getName(), e);
            }
        }

        @Override
        public PowerResult<Void> hurt(Throw power, Player target, ItemStack stack, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> rightClick(Throw power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> leftClick(Throw power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Throw power, Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(power, player, itemStack).with(e.getForce());
        }
    }
}
package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.utils.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;

import static think.rpgitems.power.Utils.checkCooldownByString;

/**
 * Power throw.
 * <p>
 *  Spawn and throw an entity
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class)
public class PowerThrow extends BasePower implements PowerRightClick, PowerLeftClick, PowerPlain {
    @Property(order = 5, required = true)
    public String entityData = "";
    @Property(order = 4)
    public String entityName = "";
    @Property(order = 1)
    public long cooldown = 20;
    @Property(order = 3)
    public double speed = 3;
    @Property(order = 0)
    public String display = "throw entity";
    @Property(order = 6)
    public boolean isPersistent;
    @Property
    public int cost = 0;

    @Override
    public void init(ConfigurationSection section) {
        boolean isRight = section.getBoolean("isRight", true);
        triggers = Collections.singleton(isRight ? Trigger.RIGHT_CLICK : Trigger.LEFT_CLICK);
        super.init(section);
    }

    @Override
    public String getName() {
        return "throw";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + display;
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (checkCooldownByString(player, getItem(), entityName + entityData, cooldown, true) && getItem().consumeDurability(stack, cost)) {
            summonEntity(player, stack);
            return PowerResult.ok();
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @SuppressWarnings("deprecation")
    private void summonEntity(Player player, ItemStack stack) {
        try {
            Location loc = player.getEyeLocation().clone();
            Class craftWorld = ReflectionUtils.getOBCClass("CraftWorld");
            Method getHandleMethod = ReflectionUtils.getMethod(craftWorld, "getHandle");
            Object worldServer = getHandleMethod.invoke(loc.getWorld());
            Class<?> chunkRegionLoader = ReflectionUtils.getNMSClass("ChunkRegionLoader");
            Class<?> mojangsonParser = ReflectionUtils.getNMSClass("MojangsonParser");
            Method getTagFromJson = mojangsonParser.getMethod("parse", String.class);
            Class<?> nbtTagCompound = ReflectionUtils.getNMSClass("NBTTagCompound");
            Method setString = nbtTagCompound.getMethod("setString", String.class, String.class);
            Class<?> nmsEntity = ReflectionUtils.getNMSClass("Entity");
            Method getUUID = nmsEntity.getMethod("getUniqueID");
            Method setPositionRotation = nmsEntity.getMethod("setPositionRotation", double.class, double.class, double.class, float.class, float.class);
            Method spawnEntity = chunkRegionLoader.getMethod("a", nbtTagCompound, ReflectionUtils.getNMSClass("World"), double.class, double.class, double.class, boolean.class);
            Object nbt;
            try {
                nbt = getTagFromJson.invoke(null, entityData.replaceAll("\\{player}", player.getName()).replaceAll("\\{playerUUID}", player.getUniqueId().toString()));
            } catch (Exception e) {
                player.sendMessage(e.getCause().getMessage());
                return;
            }
            setString.invoke(nbt, "id", entityName);
            Object entity = spawnEntity.invoke(null, nbt, worldServer, loc.getX(), loc.getY(), loc.getZ(), true);
            if (entity != null) {
                setPositionRotation.invoke(entity, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                UUID uuid = (UUID) getUUID.invoke(entity);
                Entity e = Bukkit.getEntity(uuid);
                if (e != null) {
                    if (e instanceof Projectile) {
                        ((Projectile) e).setShooter(player);
                    }
                    e.setVelocity(loc.getDirection().multiply(speed));
                    e.setPersistent(isPersistent);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
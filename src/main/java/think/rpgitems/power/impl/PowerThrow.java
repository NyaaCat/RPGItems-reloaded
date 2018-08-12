package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.utils.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerLeftClick;
import think.rpgitems.power.PowerRightClick;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static think.rpgitems.utils.PowerUtils.checkCooldownByString;

public class PowerThrow extends BasePower implements PowerRightClick, PowerLeftClick {
    @Property(order = 5)
    public String entityData = "";
    @Property(order = 4)
    public String entityName = "";
    @Property(order = 1)
    public long cooldown = 20;
    @Property(order = 3)
    public double speed = 3;
    @Property(order = 0)
    public String display = "throw entity";
    @Property(order = 2)
    public boolean isRight;
    @Property(order = 6)
    public boolean isPersistent;
    public int consumption = 0;

    @Override
    public String getName() {
        return "throw";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + display;
    }

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (isRight && checkCooldownByString(player, getItem(), entityName + entityData, cooldown, true) && getItem().consumeDurability(stack, consumption)) {
            summonEntity(player, stack, clicked);
        }
    }

    @Override
    public void leftClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!isRight && checkCooldownByString(player, getItem(), entityName + entityData, cooldown, true) && getItem().consumeDurability(stack, consumption)) {
            summonEntity(player, stack, clicked);
        }
    }

    @SuppressWarnings("deprecation")
    private void summonEntity(Player player, ItemStack stack, Block block) {
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
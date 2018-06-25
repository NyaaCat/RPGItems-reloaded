package think.rpgitems.power;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.types.PowerLeftClick;
import think.rpgitems.power.types.PowerRightClick;
import think.rpgitems.utils.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class PowerThrow extends Power implements PowerRightClick, PowerLeftClick {
    public String entityData = "";
    public String entityName = "";
    public long cooldownTime = 20;
    public double speed = 3;
    public String display = "throw entity";
    public boolean isRight;
    public int consumption = 0;

    @Override
    public void init(ConfigurationSection s) {
        entityName = s.getString("entityName");
        entityData = s.getString("entityData");
        cooldownTime = s.getLong("cooldown", 20);
        speed = s.getDouble("speed", 3);
        display = s.getString("display");
        isRight = s.getBoolean("isRight", true);
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("entityName", entityName);
        s.set("entityData", entityData);
        s.set("cooldown", cooldownTime);
        s.set("speed", speed);
        s.set("display", display);
        s.set("isRight", isRight);
        s.set("consumption", consumption);
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
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (isRight && checkCooldownByString(player, item, entityName + entityData, cooldownTime, true) && item.consumeDurability(stack, consumption)) {
            summonEntity(player, stack, clicked);
        }
    }

    @Override
    public void leftClick(Player player, ItemStack stack, Block clicked) {
        if (!isRight && checkCooldownByString(player, item, entityName + entityData, cooldownTime, true) && item.consumeDurability(stack, consumption)) {
            summonEntity(player, stack, clicked);
        }
    }

    private void summonEntity(Player player, ItemStack stack, Block block) {
        try {
            Location loc = player.getEyeLocation().clone();
            Class craftWorld = ReflectionUtil.getOBCClass("CraftWorld");
            Method getHandleMethod = ReflectionUtil.getMethod(craftWorld, "getHandle");
            Object worldServer = getHandleMethod.invoke(loc.getWorld());
            Class<?> chunkRegionLoader = ReflectionUtil.getNMSClass("ChunkRegionLoader");
            Class<?> mojangsonParser = ReflectionUtil.getNMSClass("MojangsonParser");
            Method getTagFromJson = mojangsonParser.getMethod("parse", String.class);
            Class<?> nbtTagCompound = ReflectionUtil.getNMSClass("NBTTagCompound");
            Method setString = nbtTagCompound.getMethod("setString", String.class, String.class);
            Class<?> nmsEntity = ReflectionUtil.getNMSClass("Entity");
            Method getUUID = nmsEntity.getMethod("getUniqueID");
            Method setPositionRotation = nmsEntity.getMethod("setPositionRotation", double.class, double.class, double.class, float.class, float.class);
            Method spawnEntity = chunkRegionLoader.getMethod("a", nbtTagCompound, ReflectionUtil.getNMSClass("World"), double.class, double.class, double.class, boolean.class);
            Object nbt;
            try {
                nbt = getTagFromJson.invoke(null, entityData.replaceAll("\\{player}", player.getName()).replaceAll("\\{playerUUID}", player.getUniqueId().toString()));
            } catch (Exception e) {
                TranslatableComponent msg = new TranslatableComponent("commands.summon.tagError");
                msg.addWith(new TextComponent(e.getCause().getMessage()));
                player.spigot().sendMessage(msg);
                return;
            }
            setString.invoke(nbt, "id", entityName);
            Object entity = spawnEntity.invoke(null, nbt, worldServer, loc.getX(), loc.getY(), loc.getZ(), true);
            if (entity != null) {
                setPositionRotation.invoke(entity, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                UUID uuid = (UUID) getUUID.invoke(entity);
                for (Entity e: loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
                    if (e.getUniqueId().equals(uuid)) {
                        if (e instanceof Projectile) {
                            ((Projectile) e).setShooter(player);
                        }
                        e.setVelocity(loc.getDirection().multiply(speed));
                        return;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
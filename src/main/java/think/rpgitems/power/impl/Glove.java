package think.rpgitems.power.impl;

import static think.rpgitems.power.Utils.*;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

@Meta(immutableTrigger = true, withSelectors = true, implClass = Glove.Impl.class)
public class Glove extends BasePower {
    @Property(order = 1)
    public int maxDistance = 5;

    @Property(order = 2)
    public int maxTicks = 200;

    @Property(order = 3)
    public double throwSpeed = 0.0D;

    public int getMaxDistance() {
        return maxDistance;
    }

    public int getMaxTicks() {
        return maxTicks;
    }

    @Override
    public String getName() {
        return "glove";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.glove", (0) / 20D);
    }

    public double getThrowSpeed() {
        return throwSpeed;
    }

    public static class Impl implements PowerRightClick<Glove> {
        @Override
        public PowerResult<Void> rightClick(
                Glove power, Player player, ItemStack stack, PlayerInteractEvent event) {
            if (!player.getPassengers().isEmpty()) {
                Entity entity = player.getPassengers().get(0);
                entity.leaveVehicle();
                if (power.getThrowSpeed() > 0.0D) {
                    player.getWorld()
                            .playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1.0F, 1.0F);
                    entity.setVelocity(
                            player.getLocation().getDirection().multiply(power.getThrowSpeed()));
                }
                return PowerResult.ok();
            }

            List<LivingEntity> entities =
                    getLivingEntitiesInCone(
                            getNearestLivingEntities(
                                    power,
                                    player.getEyeLocation(),
                                    player,
                                    power.getMaxDistance(),
                                    0),
                            player.getLocation().toVector(),
                            30,
                            player.getLocation().getDirection());
            for (LivingEntity entity : entities) {
                if (entity.isValid()
                        && entity.getType() != EntityType.ARMOR_STAND
                        && !entity.isInsideVehicle()
                        && entity.getPassengers().isEmpty()
                        && player.hasLineOfSight(entity)
                        && player.addPassenger(entity)) {
                    player.getWorld()
                            .playSound(
                                    player.getLocation(),
                                    Sound.BLOCK_DISPENSER_DISPENSE,
                                    1.0F,
                                    1.0F);
                    Listener listener = null;
                    if (entity instanceof Player) {
                        listener =
                                new Listener() {
                                    @EventHandler
                                    public void onPlayerQuit(PlayerQuitEvent e) {
                                        if (e.getPlayer()
                                                .getUniqueId()
                                                .equals(entity.getUniqueId())) {
                                            player.removePassenger(entity);
                                            entity.leaveVehicle();
                                        }
                                    }
                                };
                        Bukkit.getPluginManager().registerEvents(listener, RPGItems.plugin);
                    }
                    Listener finalListener = listener;
                    new BukkitRunnable() {
                        private long ticks = 0L;

                        @Override
                        public void run() {
                            if (getTicks() >= power.getMaxTicks()
                                    || player.getPassengers().isEmpty()
                                    || entity.isDead()) {
                                cancel();
                                if (finalListener != null) {
                                    HandlerList.unregisterAll(finalListener);
                                }
                                if (!player.getPassengers().isEmpty()
                                        && player.getPassengers()
                                                .get(0)
                                                .getUniqueId()
                                                .equals(entity.getUniqueId())) {
                                    player.getPassengers().get(0).leaveVehicle();
                                }
                            }
                            setTicks(getTicks() + 1);
                        }

                        public long getTicks() {
                            return ticks;
                        }

                        public void setTicks(long ticks) {
                            this.ticks = ticks;
                        }
                    }.runTaskTimer(RPGItems.plugin, 1, 1);
                    return PowerResult.ok();
                }
            }

            return PowerResult.fail();
        }

        @Override
        public Class<? extends Glove> getPowerClass() {
            return Glove.class;
        }
    }
}

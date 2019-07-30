package think.rpgitems.power.impl;

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

import java.util.List;

import static think.rpgitems.power.Utils.*;

@PowerMeta(immutableTrigger = true, withSelectors = true, implClass = PowerGlove.Impl.class)
public class PowerGlove extends BasePower {
    @Property(order = 0)
    private long cooldown = 20L;
    @Property(order = 1)
    private int maxDistance = 5;
    @Property(order = 2)
    private int maxTicks = 200;
    @Property(order = 3)
    private double throwSpeed = 0.0D;

    public long getCooldown() {
        return cooldown;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public int getMaxTicks() {
        return maxTicks;
    }

    public double getThrowSpeed() {
        return throwSpeed;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    public void setMaxTicks(int maxTicks) {
        this.maxTicks = maxTicks;
    }

    public void setThrowSpeed(double throwSpeed) {
        this.throwSpeed = throwSpeed;
    }

    public class Impl implements PowerRightClick {
        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            if (!player.getPassengers().isEmpty()) {
                Entity entity = player.getPassengers().get(0);
                entity.leaveVehicle();
                if (getThrowSpeed() > 0.0D) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1.0F, 1.0F);
                    entity.setVelocity(player.getLocation().getDirection().multiply(getThrowSpeed()));
                }
                return PowerResult.ok();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();

            List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(getPower(), player.getEyeLocation(), player, getMaxDistance(), 0), player.getLocation().toVector(), 30, player.getLocation().getDirection());
            for (LivingEntity entity : entities) {
                if (entity.isValid() && entity.getType() != EntityType.ARMOR_STAND && !entity.isInsideVehicle() &&
                            entity.getPassengers().isEmpty() && player.hasLineOfSight(entity) && player.addPassenger(entity)) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0F, 1.0F);
                    Listener listener = null;
                    if (entity instanceof Player) {
                        listener = new Listener() {
                            @EventHandler
                            public void onPlayerQuit(PlayerQuitEvent e) {
                                if (e.getPlayer().getUniqueId().equals(entity.getUniqueId())) {
                                    player.removePassenger(entity);
                                    entity.leaveVehicle();
                                }
                            }
                        };
                        Bukkit.getPluginManager().registerEvents(listener, RPGItems.plugin);
                    }
                    Listener finalListener = listener;
                    new BukkitRunnable() {
                        public void setTicks(long ticks) {
                            this.ticks = ticks;
                        }

                        public long getTicks() {
                            return ticks;
                        }

                        private long ticks = 0L;

                        @Override
                        public void run() {
                            if (getTicks() >= getMaxTicks() || player.getPassengers().isEmpty() || entity.isDead()) {
                                cancel();
                                if (finalListener != null) {
                                    HandlerList.unregisterAll(finalListener);
                                }
                                if (!player.getPassengers().isEmpty() && player.getPassengers().get(0).getUniqueId().equals(entity.getUniqueId())) {
                                    player.getPassengers().get(0).leaveVehicle();
                                }
                            }
                            setTicks(getTicks() + 1);
                        }
                    }.runTaskTimer(RPGItems.plugin, 1, 1);
                    return PowerResult.ok();
                }
            }

            return PowerResult.fail();
        }

        @Override
        public Power getPower() {
            return PowerGlove.this;
        }
    }

    @Override
    public String getName() {
        return "glove";
    }

    @Override
    public String displayText() {
        return I18n.format("power.glove", (double) getCooldown() / 20D);
    }
}

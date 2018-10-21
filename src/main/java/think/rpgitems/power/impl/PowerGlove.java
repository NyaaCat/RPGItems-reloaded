package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;
import think.rpgitems.power.Property;

import java.util.List;

import static think.rpgitems.power.Utils.*;

@PowerMeta(immutableTrigger = true, withSelectors = true)
public class PowerGlove extends BasePower implements PowerRightClick {
    @Property(order = 0)
    public long cooldown = 20L;
    @Property(order = 1)
    public int maxDistance = 5;
    @Property(order = 2)
    public int maxTicks = 200;
    @Property(order = 3)
    public double throwSpeed = 0.0D;

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        if (!player.getPassengers().isEmpty()) {
            Entity entity = player.getPassengers().get(0);
            entity.leaveVehicle();
            if (throwSpeed > 0.0D) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1.0F, 1.0F);
                entity.setVelocity(player.getLocation().getDirection().multiply(throwSpeed));
            }
            return PowerResult.ok();
        }
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();

        List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(this, player.getEyeLocation(), player, maxDistance, 0), player.getLocation().toVector(), 30, player.getLocation().getDirection());
        for (LivingEntity entity : entities) {
            if (entity.isValid() && entity.getType() != EntityType.ARMOR_STAND && !entity.isInsideVehicle() &&
                    entity.getPassengers().isEmpty() && player.hasLineOfSight(entity) && player.addPassenger(entity)) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0F, 1.0F);
                new BukkitRunnable() {
                    private long ticks = 0L;

                    @Override
                    public void run() {
                        if (ticks >= maxTicks || player.getPassengers().isEmpty() || entity.isDead()) {
                            cancel();
                            if (!player.getPassengers().isEmpty() && player.getPassengers().get(0).getUniqueId().equals(entity.getUniqueId())) {
                                player.getPassengers().get(0).leaveVehicle();
                            }
                        }
                        ticks++;
                    }
                }.runTaskTimer(RPGItems.plugin, 1, 1);
                return PowerResult.ok();
            }
        }

        return PowerResult.fail();
    }

    @Override
    public String getName() {
        return "glove";
    }

    @Override
    public String displayText() {
        return I18n.format("power.glove", (double) cooldown / 20D);
    }
}

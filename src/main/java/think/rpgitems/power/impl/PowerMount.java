package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.PowerMeta;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;

import java.util.List;

import static think.rpgitems.utils.PowerUtils.*;

@PowerMeta(immutableTrigger = true)
public class PowerMount extends BasePower implements PowerRightClick {
    @Property(order = 0)
    public long cooldown = 20L;
    @Property(order = 1)
    public int maxDistance = 5;
    @Property(order = 2)
    public int maxTicks = 200;

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (player.isInsideVehicle()) {
            return PowerResult.fail();
        }
        List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(this, player.getEyeLocation(), player, maxDistance, 0), player.getLocation().toVector(), 30, player.getLocation().getDirection());
        for (LivingEntity entity : entities) {
            if (entity.isValid() && entity.getType() != EntityType.ARMOR_STAND && !entity.isInsideVehicle() &&
                    entity.getPassengers().isEmpty() && player.hasLineOfSight(entity) && entity.addPassenger(player)) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 1.0F, 1.0F);
                new BukkitRunnable() {
                    private long ticks = 0L;

                    @Override
                    public void run() {
                        if (ticks >= maxTicks || entity.isDead() || entity.getPassengers().isEmpty() || player.isDead()) {
                            cancel();
                            if (!entity.isDead() && !entity.getPassengers().isEmpty() && entity.getPassengers().get(0).getUniqueId().equals(player.getUniqueId())) {
                                entity.getPassengers().get(0).leaveVehicle();
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
        return "mount";
    }

    @Override
    public String displayText() {
        return I18n.format("power.mount", (double) cooldown / 20D);
    }
}

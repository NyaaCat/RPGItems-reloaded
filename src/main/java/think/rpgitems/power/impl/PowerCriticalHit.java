package think.rpgitems.power.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.PowerHit;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

import java.util.concurrent.ThreadLocalRandom;

import static think.rpgitems.power.Utils.getAngleBetweenVectors;

@PowerMeta(immutableTrigger = true)
public class PowerCriticalHit extends BasePower implements PowerHit {

    @Property
    public double chance = 20;

    @Property
    public double backstabChance = 20;

    @Property
    public double factor = 1.5;

    @Property
    public double backstabFactor = 1.5;

    @Property
    public boolean setBaseDamage = false;

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (ThreadLocalRandom.current().nextDouble(100) < chance) {
            damage *= factor;
        }
        if (getAngleBetweenVectors(((LivingEntity) event.getEntity()).getEyeLocation().getDirection(), player.getEyeLocation().getDirection()) < 90 && ThreadLocalRandom.current().nextDouble(100) < backstabChance) {
            damage *= backstabFactor;
        }
        if (setBaseDamage) {
            event.setDamage(damage);
        }
        return PowerResult.ok(damage);
    }

    @Override
    public String getName() {
        return "criticalhit";
    }

    @Override
    public String displayText() {
        return (backstabChance != 0 && chance != 0) ?
                       I18n.format("power.criticalhit.both", chance, factor, backstabChance, backstabFactor)
                       : (chance != 0) ?
                                 I18n.format("power.criticalhit.critical", chance, factor) :
                                 I18n.format("power.criticalhit.backstab", backstabChance, backstabFactor);
    }
}

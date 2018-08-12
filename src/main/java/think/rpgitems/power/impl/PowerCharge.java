package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerHit;

public class PowerCharge extends BasePower implements PowerHit {

    @Property
    public int percentage = 30;

    @Property
    public int speedPercentage = 20;

    @Property
    public double cap = 300;

    @Override
    public String getName() {
        return "charge";
    }

    @Override
    public String displayText() {
        return I18n.format("power.charge", percentage);
    }

    @Override
    public void hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (!player.isSprinting())
            return;
        double originDamage = damage;
        damage = damage * (1 + percentage / 100.0);
        damage = damage + damage * (player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() / 0.13 - 1) * (speedPercentage / 100.0);
        damage = Math.max(Math.min(damage, cap), originDamage);
        event.setDamage(damage);
        if (damage > originDamage) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.1f, 0.1f);
        }
    }
}

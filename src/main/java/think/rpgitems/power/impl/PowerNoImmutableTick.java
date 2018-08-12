package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.PowerHit;

public class PowerNoImmutableTick extends BasePower implements PowerHit {

    @Override
    public String getName() {
        return "noimmutabletick";
    }

    @Override
    public String displayText() {
        return I18n.format("power.noimmutabletick");
    }

    @Override
    public void hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        Bukkit.getScheduler().runTaskLater(RPGItems.plugin, ()-> entity.setNoDamageTicks(0), 0);
        Bukkit.getScheduler().runTaskLater(RPGItems.plugin, ()-> entity.setNoDamageTicks(0), 1);
    }
}

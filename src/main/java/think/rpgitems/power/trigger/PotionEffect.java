package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerExpChange;
import think.rpgitems.power.PowerPotionEffect;
import think.rpgitems.power.PowerResult;

class PotionEffect extends Trigger<EntityPotionEffectEvent, PowerPotionEffect, Void, Void> {
    PotionEffect() {
        super(EntityPotionEffectEvent.class, PowerPotionEffect.class, Void.class, Void.class, "POTION_EFFECT");
    }

    public PotionEffect(String name) {
        super(name, "POTION_EFFECT", EntityPotionEffectEvent.class, PowerPotionEffect.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerPotionEffect power, Player player, ItemStack i, EntityPotionEffectEvent event) {
        return power.potionEffect(player, i, event);
    }
}
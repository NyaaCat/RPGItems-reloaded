package think.rpgitems.power.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

@Meta(defaultTrigger = "TICK", implClass = Undead.Impl.class)
public class Undead extends BasePower {
    @Property
    public boolean allowOffHand = false;
    @Override
    public String getName() {
        return "undead";
    }
    @Override
    public String displayText() {
        return I18n.formatDefault("power.undead");
    }
    public boolean getAllowOffHand(){
        return allowOffHand;
    }
    public class Impl implements PowerTick, PowerHit{
        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            byte internalLight = player.getWorld().getBlockAt(player.getLocation()).getLightLevel();
            byte skyLight = player.getWorld().getBlockAt(player.getLocation()).getLightFromSky();
            if(player.getRemainingAir()!=player.getMaximumAir()){
                player.setRemainingAir(player.getMaximumAir());
            }
            if(player.getFireTicks()<=140&& player.getInventory().getHelmet() instanceof Damageable &&!player.isInWaterOrRainOrBubbleColumn()&&!player.isInPowderedSnow()&&player.getWorld().getEnvironment()==World.Environment.NORMAL&&internalLight>=12&&skyLight==15){
                player.setFireTicks(160);
                return PowerResult.ok();
            }
            return PowerResult.noop();
        }
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            if(entity instanceof Wither){
                event.setCancelled(true);
                return PowerResult.ok(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public Power getPower() {
            return Undead.this;
        }
    }
}

package think.rpgitems.power.impl;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

@Meta(defaultTrigger = {"MENDING", "EXP_CHANGE"}, implClass = Mending.Impl.class)
public class Mending extends BasePower {
    @Property
    public double repairFactor = 2;
    @Property
    public boolean requireEnchantment = false;
    @Override
    public String getName() {
        return "mending";
    }
    @Override
    public String displayText() {
        return I18n.formatDefault("power.mending", getRepairFactor());
    }
    public boolean isRequireEnchantment() {
        return requireEnchantment;
    }
    public double getRepairFactor() {
        return repairFactor;
    }
    public class Impl implements PowerMending, PowerExpChange{

        @Override
        public Power getPower() {
            return Mending.this;
        }

        @Override
        public PowerResult<Void> expChange(Player player, ItemStack stack, PlayerExpChangeEvent event) {
            RPGItem item = getItem();
            if((!stack.containsEnchantment(Enchantment.MENDING)&&isRequireEnchantment())|| item.getItemStackDurability(stack).isEmpty() ||(item.getItemStackDurability(stack).get()==item.getMaxDurability())){
                return PowerResult.noop();
            }
            else{
                int expAmount = event.getAmount();
                if(item.getItemStackDurability(stack).get()+expAmount*getRepairFactor()<=item.getMaxDurability()){
                    event.setAmount(0);
                    item.setItemStackDurability(stack,(int)(item.getItemStackDurability(stack).get()+expAmount*getRepairFactor()));
                }else{
                    event.setAmount(expAmount-(int)Math.ceil((item.getMaxDurability()-item.getItemStackDurability(stack).get())/getRepairFactor()));
                    item.setItemStackDurability(stack,item.getMaxDurability());
                }
                return PowerResult.ok();
            }
        }

        @Override
        public PowerResult<Void> mending(Player player, ItemStack stack, PlayerItemMendEvent event) {
            RPGItem item = getItem();
            if((!stack.containsEnchantment(Enchantment.MENDING)&&isRequireEnchantment())|| item.getItemStackDurability(stack).isEmpty() ||(item.getItemStackDurability(stack).get()==item.getMaxDurability())){
                return PowerResult.noop();
            }
            else{
                int expAmount = event.getConsumedExperience();
                if(item.getItemStackDurability(stack).get()+expAmount*getRepairFactor()<=item.getMaxDurability()){
                    event.setRepairAmount(0);
                    item.setItemStackDurability(stack,(int)(item.getItemStackDurability(stack).get()+expAmount*getRepairFactor()));
                }else{
                    event.setRepairAmount((expAmount-(int)Math.ceil((item.getMaxDurability()-item.getItemStackDurability(stack).get())/getRepairFactor()))*2);
                    item.setItemStackDurability(stack,item.getMaxDurability());
                }
                return PowerResult.ok();
            }
        }
    }
}

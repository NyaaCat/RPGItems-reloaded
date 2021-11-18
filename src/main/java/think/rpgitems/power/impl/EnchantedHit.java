package think.rpgitems.power.impl;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

@Meta(defaultTrigger = "HIT", implClass = EnchantedHit.Impl.class)
public class EnchantedHit extends BasePower {

    @Property
    public Mode mode = Mode.ADDITION;

    @Property
    public double amountPerLevel = 1;

    @Property
    public String display;

    @Property
    @AcceptedValue(preset = Preset.ENCHANTMENT)
    public Enchantment enchantmentType = Enchantment.ARROW_DAMAGE;

    @Property
    public boolean setBaseDamage = false;

    public double getAmountPerLevel() {
        return amountPerLevel;
    }

    public Enchantment getEnchantmentType() {
        return enchantmentType;
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public String getName() {
        return "enchantedhit";
    }

    @Override
    public String displayText() {
        return getDisplay();
    }

    public String getDisplay() {
        return display;
    }

    public boolean isSetBaseDamage() {
        return setBaseDamage;
    }

    private enum Mode {
        ADDITION,
        MULTIPLICATION,
    }

    public class Impl implements PowerHit {
        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            int enchLevel = stack.getEnchantmentLevel(getEnchantmentType());
            if (getMode() == Mode.ADDITION) {
                damage += (enchLevel * getAmountPerLevel());
            }
            if (getMode() == Mode.MULTIPLICATION) {
                damage *= Math.pow(getAmountPerLevel(), enchLevel);
            }
            if (damage < 0) damage = 0;
            if (isSetBaseDamage()) {
                event.setDamage(damage);
            }
            return PowerResult.ok(damage);
        }

        @Override
        public Power getPower() {
            return EnchantedHit.this;
        }
    }
}

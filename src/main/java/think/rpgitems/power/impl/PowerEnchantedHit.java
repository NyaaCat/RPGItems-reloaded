package think.rpgitems.power.impl;

import com.meowj.langutils.lang.convert.EnumEnchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

@PowerMeta(immutableTrigger = true, implClass = PowerEnchantedHit.Impl.class)
public class PowerEnchantedHit extends BasePower {

    @Property
    private Mode mode = Mode.ADDITION;

    @Property
    private double amountPerLevel = 1;

    @Property
    private String display;

    @Property
    private EnumEnchantment enchantmentType = EnumEnchantment.ARROW_DAMAGE;

    @Property
    private boolean setBaseDamage = false;

    public class Impl implements PowerHit {
        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            int enchLevel = stack.getEnchantmentLevel(getEnchantmentType().getEnchantment());
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
            return PowerEnchantedHit.this;
        }
    }

    public double getAmountPerLevel() {
        return amountPerLevel;
    }

    public String getDisplay() {
        return display;
    }

    public EnumEnchantment getEnchantmentType() {
        return enchantmentType;
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isSetBaseDamage() {
        return setBaseDamage;
    }

    public void setAmountPerLevel(double amountPerLevel) {
        this.amountPerLevel = amountPerLevel;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public void setEnchantmentType(EnumEnchantment enchantmentType) {
        this.enchantmentType = enchantmentType;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setSetBaseDamage(boolean setBaseDamage) {
        this.setBaseDamage = setBaseDamage;
    }

    private enum Mode {
        ADDITION,
        MULTIPLICATION,
        ;
    }

    @Override
    public String getName() {
        return "enchantedhit";
    }

    @Override
    public String displayText() {
        return getDisplay();
    }

}

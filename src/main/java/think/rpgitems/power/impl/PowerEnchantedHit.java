package think.rpgitems.power.impl;

import com.meowj.langutils.lang.convert.EnumEnchantment;
import com.meowj.langutils.lang.convert.EnumEnchantmentLevel;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
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
public class PowerEnchantedHit extends BasePower implements PowerHit {

    @Property
    public Mode mode = Mode.ADDITION;

    @Property
    public double amountPerLevel = 1;

    @Property
    public String display;

    @Property
    public EnumEnchantment enchantmentType = EnumEnchantment.ARROW_DAMAGE;

    @Property
    public boolean setBaseDamage = false;

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        int enchLevel = stack.getEnchantmentLevel(enchantmentType.getEnchantment());
        if (mode == Mode.ADDITION) {
            damage += (enchLevel * amountPerLevel);
        }
        if (mode == Mode.MULTIPLICATION) {
            damage *= Math.pow(amountPerLevel, enchLevel);
        }
        if (damage < 0 ) damage = 0;
        if (setBaseDamage) {
            event.setDamage(damage);
        }
        return PowerResult.ok(damage);
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
        return display;
    }

}

package think.rpgitems.power.impl;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power potionself.
 * <p>
 * On right click it will apply {@link #type effect}
 * for {@link #duration} ticks at power {@link #amplifier}.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true)
public class PowerPotionSelf extends BasePower implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Amplifier of potion effect
     */
    @Property(order = 2)
    public int amplifier = 1;
    /**
     * Time of potion effect, in ticks
     */
    @Property(order = 1)
    public int duration = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;
    /**
     * Type of potion effect
     */
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 3, required = true)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType type = PotionEffectType.HEAL;

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        player.addPotionEffect(new PotionEffect(type, duration, amplifier), true);
        return PowerResult.ok();
    }

    @Override
    public String getName() {
        return "potionself";
    }

    @Override
    public String displayText() {
        return I18n.format("power.potionself", type.getName().toLowerCase().replaceAll("_", " "), amplifier + 1, ((double) duration) / 20d);
    }
}

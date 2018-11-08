package think.rpgitems.power.impl;

import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import static think.rpgitems.power.Utils.checkCooldown;
import static think.rpgitems.power.Utils.getNearbyEntities;

/**
 * Power aoe.
 * <p>
 * On right click the aoe power will apply {@link #type effect}
 * to all entities within the {@link #range range} for {@link #duration duration} ticks
 * at power {@link #amplifier amplifier}.
 * By default, the user will be targeted by the potion
 * as well if not set via {@link #selfapplication selfapplication}.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class)
public class PowerAOE extends BasePower implements PowerRightClick, PowerLeftClick, PowerPlain {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Amplifier of the potion
     */
    @Property(order = 4, required = true)
    public int amplifier = 1;
    /**
     * Duration of the potion
     */
    @Property(order = 3)
    public int duration = 15;
    /**
     * Range of the potion
     */
    @Property(order = 1)
    public int range = 5;
    /**
     * Whether the potion will be apply to the user
     */
    @Property(order = 5)
    public boolean selfapplication = true;
    /**
     * Type of the potion
     */
    @Property(order = 2)
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType type;
    /**
     * Display text of this power. Will use default text in case of null
     */
    @Property
    public String name = null;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Override
    public PowerResult<Void> rightClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> leftClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        PotionEffect effect = new PotionEffect(type, duration, amplifier - 1);
        if (selfapplication) {
            player.addPotionEffect(effect);
        }
        player.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, type.getColor().asRGB());
        for (Entity ent : getNearbyEntities(this, player.getLocation(), player, range)) {
            if (ent instanceof LivingEntity && !player.equals(ent)) {
                ((LivingEntity) ent).addPotionEffect(effect);
            }
        }
        return PowerResult.ok();
    }

    @Override
    public String getName() {
        return "aoe";
    }

    @Override
    public String displayText() {
        return name != null ? name : I18n.format("power.aoe.display", type.getName(), amplifier, duration, selfapplication ? I18n.format("power.aoe.selfapplication.including") : I18n.format("power.aoe.selfapplication.excluding"), range, (double) cooldown / 20d);
    }
}

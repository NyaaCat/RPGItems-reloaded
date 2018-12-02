package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power tippedarrow.
 * <p>
 * The arrow power will fire an tipped arrow on right click
 * with {@link #type effect} for {@link #duration} ticks at power {@link #amplifier}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true)
public class PowerTippedArrow extends BasePower implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Amplifier of potion effect
     */
    @Property(order = 3, required = true)
    public int amplifier = 1;
    /**
     * Duration of potion effect, in ticks
     */
    @Property(order = 2)
    public int duration = 15;
    /**
     * Type of potion effect
     */
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    @Property(order = 1)
    public PotionEffectType type = PotionEffectType.POISON;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        TippedArrow arrow = player.launchProjectile(TippedArrow.class);
        Events.registerProjectile(arrow.getEntityId(), getItem().getUID());
        arrow.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
        Events.autoRemoveProjectile(arrow.getEntityId());
        return PowerResult.ok();
    }

    @Override
    public String displayText() {
        return I18n.format("power.tippedarrow", type.getName().toLowerCase().replaceAll("_", " "), amplifier + 1, ((double) duration) / 20d, (double) cooldown / 20d);
    }

    @Override
    public String getName() {
        return "tippedarrow";
    }
}

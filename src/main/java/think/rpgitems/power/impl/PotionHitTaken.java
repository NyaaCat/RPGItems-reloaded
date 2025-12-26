package think.rpgitems.power.impl;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import java.util.*;

/**
 * Power potionhittaken.
 * <p>
 * On hit taken it will apply {@link #type effect} for {@link #duration} ticks at power {@link #amplifier} with a chance of hitting of 1/{@link #chance}.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "HIT_TAKEN", generalInterface = PowerLivingEntity.class, implClass = PotionHitTaken.Impl.class)
public class PotionHitTaken extends BasePower implements PowerPotion {

    @Property
    public int chance = 20;
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 3, required = true)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType type = PotionEffectType.INSTANT_DAMAGE;
    @Property(order = 1)
    public int duration = 20;
    @Property(order = 2)
    public int amplifier = 1;
    @Property
    public int cost = 0;
    @Property
    public boolean summingUp = false;
    @Property
    public boolean effectDamager = false;

    private final Random rand = new Random();

    /**
     * Amplifier of potion effect
     */
    public int getAmplifier() {
        return amplifier;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    public boolean isEffectDamager() {
        return effectDamager;
    }

    /**
     * Duration of potion effect
     */
    public int getDuration() {
        return duration;
    }

    public boolean isSummingUp() {
        return summingUp;
    }

    @Override
    public String getName() {
        return "potionhittaken";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault(effectDamager ? "power.potionhittaken.damager" : "power.potionhittaken.victim", (int) ((1d / (double) getChance()) * 100d), "<lang:effect.minecraft."+getType().key().value()+">", (getAmplifier()+1), (float)(getDuration()/20));
    }

    /**
     * Chance of triggering this power
     */
    public int getChance() {
        return chance;
    }

    /**
     * Type of potion effect
     */
    public PotionEffectType getType() {
        return type;
    }

    public Random getRand() {
        return rand;
    }

    public class Impl implements PowerHitTaken {

        @Override
        public Power getPower() {
            return PotionHitTaken.this;
        }

        @Override
        public PowerResult<Double> takeHit(Player player, ItemStack stack, double damage, EntityDamageEvent event) {
            if (getRand().nextInt(getChance()) != 0) {
                return PowerResult.noop();
            }
            Entity damager = event.getDamageSource().getCausingEntity();
            final int[] summing = {0};
            List<ItemStack> items = new ArrayList<>(Arrays.asList(player.getInventory().getArmorContents()));
            items.add(player.getInventory().getItemInMainHand());
            for(ItemStack i : items){
                ItemManager.toRPGItemByMeta(i).ifPresent(rpgItem -> {
                    for (Power power : rpgItem.getPowers()){
                        if(power instanceof PotionHitTaken potionHitTaken) {
                            if(potionHitTaken.getType()==getType()&&potionHitTaken.isSummingUp()){
                                summing[0] += potionHitTaken.getAmplifier();
                            }
                        }
                    }
                });
            }
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("damager", damager);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if(effectDamager) {
                if(damager instanceof LivingEntity livingEntity){
                    livingEntity.addPotionEffect(new PotionEffect(getType(), getDuration(), getAmplifier()+summing[0], isAmbient(), isShowParticles(), isShowIcon()));
                }
            }else{
                player.addPotionEffect(new PotionEffect(getType(), getDuration(), getAmplifier()+summing[0], isAmbient(), isShowParticles(), isShowIcon()));
            }
            return PowerResult.ok(damage);
        }
    }
}

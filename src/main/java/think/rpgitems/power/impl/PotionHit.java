package think.rpgitems.power.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.*;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import org.bukkit.Location;
import think.rpgitems.utils.PotionEffectUtils;

import java.util.*;

/**
 * Power potionhit.
 * <p>
 * On hit it will apply {@link #type effect} for {@link #duration} ticks at power {@link #amplifier} with a chance of hitting of 1/{@link #chance}.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "HIT", generalInterface = PowerLivingEntity.class, implClass = PotionHit.Impl.class)
public class PotionHit extends BasePower {

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
        return "potionhit";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.potionhit", (int) ((1d / (double) getChance()) * 100d), "<lang:effect.minecraft."+getType().key().value()+">", (getAmplifier()+1), (float)(getDuration()/20));
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

    public class Impl implements PowerHit, PowerLivingEntity, PowerBeamHit {
        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }


        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double value) {
            if (getRand().nextInt(getChance()) != 0) {
                return PowerResult.noop();
            }
            final int[] summing = {0};
            List<ItemStack> items = new ArrayList<>(Arrays.asList(player.getInventory().getArmorContents()));
            items.add(player.getInventory().getItemInMainHand());
            for(ItemStack i : items){
                ItemManager.toRPGItemByMeta(i).ifPresent(rpgItem -> {
                    for (Power power : rpgItem.getPowers()){
                        if(power.getName().equals("potionhit")) {
                            PotionHit potionHit = (PotionHit) power;
                            if(potionHit.getType()==getType()&&potionHit.isSummingUp()){
                                summing[0] += potionHit.getAmplifier();
                            }
                        }
                    }
                });
            }
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("target",entity);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            entity.addPotionEffect(new PotionEffect(getType(), getDuration(), getAmplifier()+summing[0]));
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return PotionHit.this;
        }

        @Override
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event) {
            return PowerResult.noop();
        }
    }
}

package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.utils.RayTraceUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static think.rpgitems.power.Utils.checkCooldown;

@Meta(defaultTrigger = {"RIGHT_CLICK", "TICK"}, generalInterface = PowerPlain.class, implClass = ParticleBarrier.Impl.class)
public class ParticleBarrier extends BasePower {

    private static final Cache<UUID, Double> barriers = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();
    private static final Cache<UUID, UUID> barrierSources = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();
    private static final Cache<UUID, Pair<Long, Double>> energys = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
    private static final AtomicInteger rc = new AtomicInteger(0);
    private static Listener event;
    @Property
    public double energyPerBarrier = 40;
    @Property
    public double barrierHealth = 40;
    @Property
    public double energyDecay = 1.5;
    @Property
    public double energyPerLevel = 10;
    @Property
    public boolean projected = false;
    @Property
    public int cooldown = 0;
    @Property
    public int cost = 0;
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 1, required = true)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType effect = PotionEffectType.INCREASE_DAMAGE;

    public static AtomicInteger getRc() {
        return rc;
    }

    public static Cache<UUID, Double> getBarriers() {
        return barriers;
    }

    public static Cache<UUID, UUID> getBarrierSources() {
        return barrierSources;
    }

    public static Cache<UUID, Pair<Long, Double>> getEnergys() {
        return energys;
    }

    public static Listener getEvent() {
        return event;
    }

    @Override
    public void init(ConfigurationSection s) {
        int orc = getRc().getAndIncrement();
        super.init(s);
        if (orc == 0) {
            event = new Listener() {
                @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
                public void onEntityDamage(EntityDamageEvent entityDamageEvent) {
                    Entity entity = entityDamageEvent.getEntity();
                    UUID uuid = entity.getUniqueId();
                    Double barrierRemain = getBarriers().getIfPresent(uuid);
                    if (barrierRemain == null || barrierRemain <= 0) {
                        return;
                    }
                    double damage = entityDamageEvent.getDamage();
                    entityDamageEvent.setDamage(0);
                    barrierRemain = barrierRemain - damage;
                    getBarriers().put(uuid, barrierRemain);
                    double energyGain = Math.min(getEnergyPerBarrier(), damage * getEnergyPerBarrier() / getBarrierHealth());
                    UUID source = getBarrierSources().getIfPresent(uuid);
                    if (source == null) return;
                    Pair<Long, Double> pair = getEnergys().getIfPresent(source);
                    long currentTime = System.currentTimeMillis();
                    boolean last = pair != null;
                    long lastTime = last ? pair.getKey() : currentTime;
                    double currentEnergy = last && pair.getValue() > 0 ? pair.getValue() : 0;
                    double energy = currentEnergy - (currentTime - lastTime) * getEnergyDecay() / 1000 + energyGain;
                    getEnergys().put(source, new Pair<>(currentTime, Math.min(energy, 100.0d)));
                }
            };
            Bukkit.getPluginManager().registerEvents(getEvent(), RPGItems.plugin);
        }
    }

    public double getEnergyPerBarrier() {
        return energyPerBarrier;
    }

    public double getBarrierHealth() {
        return barrierHealth;
    }

    public double getEnergyDecay() {
        return energyDecay;
    }

    public int getCooldown() {
        return cooldown;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    /**
     * Type of potion effect
     */
    public PotionEffectType getEffect() {
        return effect;
    }

    public double getEnergyPerLevel() {
        return energyPerLevel;
    }

    @Override
    public String getName() {
        return "particlebarrier";
    }

    @Override
    public String displayText() {
        return null;
    }

    @Override
    public void deinit() {
        int nrc = getRc().decrementAndGet();
        if (nrc == 0) {
            HandlerList.unregisterAll(getEvent());
        }
    }

    public boolean isProjected() {
        return projected;
    }

    public class Impl implements PowerPlain, PowerRightClick, PowerLeftClick, PowerTick, PowerBowShoot {

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @SuppressWarnings("unchecked")
        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if (!isProjected()) {
                barrier(player, player);
                return PowerResult.ok();
            } else {
                List<LivingEntity> livingEntities = RayTraceUtils.rayTraceEntities(player, 32);
                Optional<LivingEntity> optionalPlayer = livingEntities.stream().min(Comparator.comparing(p -> p.getLocation().distanceSquared(player.getLocation())));
                if (optionalPlayer.isPresent()) {
                    barrier(player, optionalPlayer.get());
                    return PowerResult.ok();
                } else {
                    return PowerResult.fail();
                }
            }
        }

        @Override
        public Power getPower() {
            return ParticleBarrier.this;
        }

        @SuppressWarnings("deprecation")
        private void barrier(Player source, LivingEntity target) {
            getBarriers().put(target.getUniqueId(), getBarrierHealth());
            getBarrierSources().put(target.getUniqueId(), source.getUniqueId());
            Location eyeLocation = target.getEyeLocation();
            Location base = eyeLocation.clone();
            Vector f = base.getDirection().setY(0).normalize();
            base.setYaw(base.getYaw() + 45);
            Vector r = base.getDirection().setY(0).normalize();
            base.setYaw(base.getYaw() - 90);
            Vector l = base.getDirection().setY(0).normalize();

            ArmorStand asL = makeAs(eyeLocation.clone().add(l.multiply(2)));
            asL.setLeftArmPose(new EulerAngle(90 * Math.PI / 180, 60 * Math.PI / 180, 0));

            ArmorStand asR = makeAs(eyeLocation.clone().add(r.multiply(2)));
            asR.setRightArmPose(new EulerAngle(90 * Math.PI / 180, 300 * Math.PI / 180, 0));

            ArmorStand asB = makeAs(eyeLocation.clone().subtract(f.multiply(2)));

            new BukkitRunnable() {
                private int dur = 100;

                @Override
                public void run() {
                    UUID uuid = target.getUniqueId();
                    Double barrierRemain = getBarriers().getIfPresent(uuid);
                    if (dur > 0 && barrierRemain != null && barrierRemain > 0) {
                        dur--;
                    } else {
                        asR.remove();
                        asL.remove();
                        asB.remove();
                        getBarriers().put(uuid, 0.0);
                        Location eyeLocation = target.getEyeLocation();
                        eyeLocation.getWorld().playSound(eyeLocation, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                        cancel();
                        return;
                    }
                    Location eyeLocation = target.getEyeLocation();
                    Location base = eyeLocation.clone();
                    Vector f = base.getDirection().setY(0).normalize();
                    base.setYaw(base.getYaw() + 45);
                    Vector r = base.getDirection().setY(0).normalize();
                    base.setYaw(base.getYaw() - 90);
                    Vector l = base.getDirection().setY(0).normalize();
                    asR.teleport(eyeLocation.clone().add(r.multiply(1 + dur / 100.0)));
                    asL.teleport(eyeLocation.clone().add(l.multiply(1 + dur / 100.0)));
                    asB.teleport(eyeLocation.clone().subtract(f.multiply(1 + dur / 100.0)));
                }
            }.runTaskTimer(RPGItems.plugin, 0, 1L);
        }

        @SuppressWarnings("deprecation")
        private ArmorStand makeAs(Location loc) {
            return loc.getWorld().spawn(loc, ArmorStand.class, as -> {
                as.setCanPickupItems(false);
                as.setMarker(true);
                as.setPersistent(false);
                as.setSmall(true);
                as.setCustomNameVisible(false);
                as.setGravity(false);
                as.setVisible(false);
                as.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
            });
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            Pair<Long, Double> pair = getEnergys().getIfPresent(player.getUniqueId());
            if (pair == null) return PowerResult.noop();
            long currentTimeMillis = System.currentTimeMillis();
            double energy = pair.getValue() - (currentTimeMillis - pair.getKey()) * getEnergyDecay() / 1000;
            if (energy <= 0) return PowerResult.noop();
            int level = (int) (energy / getEnergyPerLevel());
            player.addPotionEffect(new PotionEffect(getEffect(), 5, level));
            return PowerResult.ok();
        }
    }
}

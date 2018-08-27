package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.utils.RayTraceUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;
import think.rpgitems.utils.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@PowerMeta(defaultTrigger = {TriggerType.RIGHT_CLICK, TriggerType.TICK})
public class PowerParticleBarrier extends BasePower implements PowerRightClick, PowerLeftClick, PowerTick {

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

    private Consumer<EntityDamageEvent> event;

    private static final Cache<UUID, Double> barriers = CacheBuilder.newBuilder()
                                                                    .expireAfterAccess(1, TimeUnit.MINUTES)
                                                                    .build();

    private static final Cache<UUID, UUID> barrierSources = CacheBuilder.newBuilder()
                                                                        .expireAfterAccess(1, TimeUnit.MINUTES)
                                                                        .build();

    private static final Cache<UUID, Pair<Long, Double>> energys = CacheBuilder.newBuilder()
                                                                               .expireAfterAccess(10, TimeUnit.MINUTES)
                                                                               .build();

    @Override
    public void init(ConfigurationSection s) {
        super.init(s);
        event = entityDamageEvent -> {
            Entity entity = entityDamageEvent.getEntity();
            UUID uuid = entity.getUniqueId();
            Double barrierRemain = barriers.getIfPresent(uuid);
            if (barrierRemain == null || barrierRemain <= 0) {
                return;
            }
            double damage = entityDamageEvent.getDamage();
            entityDamageEvent.setDamage(0);
            barrierRemain = barrierRemain - damage;
            barriers.put(uuid, barrierRemain);
            double energyGain = damage * energyPerBarrier / barrierHealth;
            UUID source = barrierSources.getIfPresent(uuid);
            if (source == null) return;
            Pair<Long, Double> pair = energys.getIfPresent(source);
            long currentTime = System.currentTimeMillis();
            boolean last = pair != null;
            long lastTime = last ? pair.getKey() : currentTime;
            double currentEnergy = last && pair.getValue() > 0 ? pair.getValue() : 0;
            double energy = currentEnergy - (currentTime - lastTime) * energyDecay / 1000 + energyGain;
            energys.put(source, new Pair<>(currentTime, Math.max(energy, 100.0d)));
        };
        RPGItems.listener.addEventListener(EntityDamageEvent.class, event);
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
    public PowerResult<Void> leftClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        return fire(player);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        return fire(player);
    }

    @SuppressWarnings("unchecked")
    public PowerResult<Void> fire(Player player) {
        if (!projected) {
            barrier(player, player);
            return PowerResult.ok();
        } else {
            try {
                List<LivingEntity> livingEntities = RayTraceUtils.rayTraceEntites(player, 32, RayTraceUtils.isAPlayer().and(RayTraceUtils.notPlayer(player)));
                Optional<LivingEntity> optionalPlayer = livingEntities.stream().min(Comparator.comparing(p -> p.getLocation().distanceSquared(player.getLocation())));
                if (optionalPlayer.isPresent()) {
                    barrier(player, optionalPlayer.get());
                    return PowerResult.ok();
                } else {
                    return PowerResult.fail();
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void barrier(Player source, LivingEntity target) {
        barriers.put(target.getUniqueId(), barrierHealth);
        barrierSources.put(target.getUniqueId(), source.getUniqueId());
        Location eyeLocation = target.getEyeLocation();
        Location base = eyeLocation.clone();
        Vector f = base.getDirection().setY(0).normalize();
        base.setYaw(base.getYaw() + 50);
        Vector r = base.getDirection().setY(0).normalize();
        base.setYaw(base.getYaw() - 100);
        Vector l = base.getDirection().setY(0).normalize();

        ArmorStand asL = eyeLocation.getWorld().spawn(eyeLocation.clone().add(l.multiply(2)), ArmorStand.class);
        asL.setCanPickupItems(false);
        asL.setMarker(true);
        asL.setPersistent(false);
        asL.setSmall(true);
        asL.setCustomNameVisible(false);
        asL.setGravity(false);
        asL.setVisible(false);
        asL.setLeftArmPose(new EulerAngle(90 * Math.PI / 180, 60 * Math.PI / 180, 0));
        asL.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));

        ArmorStand asR = eyeLocation.getWorld().spawn(eyeLocation.clone().add(r.multiply(2)), ArmorStand.class);
        asR.setCanPickupItems(false);
        asR.setMarker(true);
        asR.setPersistent(false);
        asR.setSmall(true);
        asR.setCustomNameVisible(false);
        asR.setGravity(false);
        asR.setVisible(false);
        asR.setRightArmPose(new EulerAngle(90 * Math.PI / 180, 300 * Math.PI / 180, 0));
        asR.getEquipment().setItemInMainHand(new ItemStack(Material.SHIELD));

        ArmorStand asB = eyeLocation.getWorld().spawn(eyeLocation.clone().subtract(f.multiply(2)), ArmorStand.class);
        asB.setCanPickupItems(false);
        asB.setMarker(true);
        asB.setPersistent(false);
        asB.setSmall(true);
        asB.setCustomNameVisible(false);
        asB.setGravity(false);
        asB.setVisible(false);
        asB.setLeftArmPose(new EulerAngle(90 * Math.PI / 180, 270 * Math.PI / 180, 0));
        asB.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));

        new BukkitRunnable() {
            private int dur = 100;

            @Override
            public void run() {
                UUID uuid = target.getUniqueId();
                Double barrierRemain = barriers.getIfPresent(uuid);
                if (dur > 0 && barrierRemain != null && barrierRemain > 0) {
                    dur--;
                } else {
                    asR.remove();
                    asL.remove();
                    asB.remove();
                    Location eyeLocation = target.getEyeLocation();
                    eyeLocation.getWorld().playSound(eyeLocation, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                    cancel();
                    return;
                }
                Location eyeLocation = target.getEyeLocation();
                Location base = eyeLocation.clone();
                Vector f = base.getDirection().setY(0).normalize();
                base.setYaw(base.getYaw() + 50);
                Vector r = base.getDirection().setY(0).normalize();
                base.setYaw(base.getYaw() - 100);
                Vector l = base.getDirection().setY(0).normalize();
                asR.teleport(eyeLocation.clone().add(r.multiply(1 + dur / 100.0)));
                asL.teleport(eyeLocation.clone().add(l.multiply(1 + dur / 100.0)));
                asB.teleport(eyeLocation.clone().subtract(f.multiply(1 + dur / 100.0)));
            }
        }.runTaskTimer(RPGItems.plugin, 0, 1L);
    }

    @Override
    public void deinit() {
        RPGItems.listener.removeEventListener(EntityDamageEvent.class, event);
    }

    @Override
    public PowerResult<Void> tick(Player player, ItemStack stack) {
        Pair<Long, Double> pair = energys.getIfPresent(player.getUniqueId());
        if (pair == null) return PowerResult.noop();
        long currentTimeMillis = System.currentTimeMillis();
        double energy = pair.getValue() - (currentTimeMillis - pair.getKey()) * energyDecay / 1000;
        if (energy <= 0) return PowerResult.noop();
        int level = (int) (energy / energyPerLevel);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 5, level));
        return PowerResult.ok();
    }
}

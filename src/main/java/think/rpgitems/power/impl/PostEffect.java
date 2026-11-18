package think.rpgitems.power.impl;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashMap;
import java.util.Map;

import static think.rpgitems.power.Utils.checkCooldown;

@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = PostEffect.Impl.class)
public class PostEffect extends BasePower {
    @Property
    public String key = "minecraft:blur";
    @Property
    public int duration = 100;
    @Property
    public int cooldown = 0;
    @Property
    public int cost = 0;
    @Property
    public boolean requireHurtByEntity = true;

    private static final Map<Player, ScheduledTask> scheduledTasks = new HashMap<>();

    public int getCooldown() {
        return cooldown;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public int getCost() {
        return cost;
    }

    public String getKey() {
        return key;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public String getName() {
        return "posteffect";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.posteffect", getKey(), getDuration() / 20.0, getCooldown() / 20.0);
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerPlain, PowerBowShoot, PowerHurt, PowerHitTaken, PowerConsume, PowerJump, PowerOffhandClick, PowerProjectileLaunch, PowerSneak, PowerTick {

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            HashMap<String, Object> argsMap = new HashMap<>();
            argsMap.put("key", getKey());
            argsMap.put("duration", getDuration());
            PowerActivateEvent powerEvent = new PowerActivateEvent(player, stack, getPower(), argsMap);
            if (!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if(scheduledTasks.containsKey(player)){
                scheduledTasks.get(player).cancel();
                scheduledTasks.remove(player);
            }else{
                player.postEffects().add(Key.key(getKey()));
            }
            scheduledTasks.put(player, player.getScheduler().runDelayed(RPGItems.plugin, _ -> {
                player.postEffects().remove(Key.key(getKey()));
                scheduledTasks.remove(player);
            }, () -> scheduledTasks.remove(player), getDuration()));
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return PostEffect.this;
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(player, itemStack).with(e.getForce());
        }

        @Override
        public PowerResult<Void> consume(Player player, ItemStack stack, PlayerItemConsumeEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> jump(Player player, ItemStack stack, PlayerJumpEvent event) {
            return fire(player, stack);

        }

        @Override
        public PowerResult<Void> offhandClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);

        }

        @Override
        public PowerResult<Void> projectileLaunch(Player player, ItemStack stack, ProjectileLaunchEvent event) {
            return fire(player, stack);

        }

        @Override
        public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return fire(player, stack);
        }
    }
}
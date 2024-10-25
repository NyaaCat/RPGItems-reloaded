package think.rpgitems.power.impl;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.support.MythicMobsSupport;

import java.util.HashMap;
import java.util.logging.Level;

import static think.rpgitems.power.Utils.checkCooldown;


@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = {
        PowerLeftClick.class,
        PowerRightClick.class,
        PowerSwim.class,
        PowerJump.class,
        PowerPlain.class,
        PowerSneak.class,
        PowerSprint.class,
        PowerHurt.class,
        PowerHit.class,
        PowerHitTaken.class,
        PowerBowShoot.class,
        PowerBeamHit.class,
        PowerLocation.class
}, implClass = MythicSkillCast.Impl.class)
public class MythicSkillCast extends BasePower {
    @Property(order = 0)
    public int cooldown = 20;
    @Property
    public int cost = 0;
    @Property
    public boolean suppressArrow = false;
    @Property
    public String applyForce = "NaN";
    @Property
    public String skill = "";

    public String getSkill() {
        return skill;
    }

    public int getCost() {
        return cost;
    }

    @Override
    public String getName() {
        return "mythicskillcast";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.mythicskillcast",getSkill(),(double) getCooldown() / 20d);
    }

    public boolean isSuppressArrow() {
        return suppressArrow;
    }
    public boolean isApplyForce() {
        return false;
    }

    public int getCooldown() {
        return cooldown;
    }


    public class Impl implements PowerJump, PowerSwim, PowerRightClick, PowerLeftClick, PowerSneak, PowerSprint, PowerHitTaken, PowerHit, PowerPlain, PowerBowShoot, PowerHurt {


        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
                return fire(target, stack).with(damage);
        }


        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if(!MythicMobsSupport.hasSupport()){
                RPGItems.logger.log(Level.WARNING,player.getName()+" is trying to cast MythicMobs skill without MythicMobs installed on your server!");
                return PowerResult.fail();
            }
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("skill",getSkill());
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            BukkitAPIHelper helper = MythicBukkit.inst().getAPIHelper();
            if(helper!=null){
                helper.castSkill(player,getSkill());
                return PowerResult.ok();
            }
            else{
                RPGItems.logger.log(Level.SEVERE,"Error getting APIHelper in MythicMobs!");
                return PowerResult.fail();
            }
        }

        @Override
        public Power getPower() {
            return MythicSkillCast.this;
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            return fire(target, stack);
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
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack).with(damage);
        }

        @Override
        public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> jump(Player player, ItemStack stack, PlayerJumpEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> swim(Player player, ItemStack stack, EntityToggleSwimEvent event) {
            return fire(player, stack);
        }
        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            if (isSuppressArrow()) {
                e.setCancelled(true);
            }
            return fire(player, itemStack).with(isSuppressArrow() ? -1 : e.getForce());
        }
    }
}

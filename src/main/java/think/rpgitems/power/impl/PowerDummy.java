package think.rpgitems.power.impl;

import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.checkCooldown;

@PowerMeta(defaultTrigger = TriggerType.RIGHT_CLICK)
public class PowerDummy extends BasePower implements PowerHit, PowerHitTaken, PowerLeftClick, PowerRightClick, PowerOffhandClick, PowerProjectileHit, PowerSneak, PowerSprint, PowerSwapToMainhand, PowerSwapToOffhand, PowerTick {


    /**
     * Cooldown time of this power
     */
    @Property
    public long cooldown = 20;

    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Property
    public TriggerResult successResult = TriggerResult.OK;

    @Property
    public TriggerResult costResult = TriggerResult.COST;

    @Property
    public TriggerResult cooldownResult = TriggerResult.COOLDOWN;

    private PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.of(cooldownResult);
        if (!getItem().consumeDurability(stack, cost)) PowerResult.of(costResult);
        return PowerResult.of(successResult);
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public String displayText() {
        return null;
    }

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return fire(player, stack).with(damage);
    }

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        return fire(target, stack).with(damage);
    }

    @Override
    public PowerResult<Void> offhandClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> projectileHit(Player player, ItemStack stack, Projectile arrow, ProjectileHitEvent event) {
        return fire(player, stack);
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
    public PowerResult<Boolean> swapToMainhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
        return fire(player, stack).with(true);
    }

    @Override
    public PowerResult<Boolean> swapToOffhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
        return fire(player, stack).with(true);
    }

    @Override
    public PowerResult<Void> tick(Player player, ItemStack stack) {
        return fire(player, stack);
    }
}

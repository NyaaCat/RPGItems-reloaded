package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.VaultUtils;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.AdminHandler;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

import java.util.logging.Level;

import static think.rpgitems.power.Utils.checkCooldown;

@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = PowerEconomy.Impl.class)
public class PowerEconomy extends BasePower {

    private static Economy eco;

    @Property
    public int cooldown = 0;

    @Property
    public double amountToPlayer;

    @Property
    public boolean showFailMessage;

    @Property
    public boolean abortOnFailure = true;

    @Property
    public boolean requireHurtByEntity = true;

    @Override
    public void init(ConfigurationSection section) {
        super.init(section);
        if (eco == null) {
            try {
                eco = VaultUtils.getVaultEconomy();
            } catch (RuntimeException e) {
                RPGItems.plugin.getLogger().log(Level.SEVERE, "Vault Economy not found", e);
                throw new AdminHandler.CommandException("message.error.economy");
            }
        }
    }

    @Override
    public String getName() {
        return "economy";
    }

    @Override
    public String displayText() {
        return I18n.format(getAmountToPlayer() > 0 ? "power.economy.deposit" : "power.economy.withdraw", eco.format(Math.abs(getAmountToPlayer())), (double) getCooldown() / 20d);
    }

    public double getAmountToPlayer() {
        return amountToPlayer;
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    public boolean isAbortOnFailure() {
        return abortOnFailure;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public boolean isShowFailMessage() {
        return showFailMessage;
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerPlain, PowerHit, PowerHurt, PowerHitTaken, PowerBowShoot {


        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true))
                return isAbortOnFailure() ? PowerResult.abort() : PowerResult.cd();
            EconomyResponse economyResponse;
            if (getAmountToPlayer() > 0) {
                economyResponse = eco.depositPlayer(player, getAmountToPlayer());
            } else {
                economyResponse = eco.withdrawPlayer(player, -getAmountToPlayer());
            }
            if (economyResponse.transactionSuccess()) {
                return PowerResult.ok();
            }
            if (isShowFailMessage()) {
                new Message(economyResponse.errorMessage).send(player);
            }
            return isAbortOnFailure() ? PowerResult.abort() : PowerResult.fail();
        }

        @Override
        public Power getPower() {
            return PowerEconomy.this;
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
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
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack).with(damage);
        }
    }
}

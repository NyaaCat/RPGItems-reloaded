package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Message;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import think.rpgitems.AdminCommands;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

import java.util.logging.Level;

import static think.rpgitems.power.Utils.checkCooldown;

@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = {
        PowerLeftClick.class,
        PowerRightClick.class,
        PowerPlain.class,
        PowerSneak.class,
        PowerLivingEntity.class,
        PowerSprint.class,
        PowerHurt.class,
        PowerHit.class,
        PowerHitTaken.class,
        PowerBowShoot.class,
        PowerBeamHit.class,
        PowerLocation.class
}, implClass = Economy.Impl.class, note = "Requires Vault plugin and a Vault-Compatible economy plugin")
public class Economy extends BasePower {

    private static net.milkbowl.vault.economy.Economy eco;

    @Property
    public int coolDown = 0;

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
        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> provider = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (provider != null) {
            eco = provider.getProvider();
        } else {
            RPGItems.plugin.getLogger().log(Level.SEVERE, "Vault Economy not found");
            throw new AdminCommands.CommandException("message.error.economy");
        }
    }

    @Override
    public String getName() {
        return "economy";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault(getAmountToPlayer() > 0 ? "power.economy.deposit" : "power.economy.withdraw", eco.format(Math.abs(getAmountToPlayer())), (double) getCoolDown() / 20d);
    }

    public double getAmountToPlayer() {
        return amountToPlayer;
    }

    /**
     * Cooldown time of this power
     */
    public int getCoolDown() {
        return coolDown;
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
            if (!checkCooldown(getPower(), player, getCoolDown(), true, true))
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
            return Economy.this;
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

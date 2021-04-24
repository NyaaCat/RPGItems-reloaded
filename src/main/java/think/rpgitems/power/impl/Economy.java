package think.rpgitems.power.impl;

import static think.rpgitems.power.Utils.checkCooldown;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.VaultUtils;
import java.util.logging.Level;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.AdminCommands;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

@Meta(
        defaultTrigger = "RIGHT_CLICK",
        generalInterface = {
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
        },
        implClass = Economy.Impl.class,
        note = "Requires Vault plugin and a Vault-Compatible economy plugin")
public class Economy extends BasePower {

    private static net.milkbowl.vault.economy.Economy eco;

    @Property public int cooldown = 0;

    @Property public double amountToPlayer;

    @Property public boolean showFailMessage;

    @Property public boolean abortOnFailure = true;

    @Property public boolean requireHurtByEntity = true;

    @Override
    public void init(ConfigurationSection section) {
        super.init(section);
        if (eco == null) {
            try {
                eco = VaultUtils.getVaultEconomy();
            } catch (RuntimeException e) {
                RPGItems.plugin.getLogger().log(Level.SEVERE, "Vault Economy not found", e);
                throw new AdminCommands.CommandException("message.error.economy");
            }
        }
    }

    @Override
    public String getName() {
        return "economy";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault(
                getAmountToPlayer() > 0 ? "power.economy.deposit" : "power.economy.withdraw",
                eco.format(Math.abs(getAmountToPlayer())),
                (double) getCooldown() / 20d);
    }

    public double getAmountToPlayer() {
        return amountToPlayer;
    }

    /** Cooldown time of this power */
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

    public static class Impl
            implements PowerRightClick<Economy>,
                    PowerLeftClick<Economy>,
                    PowerPlain<Economy>,
                    PowerHit<Economy>,
                    PowerHurt<Economy>,
                    PowerHitTaken<Economy>,
                    PowerBowShoot<Economy> {

        @Override
        public PowerResult<Double> takeHit(
                Economy power,
                Player target,
                ItemStack stack,
                double damage,
                EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Economy power, Player player, ItemStack stack) {
            if (!checkCooldown(power, player, power.getCooldown(), true, true))
                return power.isAbortOnFailure() ? PowerResult.abort() : PowerResult.cd();
            EconomyResponse economyResponse;
            if (power.getAmountToPlayer() > 0) {
                economyResponse = eco.depositPlayer(player, power.getAmountToPlayer());
            } else {
                economyResponse = eco.withdrawPlayer(player, -power.getAmountToPlayer());
            }
            if (economyResponse.transactionSuccess()) {
                return PowerResult.ok();
            }
            if (power.isShowFailMessage()) {
                new Message(economyResponse.errorMessage).send(player);
            }
            return power.isAbortOnFailure() ? PowerResult.abort() : PowerResult.fail();
        }

        @Override
        public Class<? extends Economy> getPowerClass() {
            return Economy.class;
        }

        @Override
        public PowerResult<Void> hurt(
                Economy power, Player target, ItemStack stack, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> leftClick(
                Economy power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> rightClick(
                Economy power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(
                Economy power, Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(power, player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Double> hit(
                Economy power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                EntityDamageByEntityEvent event) {
            return fire(power, player, stack).with(damage);
        }
    }
}

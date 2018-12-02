package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.VaultUtils;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.Handler;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

import java.util.logging.Level;

import static think.rpgitems.power.Utils.checkCooldown;

@PowerMeta(generalInterface = PowerPlain.class)
public class PowerEconomy extends BasePower implements PowerRightClick, PowerLeftClick, PowerPlain, PowerHit {

    private static Economy eco;

    /**
     * Cooldown time of this power
     */
    @Property
    public int cooldown = 20;

    @Property
    public double amountToPlayer;

    @Property
    public boolean showFailMessage;

    @Property
    public boolean abortOnFailure = true;

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }


    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return fire(player, stack).with(damage);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true, true)) return abortOnFailure ? PowerResult.abort() : PowerResult.cd();
        EconomyResponse economyResponse;
        if (amountToPlayer > 0) {
            economyResponse = eco.depositPlayer(player, amountToPlayer);
        } else {
            economyResponse = eco.withdrawPlayer(player, -amountToPlayer);
        }
        if (economyResponse.transactionSuccess()) {
            return PowerResult.ok();
        }
        if (showFailMessage) {
            new Message(economyResponse.errorMessage).send(player);
        }
        return abortOnFailure ? PowerResult.abort() : PowerResult.fail();
    }

    @Override
    public @LangKey(skipCheck = true) String getName() {
        return "economy";
    }

    @Override
    public String displayText() {
        return I18n.format(amountToPlayer > 0 ? "power.economy.deposit" : "power.economy.withdraw", eco.format(Math.abs(amountToPlayer)), (double) cooldown / 20d);
    }

    @Override
    public void init(ConfigurationSection section) {
        super.init(section);
        if (eco == null) {
            try {
                eco = VaultUtils.getVaultEconomy();
            } catch (RuntimeException e) {
                RPGItems.plugin.getLogger().log(Level.SEVERE, "Vault Economy not found", e);
                throw new Handler.CommandException("message.error.economy");
            }
        }
    }
}

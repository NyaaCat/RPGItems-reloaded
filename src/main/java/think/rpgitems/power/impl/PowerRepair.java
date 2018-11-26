package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import com.google.common.base.Strings;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import java.util.Collections;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power repair.
 * <p>
 * Repair the item with some material
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class)
public class PowerRepair extends BasePower implements PowerRightClick, PowerLeftClick, PowerPlain {

    /**
     * Cooldown time of this power
     */
    @Property
    public int cooldown = 0;

    @Property(order = 2, required = true)
    public int durability = 20;

    @Property(order = 0)
    public String display = "";

    @Property(order = 4)
    public boolean isSneak;

    @Property(order = 1)
    public ItemStack material;

    @Property
    public RepairMode mode = RepairMode.DEFAULT;

    @Property
    public boolean abortOnSuccess = false;

    @Property
    public boolean abortOnFailure = false;

    @Property
    public String customMessage;
    
    @Property
    public int amount = 1;

    @Override
    public void init(ConfigurationSection section) {
        if (section.isBoolean("isRight")) {
            triggers = section.getBoolean("isRight", true) ? Collections.singleton(Trigger.RIGHT_CLICK) : Collections.singleton(Trigger.LEFT_CLICK);
        }
        super.init(section);
    }

    @Override
    public String getName() {
        return "repair";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + display;
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        if (player.isSneaking() == isSneak) {
            return fire(player, stack);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        if (player.isSneaking() == isSneak) {
            return fire(player, stack);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true)) PowerResult.cd();
        int max = getItem().getMaxDurability();
        int itemDurability = getItem().getDurability(stack);
        int delta = max - itemDurability;
        if (mode != RepairMode.ALWAYS) {
            if (max == -1 || delta == 0) {
                return PowerResult.noop();
            }
            if (delta < this.durability && mode != RepairMode.ALLOW_OVER) {
                return PowerResult.noop();
            }
        }
        if (removeItem(player.getInventory(), material, amount)) {
            getItem().setDurability(stack, Math.max(itemDurability + this.durability, max));
            return abortOnSuccess ? PowerResult.abort() : PowerResult.ok();
        } else {
            BaseComponent msg = Strings.isNullOrEmpty(customMessage) ?
                                        new TextComponent(I18n.format("message.error.need_material", material.getType().name())) :
                                        new TextComponent(customMessage);
            HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[]{new TextComponent(ItemStackUtils.itemToJson(material))});
            msg.setHoverEvent(hover);
            new Message("").append(msg).send(player);
            return abortOnFailure ? PowerResult.abort() : PowerResult.fail();
        }
    }

    private boolean removeItem(Inventory inventory, ItemStack item, int amount) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack tmp = inventory.getItem(slot);
            if (tmp != null && tmp.getType() != Material.AIR && tmp.getAmount() >= amount && tmp.isSimilar(item)) {
                if (tmp.getAmount() > amount) {
                    tmp.setAmount(tmp.getAmount() - amount);
                    inventory.setItem(slot, tmp);
                    return true;
                } else {
                    inventory.setItem(slot, new ItemStack(Material.AIR));
                    return true;
                }
            }
        }
        return false;
    }

    public enum RepairMode {
        DEFAULT,
        ALLOW_OVER,
        ALWAYS,
    }
}

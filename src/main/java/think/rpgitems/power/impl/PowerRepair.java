package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import java.util.Collections;

/**
 * Power repair.
 * <p>
 * Repair the item with some material
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK")
public class PowerRepair extends BasePower implements PowerRightClick, PowerLeftClick {

    @Property(order = 2, required = true)
    public int durability = 20;

    @Property(order = 0)
    public String display = "";

    @Property(order = 4)
    public boolean isSneak;

    @Property(order = 1)
    public ItemStack material;

    @Property
    public boolean abortOnSuccess = false;

    @Override
    public void init(ConfigurationSection section) {
        triggers = section.getBoolean("isRight", true) ? Collections.singleton(Trigger.RIGHT_CLICK) : Collections.singleton(Trigger.LEFT_CLICK);
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
            return repair(player, stack);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        if (player.isSneaking() == isSneak) {
            return repair(player, stack);
        }
        return PowerResult.noop();
    }

    private PowerResult<Void> repair(Player player, ItemStack stack) {
        int max = getItem().getMaxDurability();
        int itemDurability = getItem().getDurability(stack);
        if (getItem().getMaxDurability() != -1 && max - this.durability >= itemDurability) {
            if (removeItem(player.getInventory(), material)) {
                getItem().setDurability(stack, itemDurability + this.durability);
                return abortOnSuccess ? PowerResult.abort() : PowerResult.ok();
            } else {
                BaseComponent msg = new TextComponent(I18n.format("message.error.need_material", material.getType().name()));
                HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[]{new TextComponent(ItemStackUtils.itemToJson(material))});
                msg.setHoverEvent(hover);
                new Message("").append(msg).send(player);
                return PowerResult.fail();
            }
        }
        return PowerResult.noop();
    }

    private boolean removeItem(Inventory inventory, ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack tmp = inventory.getItem(slot);
            if (tmp != null && tmp.getType() != Material.AIR && tmp.getAmount() > 0 && tmp.isSimilar(item)) {
                if (tmp.getAmount() > 1) {
                    tmp.setAmount(tmp.getAmount() - 1);
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
}

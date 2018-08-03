package think.rpgitems.power;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.power.types.PowerLeftClick;
import think.rpgitems.power.types.PowerRightClick;
import think.rpgitems.utils.ReflectionUtil;

public class PowerRepair extends Power implements PowerRightClick, PowerLeftClick {
    @Property(order = 2)
    public int durability = 20;
    @Property(order = 0)
    public String display = "";
    @Property(order = 3)
    public boolean isRight;
    @Property(order = 4)
    public boolean isSneak;
    @Property(order = 1)
    public ItemStack material;


    @Override
    public void init(ConfigurationSection s) {
        material = s.getItemStack("material");
        durability = s.getInt("durability", 20);
        display = s.getString("display");
        isRight = s.getBoolean("isRight", true);
        isSneak = s.getBoolean("isSneak", true);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("durability", durability);
        s.set("material", material);
        s.set("display", display);
        s.set("isRight", isRight);
        s.set("isSneak", isSneak);
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
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (isRight && player.isSneaking() == isSneak) {
            repair(player, stack, clicked);
        }
    }

    @Override
    public void leftClick(Player player, ItemStack stack, Block clicked) {
        if (!isRight && player.isSneaking() == isSneak) {
            repair(player, stack, clicked);
        }
    }

    private void repair(Player player, ItemStack stack, Block block) {
        int max = item.getMaxDurability();
        int itemDurability = item.getDurability(stack);
        if (item.getMaxDurability() != -1 && max - this.durability >= itemDurability) {
            if (removeItem(player.getInventory(), material)) {
                item.setDurability(stack, itemDurability + this.durability);
            } else {
                BaseComponent msg = new TextComponent(I18n.format("message.error.need_material", material.getType().name()));
                HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[]{new TextComponent(ReflectionUtil.convertItemStackToJson(material))});
                msg.setHoverEvent(hover);
                player.spigot().sendMessage(msg);
            }
        }
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

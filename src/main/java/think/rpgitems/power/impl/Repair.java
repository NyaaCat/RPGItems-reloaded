package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import com.google.common.base.Strings;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.BaseTriggers;

import java.util.Collections;
import java.util.HashMap;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power repair.
 * <p>
 * Repair the item with some material
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = Repair.Impl.class)
public class Repair extends BasePower {

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
    public boolean allowBreak = true;

    @Property
    public boolean abortOnSuccess = false;

    @Property
    public boolean abortOnFailure = false;

    @Property
    public String customMessage;

    @Property
    public int amount = 1;

    @Property
    public boolean showFailMsg = true;

    @Property
    public boolean requireHurtByEntity = true;

    @Override
    public void init(ConfigurationSection section) {
        if (section.isBoolean("isRight")) {
            triggers = section.getBoolean("isRight", true) ? Collections.singleton(BaseTriggers.RIGHT_CLICK) : Collections.singleton(BaseTriggers.LEFT_CLICK);
        }
        super.init(section);
    }

    public int getAmount() {
        return amount;
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public int getDurability() {
        return durability;
    }

    public ItemStack getMaterial() {
        return material;
    }

    public RepairMode getMode() {
        return mode;
    }

    @Override
    public String getName() {
        return "repair";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + getDisplay();
    }

    public String getDisplay() {
        return display;
    }

    public boolean isAbortOnFailure() {
        return abortOnFailure;
    }

    public boolean isAbortOnSuccess() {
        return abortOnSuccess;
    }

    public boolean isAllowBreak() {
        return allowBreak;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public boolean isShowFailMsg() {
        return showFailMsg;
    }

    public boolean isSneak() {
        return isSneak;
    }

    public enum RepairMode {
        DEFAULT,
        ALLOW_OVER,
        ALWAYS,
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerPlain, PowerHitTaken, PowerHurt, PowerBowShoot {

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower());
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            int max = getItem().getMaxDurability();
            int repairCount = 0;
            for (int i = 0; i < getAmount(); i++) {
                int itemDurability = getItem().getItemStackDurability(stack).orElseThrow(() -> new IllegalStateException("Repair is not allowed on item without durability"));
                int delta = max - itemDurability;
                if (getMode() != RepairMode.ALWAYS) {
                    if (max == -1 || delta == 0) {
                        break;
                    }
                    if (getDurability() > delta && getMode() != RepairMode.ALLOW_OVER) {
                        break;
                    }
                }
                if (!isAllowBreak() && getDurability() + itemDurability < 0) {
                    break;
                }
                if (removeItem(player.getInventory(), getMaterial(), 1)) {
                    getItem().setItemStackDurability(stack, Math.min(itemDurability + getDurability(), max));
                    repairCount++;
                } else {
                    if (isShowFailMsg()) {
                        Component msg = Strings.isNullOrEmpty(getCustomMessage()) ?
                                Component.text(I18n.formatDefault("message.error.need_material", (getMaterial().hasItemMeta() && getMaterial().getItemMeta().hasDisplayName()) ? getMaterial().getItemMeta().getDisplayName() : getMaterial().getType().getKey().toString())) :
                                Component.text(getCustomMessage());
                        msg = msg.hoverEvent(getMaterial());
                        new Message("").append(msg).send(player);
                    }
                    return isAbortOnFailure() ? PowerResult.abort() : PowerResult.fail();
                }
            }
            if (repairCount == 0) {
                return PowerResult.noop();
            }
            return isAbortOnSuccess() ? PowerResult.abort() : PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return Repair.this;
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

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            if (player.isSneaking() == isSneak()) {
                return fire(player, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            if (player.isSneaking() == isSneak()) {
                return fire(player, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(player, itemStack).with(e.getForce());
        }
    }
}

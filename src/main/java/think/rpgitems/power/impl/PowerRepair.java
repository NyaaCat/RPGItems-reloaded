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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
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
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = PowerRepair.Impl.class)
public class PowerRepair extends BasePower {

    @Property
    private int cooldown = 0;

    @Property(order = 2, required = true)
    private int durability = 20;

    @Property(order = 0)
    private String display = "";

    @Property(order = 4)
    private boolean isSneak;

    @Property(order = 1)
    private ItemStack material;

    @Property
    private RepairMode mode = RepairMode.DEFAULT;

    @Property
    private boolean allowBreak = true;

    @Property
    private boolean abortOnSuccess = false;

    @Property
    private boolean abortOnFailure = false;

    @Property
    private String customMessage;

    @Property
    private int amount = 1;

    @Property
    private boolean showFailMsg = true;

    @Property
    private boolean requireHurtByEntity = true;

    @Override
    public void init(ConfigurationSection section) {
        if (section.isBoolean("isRight")) {
            triggers = section.getBoolean("isRight", true) ? Collections.singleton(Trigger.RIGHT_CLICK) : Collections.singleton(Trigger.LEFT_CLICK);
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

    public String getDisplay() {
        return display;
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

    public class Impl implements PowerRightClick, PowerLeftClick, PowerPlain, PowerHitTaken, PowerHurt, PowerBowShoot {

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
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

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) PowerResult.cd();
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
                        BaseComponent msg = Strings.isNullOrEmpty(getCustomMessage()) ?
                                                    new TextComponent(I18n.format("message.error.need_material", getMaterial().getType().name())) :
                                                    new TextComponent(getCustomMessage());
                        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[]{new TextComponent(ItemStackUtils.itemToJson(getMaterial()))});
                        msg.setHoverEvent(hover);
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
        public Power getPower() {
            return PowerRepair.this;
        }
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

    public void setAbortOnFailure(boolean abortOnFailure) {
        this.abortOnFailure = abortOnFailure;
    }

    public void setAbortOnSuccess(boolean abortOnSuccess) {
        this.abortOnSuccess = abortOnSuccess;
    }

    public void setAllowBreak(boolean allowBreak) {
        this.allowBreak = allowBreak;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public void setDurability(int durability) {
        this.durability = durability;
    }

    public void setMaterial(ItemStack material) {
        this.material = material;
    }

    public void setMode(RepairMode mode) {
        this.mode = mode;
    }

    public void setRequireHurtByEntity(boolean requireHurtByEntity) {
        this.requireHurtByEntity = requireHurtByEntity;
    }

    public void setShowFailMsg(boolean showFailMsg) {
        this.showFailMsg = showFailMsg;
    }

    public void setSneak(boolean sneak) {
        isSneak = sneak;
    }

    public enum RepairMode {
        DEFAULT,
        ALLOW_OVER,
        ALWAYS,
    }
}

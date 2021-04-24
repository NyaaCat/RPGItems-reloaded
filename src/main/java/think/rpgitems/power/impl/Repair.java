package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import com.google.common.base.Strings;
import java.util.Collections;
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
import think.rpgitems.power.trigger.BaseTriggers;

/**
 * Power repair.
 *
 * <p>Repair the item with some material
 */
@Meta(
    defaultTrigger = "RIGHT_CLICK",
    generalInterface = PowerPlain.class,
    implClass = Repair.Impl.class)
public class Repair extends BasePower {
  @Property(order = 2, required = true)
  public int durability = 20;

  @Property(order = 0)
  public String display = "";

  @Property(order = 4)
  public boolean isSneak;

  @Property(order = 1)
  public ItemStack material;

  @Property public RepairMode mode = RepairMode.DEFAULT;

  @Property public boolean allowBreak = true;

  @Property public boolean abortOnSuccess = false;

  @Property public boolean abortOnFailure = false;

  @Property public String customMessage;

  @Property public int amount = 1;

  @Property public boolean showFailMsg = true;

  @Property public boolean requireHurtByEntity = true;

  @Override
  public void init(ConfigurationSection section) {
    if (section.isBoolean("isRight")) {
      triggers =
          section.getBoolean("isRight", true)
              ? Collections.singleton(BaseTriggers.RIGHT_CLICK)
              : Collections.singleton(BaseTriggers.LEFT_CLICK);
    }
    super.init(section);
  }

  public int getAmount() {
    return amount;
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

  public static class Impl
      implements PowerRightClick<Repair>,
          PowerLeftClick<Repair>,
          PowerPlain<Repair>,
          PowerHitTaken<Repair>,
          PowerHurt<Repair>,
          PowerBowShoot<Repair> {

    @Override
    public PowerResult<Double> takeHit(
        Repair power, Player target, ItemStack stack, double damage, EntityDamageEvent event) {
      if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
        return fire(power, target, stack).with(damage);
      }
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> fire(Repair power, Player player, ItemStack stack) {
      int max = power.getItem().getMaxDurability();
      int repairCount = 0;
      for (int i = 0; i < power.getAmount(); i++) {
        int itemDurability =
            power
                .getItem()
                .getItemStackDurability(stack)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "Repair is not allowed on item without durability"));
        int delta = max - itemDurability;
        if (power.getMode() != RepairMode.ALWAYS) {
          if (max == -1 || delta == 0) {
            break;
          }
          if (power.getDurability() > delta && power.getMode() != RepairMode.ALLOW_OVER) {
            break;
          }
        }
        if (!power.isAllowBreak() && power.getDurability() + itemDurability < 0) {
          break;
        }
        if (removeItem(player.getInventory(), power.getMaterial(), 1)) {
          power
              .getItem()
              .setItemStackDurability(stack, Math.min(itemDurability + power.getDurability(), max));
          repairCount++;
        } else {
          if (power.isShowFailMsg()) {
            BaseComponent msg =
                Strings.isNullOrEmpty(power.getCustomMessage())
                    ? new TextComponent(
                        I18n.formatDefault(
                            "message.error.need_material",
                            (power.getMaterial().hasItemMeta()
                                    && power.getMaterial().getItemMeta().hasDisplayName())
                                ? power.getMaterial().getItemMeta().getDisplayName()
                                : power.getMaterial().getType().getKey().toString()))
                    : new TextComponent(power.getCustomMessage());
            HoverEvent hover =
                new HoverEvent(
                    HoverEvent.Action.SHOW_ITEM,
                    new BaseComponent[] {
                      new TextComponent(ItemStackUtils.itemToJson(power.getMaterial()))
                    });
            msg.setHoverEvent(hover);
            new Message("").append(msg).send(player);
          }
          return power.isAbortOnFailure() ? PowerResult.abort() : PowerResult.fail();
        }
      }
      if (repairCount == 0) {
        return PowerResult.noop();
      }
      return power.isAbortOnSuccess() ? PowerResult.abort() : PowerResult.ok();
    }

    @Override
    public Class<? extends Repair> getPowerClass() {
      return Repair.class;
    }

    private boolean removeItem(Inventory inventory, ItemStack item, int amount) {
      for (int slot = 0; slot < inventory.getSize(); slot++) {
        ItemStack tmp = inventory.getItem(slot);
        if (tmp != null
            && tmp.getType() != Material.AIR
            && tmp.getAmount() >= amount
            && tmp.isSimilar(item)) {
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
    public PowerResult<Void> hurt(
        Repair power, Player target, ItemStack stack, EntityDamageEvent event) {
      if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
        return fire(power, target, stack);
      }
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> rightClick(
        Repair power, Player player, ItemStack stack, PlayerInteractEvent event) {
      if (player.isSneaking() == power.isSneak()) {
        return fire(power, player, stack);
      }
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> leftClick(
        Repair power, Player player, ItemStack stack, PlayerInteractEvent event) {
      if (player.isSneaking() == power.isSneak()) {
        return fire(power, player, stack);
      }
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Float> bowShoot(
        Repair power, Player player, ItemStack itemStack, EntityShootBowEvent e) {
      return fire(power, player, itemStack).with(e.getForce());
    }
  }
}

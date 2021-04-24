/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.power.impl;

import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.BaseTriggers;

/**
 * Power consume.
 *
 * <p>The consume power will remove one item on click. With {@link #cooldown cooldown} time (ticks).
 */
@Meta(
    defaultTrigger = "RIGHT_CLICK",
    generalInterface = PowerPlain.class,
    implClass = Consume.Impl.class)
public class Consume extends BasePower {

  @Property public boolean requireHurtByEntity = true;

  @Override
  public void init(ConfigurationSection section) {
    boolean isRight = section.getBoolean("isRight", true);
    triggers = Collections.singleton(isRight ? BaseTriggers.RIGHT_CLICK : BaseTriggers.LEFT_CLICK);
    super.init(section);
  }

  @Override
  public String getName() {
    return "consume";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault("power.consume");
  }

  public boolean isRequireHurtByEntity() {
    return requireHurtByEntity;
  }

  public static class Impl
      implements PowerPlain<Consume>,
          PowerRightClick<Consume>,
          PowerLeftClick<Consume>,
          PowerSneak<Consume>,
          PowerHitTaken<Consume>,
          PowerHurt<Consume>,
          PowerSprint<Consume>,
          PowerAttachment<Consume> {
    @Override
    public PowerResult<Void> rightClick(
        Consume power, final Player player, ItemStack stack, PlayerInteractEvent event) {
      return fire(power, player, stack);
    }

    public PowerResult<Void> fire(Consume power, final Player player, ItemStack s) {
      int count = s.getAmount() - 1;
      if (count == 0) {
        s.setAmount(0);
        s.setType(Material.AIR);
      } else {
        s.setAmount(count);
      }

      return PowerResult.ok();
    }

    @Override
    public Class<? extends Consume> getPowerClass() {
      return Consume.class;
    }

    @Override
    public PowerResult<Void> leftClick(
        Consume power, final Player player, ItemStack stack, PlayerInteractEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Void> sneak(
        Consume power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Void> sprint(
        Consume power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Double> takeHit(
        Consume power, Player target, ItemStack stack, double damage, EntityDamageEvent event) {
      if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
        return fire(power, target, stack).with(damage);
      }
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> hurt(
        Consume power, Player target, ItemStack stack, EntityDamageEvent event) {
      if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
        return fire(power, target, stack);
      }
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> attachment(
        Consume power,
        Player player,
        ItemStack stack,
        RPGItem originItem,
        Event originEvent,
        ItemStack originStack) {
      return fire(power, player, stack);
    }
  }
}

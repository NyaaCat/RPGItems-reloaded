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

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

/**
 * Power fireball.
 *
 * <p>The fireball power will fire an fireball on right click.
 */
@Meta(
    defaultTrigger = "RIGHT_CLICK",
    generalInterface = PowerPlain.class,
    implClass = Fireballs.Impl.class)
public class Fireballs extends BasePower {

  @Override
  public String getName() {
    return "fireball";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault("power.fireball", 0);
  }

  public static class Impl
      implements PowerRightClick<Fireballs>,
          PowerLeftClick<Fireballs>,
          PowerSprint<Fireballs>,
          PowerSneak<Fireballs>,
          PowerPlain<Fireballs> {

    @Override
    public PowerResult<Void> rightClick(
        Fireballs power, Player player, ItemStack stack, PlayerInteractEvent event) {
      return fire(power, player, stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PowerResult<Void> fire(Fireballs power, Player player, ItemStack stack) {
      player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
      Events.registerRPGProjectile(power.getItem(), stack, player);
      SmallFireball entity = player.launchProjectile(SmallFireball.class);
      entity.setPersistent(false);
      return PowerResult.ok();
    }

    @Override
    public Class<? extends Fireballs> getPowerClass() {
      return Fireballs.class;
    }

    @Override
    public PowerResult<Void> leftClick(
        Fireballs power, Player player, ItemStack stack, PlayerInteractEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Void> sneak(
        Fireballs power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Void> sprint(
        Fireballs power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
      return fire(power, player, stack);
    }
  }
}

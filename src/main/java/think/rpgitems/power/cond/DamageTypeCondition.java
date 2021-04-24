package think.rpgitems.power.cond;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.data.Context;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

@Meta(marker = true)
public class DamageTypeCondition extends BaseCondition<Void> {

  @Property(order = 0, required = true)
  public String id;

  @Property public boolean isStatic = false;

  @Property public boolean isCritical = false;

  @Override
  public String id() {
    return id;
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public boolean isCritical() {
    return isCritical;
  }

  @Property
  //    @AcceptedValue({"melee", "ranged", "magic", "summon"})
  public String damageType = "";

  @Override
  public PowerResult<Void> check(
      Player player, ItemStack stack, Map<PropertyHolder, PowerResult<?>> context) {
    try {
      String damageType =
          (String) Context.instance().get(player.getUniqueId(), RPGItem.DAMAGE_TYPE);
      if (damageType == null) {
        return PowerResult.fail();
      }
      return damageType.equals(this.damageType) ? PowerResult.ok() : PowerResult.fail();
    } catch (Exception ignored) {
    }
    return PowerResult.fail();
  }

  @Override
  public String getName() {
    return "damagetype";
  }
}

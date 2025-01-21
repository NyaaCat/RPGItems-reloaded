package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.BaseTriggers;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class, implClass = Attachments.Impl.class)
public class Attachments extends BasePower {

    @Property
    public List<EquipmentSlot> allowedSlots;

    @Property
    public List<Integer> allowedInvSlots;

    @Property
    public int limit;

    @Property
    public Set<String> allowedItems;

    @Property
    public boolean requireHurtByEntity = true;

    public List<Integer> getAllowedInvSlots() {
        return allowedInvSlots;
    }

    public Set<String> getAllowedItems() {
        return allowedItems;
    }

    public List<EquipmentSlot> getAllowedSlots() {
        return allowedSlots;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public String getName() {
        return "attachments";
    }

    @Override
    public String displayText() {
        return null;
    }

    /**
     * Whether to require hurt by entity for HURT trigger
     */
    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public class Impl implements PowerTick, PowerRightClick, PowerLeftClick, PowerOffhandClick, PowerPlain, PowerHit, PowerSneaking, PowerHurt, PowerHitTaken, PowerBowShoot, PowerConsume {

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            return fire(player, stack, null);
        }

        public PowerResult<Void> fire(Player player, ItemStack stack, Event event) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower());
            if(!powerEvent.callEvent())
                return PowerResult.fail();
            Set<RPGItem> allow = (getAllowedItems() == null || getAllowedItems().isEmpty()) ? null : getAllowedItems().stream().flatMap(s -> {
                try {
                    int uid = Integer.parseInt(s);
                    Set<RPGItem> items = ItemManager.getItems(uid);
                    return items.stream();
                } catch (NumberFormatException e) {
                    Set<RPGItem> items = ItemManager.getItems(s);
                    return items.stream();
                }
            }).collect(Collectors.toSet());
            int num = 0;
            ItemStack itemStack = null;
            PlayerInventory inventory = player.getInventory();
            if (getAllowedSlots() != null) {
                for (EquipmentSlot allowedSlot : getAllowedSlots()) {
                    switch (allowedSlot) {
                        case HAND:
                            itemStack = inventory.getItemInMainHand();
                            break;
                        case OFF_HAND:
                            itemStack = inventory.getItemInOffHand();
                            break;
                        case FEET:
                            itemStack = inventory.getBoots();
                            break;
                        case LEGS:
                            itemStack = inventory.getLeggings();
                            break;
                        case CHEST:
                            itemStack = inventory.getChestplate();
                            break;
                        case HEAD:
                            itemStack = inventory.getHelmet();
                            break;
                    }
                    if (attach(player, stack, event, itemStack, allow)) {
                        num += 1;
                    }
                    if (num >= getLimit()) return PowerResult.ok();
                }
            }
            if (getAllowedInvSlots() == null || getAllowedInvSlots().isEmpty()) {
                for (ItemStack envSlot : inventory.getContents()) {
                    if (attach(player, stack, event, envSlot, allow)) {
                        num += 1;
                    }
                    if (num >= getLimit()) return PowerResult.ok();
                }
            } else {
                for (int envSlot : getAllowedInvSlots()) {
                    if (envSlot < 0) break;
                    itemStack = inventory.getItem(envSlot);
                    if (attach(player, stack, event, itemStack, allow)) {
                        num += 1;
                    }
                    if (num >= getLimit()) return PowerResult.ok();
                }
            }

            if (num == 0) {
                return PowerResult.fail();
            } else {
                return PowerResult.ok();
            }
        }

        public boolean attach(Player player, ItemStack stack, Event event, ItemStack itemStack, Set<RPGItem> allow) {
            if (itemStack == null) return false;
            if (itemStack.equals(stack)) return false;
            Optional<RPGItem> optItem = ItemManager.toRPGItem(itemStack);
            if (!optItem.isPresent()) return false;
            RPGItem item = optItem.get();
            if (allow != null && !allow.contains(item)) return false;
            item.power(player, itemStack, event, BaseTriggers.ATTACHMENT, Pair.of(stack, event));
            return true;
        }

        @Override
        public PowerResult<Void> sneaking(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack, event).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack, event);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack, event).with(damage);
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack, event);
        }

        @Override
        public PowerResult<Void> offhandClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack, event);
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack, event);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }

        @Override
        public Power getPower() {
            return Attachments.this;
        }

        @Override
        public PowerResult<Void> consume(Player player, ItemStack stack, PlayerItemConsumeEvent event){
            return fire(player, stack, event);
        }
    }
}

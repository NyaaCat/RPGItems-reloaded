package think.rpgitems.support;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.adapters.item.ItemComponentBukkitItemStack;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import io.lumine.mythic.core.drops.Drop;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class MythicMobsEvents implements Listener {
    @EventHandler
    public void onDropLoad(MythicDropLoadEvent event) {
        String dropName = event.getDropName();
        if (dropName == null || !dropName.toLowerCase(Locale.ROOT).startsWith("rpg.")) {
            return;
        }

        String itemNameFromDrop = dropName.substring(4);
        for (String name : ItemManager.itemNames()) {
            if (name.equalsIgnoreCase(itemNameFromDrop)) {
                event.register(new RPGItemDrop(event.getContainer().getConfigLine(), event.getConfig()));
                return;
            }
        }
    }

    @EventHandler
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            EntityEquipment mobEquipment = entity.getEquipment();
            if (mobEquipment == null) {
                return;
            }
            List<String> equipmentList = event.getMobType().getConfig().getStringList("Requipment");
            for (String equipment : equipmentList) {
                String[] args = equipment.split(" ");
                String[] eas = args[0].split(":"); //eas: Equipment and slot

                RPGItem rpgItem = ItemManager.getItem(eas[0]).orElse(null);
                if (rpgItem == null || args.length > 1 && Math.random() > Double.parseDouble(args[1])) {
                    continue;
                }
                ItemStack itemStack = rpgItem.toItemStack();
                switch (eas[1].toUpperCase(Locale.ROOT)) {
                    case "-1":
                    case "OFFHAND":
                        mobEquipment.setItemInOffHand(itemStack);
                        break;
                    case "0":
                    case "HAND":
                        mobEquipment.setItemInMainHand(itemStack);
                        break;
                    case "1":
                    case "FEET":
                        mobEquipment.setBoots(itemStack);
                        break;
                    case "2":
                    case "LEGS":
                        mobEquipment.setLeggings(itemStack);
                        break;
                    case "3":
                    case "CHEST":
                        mobEquipment.setChestplate(itemStack);
                        break;
                    case "4":
                    case "HEAD":
                        mobEquipment.setHelmet(itemStack);
                        break;
                    default:
                        RPGItems.logger.warning("Unknown equipment slot: " + eas[1]);
                }
            }
        }
    }

    static class RPGItemDrop extends Drop implements IItemDrop {
        public RPGItemDrop(String line, MythicLineConfig config) {
            super(line, config);
        }

        @Override
        public AbstractItemStack getDrop(DropMetadata meta, double p1) {
            String name = null;
            int amount = 1;
            if (meta != null) {
                String[] parts = this.getLine().split(" ");
                name = parts[0].split("\\.")[1];
                amount = toAmount(parts[1]);
            }
            if (name != null && ItemManager.getItem(name).isPresent()) {
                ItemStack stack = ItemManager.getItem(name).get().toItemStack();
                stack.setAmount(amount);
                return new ItemComponentBukkitItemStack(stack);
            } else {
                return new ItemComponentBukkitItemStack(new ItemStack(Material.AIR));
            }
        }
    }

    private static int toAmount(String input) {
        String[] parts = input.split("-");
        int min, max;

        if (parts.length == 1) {
            min = roll(parts[0]);
            max = min;
        } else {
            min = roll(parts[0]);
            max = roll(parts[1]);
        }

        if (max < min) {
            int temp = min;
            min = max;
            max = temp;
        }

        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static int roll(String value) {
        double num;
        try {
            num = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }

        int base = (int) num;
        double decimal = num - base;

        if (ThreadLocalRandom.current().nextDouble() < decimal) {
            base += 1;
        }

        return base;
    }
}

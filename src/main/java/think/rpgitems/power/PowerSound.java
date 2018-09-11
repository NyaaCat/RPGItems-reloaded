package think.rpgitems.power;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.types.PowerLeftClick;
import think.rpgitems.power.types.PowerRightClick;

/**
 * Power sound.
 * <p>
 * Play a sound
 * </p>
 */
public class PowerSound extends Power implements PowerLeftClick, PowerRightClick {
    /**
     * Pitch of sound
     */
    public float pitch = 1.0f;
    /**
     * Volume of sound
     */
    public float volume = 1.0f;
    /**
     * Sound to be played
     */
    public String sound = "";
    /**
     * Cost of this power
     */
    public int consumption = 0;
    /**
     * Display text of this power
     */
    public String display = "Plays sound";
    /**
     * Cooldown time of this power
     */
    public long cooldown = 20;
    /**
     * Whether triggers when right click
     */
    public boolean isRight = true;


    @Override
    public void init(ConfigurationSection s) {
        display = s.getString("display");
        sound = s.getString("sound");
        consumption = s.getInt("consumption", 0);
        cooldown = s.getLong("cooldown");
        pitch = (float) s.getDouble("pitch");
        volume = (float) s.getDouble("volume");
        isRight = s.getBoolean("isRight", true);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("isRight", isRight);
        s.set("pitch", pitch);
        s.set("volume", volume);
        s.set("display", display);
        s.set("sound", sound);
        s.set("cooldown", cooldown);
        s.set("consumption", consumption);
    }

    @Override
    public String getName() {
        return "sound";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + display;
    }

    @Override
    public void leftClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        if (isRight || !checkCooldown(player, cooldown, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        Location location = player.getLocation();
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        if (!isRight || !checkCooldown(player, cooldown, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        Location location = player.getLocation();
        location.getWorld().playSound(location, sound, volume, pitch);
    }
}

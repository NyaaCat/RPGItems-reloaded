package think.rpgitems.power.impl;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power sound.
 * <p>
 * Play a sound
 * </p>
 */
@PowerMeta(defaultTrigger = TriggerType.RIGHT_CLICK)
public class PowerSound extends BasePower implements PowerLeftClick, PowerRightClick {
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
    public int cost = 0;
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
        cost = s.getInt("cost", 0);
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
        s.set("cost", cost);
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
    public PowerResult<Void> leftClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        Location location = player.getLocation();
        location.getWorld().playSound(location, sound, volume, pitch);
        return PowerResult.ok();
    }
}

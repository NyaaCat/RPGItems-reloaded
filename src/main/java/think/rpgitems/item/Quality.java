package think.rpgitems.item;

import org.bukkit.ChatColor;

public enum Quality {
    TRASH(ChatColor.GRAY.toString(), "7"), COMMON(ChatColor.WHITE.toString(), "f"), UNCOMMON(ChatColor.GREEN.toString(), "a"), RARE(ChatColor.BLUE.toString(), "9"), EPIC(ChatColor.DARK_PURPLE.toString(), "5"), LEGENDARY(ChatColor.GOLD.toString(), "6");

    public final String colour;
    public final String cCode;

    Quality(String colour, String code) {
        this.colour = colour;
        this.cCode = code;
    }
}

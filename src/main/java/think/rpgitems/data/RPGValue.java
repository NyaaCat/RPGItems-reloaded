package think.rpgitems.data;

import org.bukkit.entity.Player;
import think.rpgitems.item.RPGItem;

import java.util.HashMap;

public class RPGValue {

    static HashMap<String, RPGValue> map = new HashMap<>();
    Object value;

    public RPGValue(Player player, RPGItem item, String name, Object value) {
        this.value = value;
        map.put(player.getName() + "." + item.getUID() + "." + name, this);
    }

    public static RPGValue get(Player player, RPGItem item, String name) {
        return map.get(player.getName() + "." + item.getUID() + "." + name);
    }

    public void set(Object value) {
        this.value = value;
    }

    public boolean asBoolean() {
        return (Boolean) value;
    }

    public byte asByte() {
        return (Byte) value;
    }

    public double asDouble() {
        return (Double) value;
    }

    public float asFloat() {
        return (Float) value;
    }

    public int asInt() {
        return (Integer) value;
    }

    public long asLong() {
        return (Long) value;
    }

    public short asShort() {
        return (Short) value;
    }

    public String asString() {
        return (String) value;
    }

    public Object value() {
        return value;
    }

}

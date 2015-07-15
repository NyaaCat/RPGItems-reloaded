package think.rpgitems.data;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * RPGMetadata is a (bad) way of tagging extra data onto an item without it showing up on the item
 * itself. This uses a similar system to Notch's entity metadata system. The data will be encoded in
 * minecraft colour codes which makes this really inefficient because 1 byte -> 2 bytes(Hex) -> 4
 * bytes( Colour codes). RPGMetadatais prefixed with CAFE to allow it to be easily detected.
 * 
 */
public class RPGMetadata extends TIntObjectHashMap<Object> {

    private final static String METADATA_PREFIX = ChatColor.COLOR_CHAR + "c" + ChatColor.COLOR_CHAR + "a" + ChatColor.COLOR_CHAR + "f" + ChatColor.COLOR_CHAR + "e";

    public static final int DURABILITY = 0;
    
    public static boolean hasRPGMetadata(ItemStack item) {
        if (!item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore())
            return false;
        List<String> lore = meta.getLore();
        if (lore.size() == 0)
            return false;
        return lore.get(0).contains(METADATA_PREFIX);
    }

    public static RPGMetadata parseLoreline(String lore) {
        RPGMetadata meta = new RPGMetadata();
        int pos = lore.indexOf(METADATA_PREFIX);
        if (pos == -1) {
            return meta;
        }
        String lenStr = lore.substring(pos + METADATA_PREFIX.length(), pos + METADATA_PREFIX.length() + 8);
        int length = parseShort(lenStr, 0);
        String data = lore.substring(pos + METADATA_PREFIX.length() + 8, pos + METADATA_PREFIX.length() + 8 + length);
        int off = 0;
        while (off < length) {
            int index = parseByte(data, off);
            off += 4;
            int key = index & 0x1F;
            int type = index >> 5;
            switch(type) {
            case 0: //Byte
                int byteValue = parseByte(data, off);
                off += 4;
                meta.put(key, Byte.valueOf((byte) byteValue));
                break;
            case 1: //Short
                int shortValue = parseShort(data, off);
                off += 8;
                meta.put(key, Short.valueOf((short) shortValue));
                break;
            case 2: //Int
                int intValue = parseInt(data, off);
                off += 16;
                meta.put(key, Integer.valueOf(intValue));
                break;
            case 3: //Float
                int floatValueBits = parseInt(data, off);
                off += 16;
                meta.put(key, Float.intBitsToFloat(floatValueBits));
                break;
            case 4: //String
                int stringLength = parseShort(data, off);
                off += 8;
                byte[] bytes = new byte[stringLength];
                for (int i = 0; i < stringLength; i++) {
                    bytes[i] = (byte) parseByte(data, off);
                    off += 4;
                }
                try {
                    meta.put(key, new String(bytes, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return meta;
    }
    
    private static int parseInt(String iStr, int off) {
        return (parseShort(iStr, off + 0) << 16) | parseShort(iStr, off + 8);
    }
    
    private static int parseShort(String sStr, int off) {
        return (parseByte(sStr, off + 0) << 8) | parseByte(sStr, off + 4);
    }
    
    private static int parseByte(String bStr, int off) {
        return Integer.parseInt("" + bStr.charAt(off + 1) + bStr.charAt(off + 3), 16);
    }

    public RPGMetadata() {

    }

    public String toMCString() {
        StringBuilder out = new StringBuilder();
        out.append(METADATA_PREFIX);
        TIntObjectIterator<Object> it = iterator();
        while (it.hasNext()) {
            it.advance();
            int key = it.key();
            Object value = it.value();
            int index = key & 0x1F;
            if (value instanceof Byte) {
                index |= 0 << 5;
                writeByte(out, index);
                writeByte(out, ((Byte) value).intValue());
            } else if (value instanceof Short) {
                index |= 1 << 5;
                writeByte(out, index);
                writeShort(out, ((Short) value).intValue());
            } else if (value instanceof Integer) {
                index |= 2 << 5;
                writeByte(out, index);
                writeInt(out, ((Integer) value).intValue());
            } else if (value instanceof Float) {
                index |= 3 << 5;
                writeByte(out, index);
                writeInt(out, Float.floatToIntBits((Float) value));
            } else if (value instanceof String) {
                index |= 4 << 5;
                writeByte(out, index);
                writeString(out, (String) value);
            }
        }
        int size = out.length() - METADATA_PREFIX.length();
        insertShort(out, METADATA_PREFIX.length(), size);
        return out.toString();
    }

    private void writeString(StringBuilder out, String value) {
        try {
            byte[] data = value.getBytes("UTF-8");
            writeShort(out, data.length);
            for (byte b : data) {
                writeByte(out, b);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private void writeByte(StringBuilder out, int b) {
        String hex = Integer.toString(b, 16);
        out.append(ChatColor.COLOR_CHAR);
        out.append(hex.length() == 1 ? "0" : hex.charAt(0));
        out.append(ChatColor.COLOR_CHAR);
        out.append(hex.charAt(hex.length() - 1));
    }

    private void writeShort(StringBuilder out, int s) {
        writeByte(out, s >> 8);
        writeByte(out, s & 0xFF);
    }

    private void writeInt(StringBuilder out, int s) {
        writeShort(out, s >> 16);
        writeShort(out, s & 0xFFFF);
    }
    
    private void insertByte(StringBuilder out, int offset, int b) {
        String hex = Integer.toString(b, 16);
        out.insert(offset, ChatColor.COLOR_CHAR);
        out.insert(offset + 1, hex.length() == 1 ? "0" : hex.charAt(0));
        out.insert(offset + 2, ChatColor.COLOR_CHAR);
        out.insert(offset + 3, hex.charAt(hex.length() - 1));
    }

    private void insertShort(StringBuilder out, int offset, int s) {
        insertByte(out, offset, s >> 8);
        insertByte(out, offset + 4, s & 0xFF);
    }

    private void insertInt(StringBuilder out, int offset, int s) {
        insertShort(out, offset, s >> 16);
        insertShort(out, offset + 8, s & 0xFFFF);
    }
}

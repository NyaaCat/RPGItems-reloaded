package think.rpgitems.data;

import java.io.IOException;
import java.io.InputStream;
import think.rpgitems.RPGItems;

public class Font {
    public static int[] widths;

    public static void load() {
        widths = new int[0xFFFF];

        try {
            InputStream in = RPGItems.plugin.getResource("font.bin");
            for (int i = 0; i < widths.length; i++) {
                widths[i] = in.read();
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

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
package think.rpgitems.data;

import java.io.IOException;
import java.io.InputStream;

import think.rpgitems.Plugin;

public class Font {
    public static int[] widths;

    public static void load() {
        widths = new int[0xFFFF];

        try {
            InputStream in = Plugin.plugin.getResource("font.bin");
            for (int i = 0; i < widths.length; i++) {
                widths[i] = in.read();
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

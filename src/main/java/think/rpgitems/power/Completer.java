package think.rpgitems.power;

import java.util.List;

public @interface Completer {
    Class<? extends Getter<List<String>>> completer();
}

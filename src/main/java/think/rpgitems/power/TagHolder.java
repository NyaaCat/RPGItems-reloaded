package think.rpgitems.power;

import java.util.Set;

public interface TagHolder {
    Set<String> getTags();
    void addTag(String tag);
    void removeTag(String tag);
}

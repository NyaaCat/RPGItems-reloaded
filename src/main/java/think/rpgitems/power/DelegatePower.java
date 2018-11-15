package think.rpgitems.power;

import org.apache.commons.lang.NotImplementedException;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.item.RPGItem;

import java.util.Locale;
import java.util.Set;

@PowerMeta
public abstract class DelegatePower<T extends Power> implements Power {

    private final T instance;

    public DelegatePower(T instance) {
        this.instance = instance;
    }

    protected T getInstance() {
        return instance;
    }

    @Override
    public void init(ConfigurationSection s) {
        throw new NotImplementedException();
    }

    @Override
    public void save(ConfigurationSection s) {
        throw new NotImplementedException();
    }

    @Override
    public NamespacedKey getNamespacedKey() {
        throw new NotImplementedException();
    }

    @Override
    public @LangKey(skipCheck = true) String getName() {
        throw new NotImplementedException();
    }

    @Override
    public String getLocalizedName(String locale) {
        throw new NotImplementedException();
    }

    @Override
    public String getLocalizedName(Locale locale) {
        throw new NotImplementedException();
    }

    @Override
    public String displayText() {
        throw new NotImplementedException();
    }

    @Override
    public String localizedDisplayText(String locale) {
        throw new NotImplementedException();
    }

    @Override
    public String localizedDisplayText(Locale locale) {
        throw new NotImplementedException();
    }

    @Override
    public RPGItem getItem() {
        return instance.getItem();
    }

    @Override
    public void setItem(RPGItem item) {
        throw new NotImplementedException();
    }

    @Override
    public Set<Trigger> getTriggers() {
        throw new NotImplementedException();
    }

    @Override
    public Set<String> getSelectors() {
        return instance.getSelectors();
    }

    @Override
    public Set<String> getConditions() {
        return instance.getConditions();
    }

    @Override
    public void deinit() {
        throw new NotImplementedException();
    }

    @Override
    public <TC extends Power> TC cast(Class<TC> powerClass) {
        throw new NotImplementedException();
    }
}

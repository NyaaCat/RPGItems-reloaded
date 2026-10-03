package think.rpgitems.api.firing;

import think.rpgitems.power.Getter;
import think.rpgitems.power.Setter;

import java.util.Optional;

/**
 * Stores a {@link FiringLocation} as its bare name, the same text the former enums produced.
 * Unregistered names are kept as they are, so an item does not lose its configuration when the extension
 * providing the name is missing at load time.
 */
public class FiringLocationSerializer implements Getter<FiringLocation>, Setter<FiringLocation> {
    @Override
    public String get(FiringLocation object) {
        return object.name();
    }

    @Override
    public Optional<FiringLocation> set(String value) throws IllegalArgumentException {
        return Optional.of(FiringLocation.of(value));
    }
}

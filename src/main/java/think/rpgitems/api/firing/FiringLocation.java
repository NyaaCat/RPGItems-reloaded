package think.rpgitems.api.firing;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value of a power's {@code firingLocation} property.
 * <p>
 * {@link #SELF} and {@link #TARGET} are built in and behave exactly as the former per-power enums did.
 * Any other name is an <em>extension</em> location which is resolved, when the power fires, through the
 * {@link FiringLocationProvider} registered under that name in {@link FiringLocations}.
 * <p>
 * Instances are immutable and compare by name, so they can be shared between threads freely.
 */
public final class FiringLocation {
    public static final FiringLocation SELF = new FiringLocation("SELF");
    public static final FiringLocation TARGET = new FiringLocation("TARGET");

    private static final Pattern NAME = Pattern.compile("[A-Z][A-Z0-9_]{0,31}");

    private final String name;

    private FiringLocation(String name) {
        this.name = name;
    }

    /**
     * @param name case-insensitive name, letters, digits and underscores only
     * @return the firing location for that name; the name does not need to be registered (yet)
     * @throws IllegalArgumentException if {@code name} is not a syntactically valid name
     */
    public static FiringLocation of(String name) {
        String normalized = normalize(name);
        if (normalized.equals(SELF.name)) return SELF;
        if (normalized.equals(TARGET.name)) return TARGET;
        return new FiringLocation(normalized);
    }

    static String normalize(String name) {
        if (name == null) throw new IllegalArgumentException("firing location name is null");
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        if (!NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid firing location name: " + name);
        }
        return normalized;
    }

    public String name() {
        return name;
    }

    public boolean isSelf() {
        return name.equals(SELF.name);
    }

    public boolean isTarget() {
        return name.equals(TARGET.name);
    }

    /**
     * @return true if this is neither {@link #SELF} nor {@link #TARGET}
     */
    public boolean isExtension() {
        return !isSelf() && !isTarget();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FiringLocation other && other.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}

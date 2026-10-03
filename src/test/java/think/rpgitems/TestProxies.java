package think.rpgitems;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Function;

/**
 * Minimal stand-ins for Bukkit interfaces. Only the methods listed in {@code answers} respond; everything
 * else fails loudly, so a test cannot pass by accident through behaviour it did not set up.
 */
public final class TestProxies {
    private TestProxies() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T of(Class<T> type, String label, Map<String, Function<Object[], Object>> answers) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return label;
            }
            Function<Object[], Object> answer = answers.get(method.getName());
            if (answer == null) {
                throw new UnsupportedOperationException(label + "." + method.getName() + " is not stubbed");
            }
            return answer.apply(args);
        });
    }
}

package think.rpgitems.utils;

import cat.nyaa.nyaacore.Pair;

import java.util.Map;

public class WeightedPair<K, V> extends Pair<K, V> implements Weightable {
    private final int weight;

    public WeightedPair(K key, V value, int weight) {
        super(key, value);
        this.weight = weight;
    }

    public WeightedPair(Map.Entry<? extends K, ? extends V> entry, int weight) {
        super(entry);
        this.weight = weight;
    }

    @Override
    public int getWeight() {
        return weight;
    }
}

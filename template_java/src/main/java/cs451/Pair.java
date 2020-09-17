package cs451;

import java.util.Objects;

public final class Pair<K, V> {

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new Pair<K, V>(key, value);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj.getClass() != Pair.class)
            return false;

        var other = (Pair) obj;

        return Objects.equals(key, other.key) && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "(" + key + ", " + value + ")";
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    private final K key;
    private final V value;
}

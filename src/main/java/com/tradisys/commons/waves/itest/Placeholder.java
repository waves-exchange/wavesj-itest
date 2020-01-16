package com.tradisys.commons.waves.itest;

import com.wavesplatform.wavesj.DataEntry;

public abstract class Placeholder<T> {
    private final String key;
    private T value;

    public Placeholder(String key, T value) {
        this.key = key;
        this.value = value;
    }

    public abstract DataEntry<?> toDataEntry();

    public String apply(String script) {
        return script.replace(key, value.toString());
    }

    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Placeholder)) return false;

        Placeholder<?> that = (Placeholder<?>) o;

        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
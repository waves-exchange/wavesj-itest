package com.tradisys.commons.waves.itest;

public class Placeholder<T> {
    private final String key;
    private T value;

    public Placeholder(String key, T value) {
        this.key = key;
        this.value = value;
    }

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
}
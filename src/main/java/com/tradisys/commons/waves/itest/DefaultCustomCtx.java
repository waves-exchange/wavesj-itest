package com.tradisys.commons.waves.itest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class DefaultCustomCtx implements BaseJUnitITest.CustomCtx {

    private Map<String, Placeholder<?>> accDataPlaceholders = new HashMap<>();

    public <T extends Number> IntegerPlaceholder intPlaceholder(String key, T val) {
        return intPlaceholder(key, val, false);
    }

    public <T extends Number> IntegerPlaceholder intPlaceholder(String key, T val, boolean keepAsAccData) {
        return placeholder(key, val, keepAsAccData, (k, v) -> new IntegerPlaceholder(key, val));
    }

    public StringPlaceholder strPlaceholder(String key, String val) {
        return strPlaceholder(key, val, false);
    }

    public StringPlaceholder strPlaceholder(String key, String val, boolean keepAsAccData) {
        return placeholder(key, val, keepAsAccData, (k, v) -> new StringPlaceholder(key, val));
    }

    public BooleanPlaceholder booleanPlaceholder(String key, Boolean val, boolean keepAsAccData) {
        return placeholder(key, val, keepAsAccData, (k, v) -> new BooleanPlaceholder(key, val));
    }

    public StringPlaceholder strPlaceholder(StringPlaceholder placeholder, boolean keepAsAccData) {
        return placeholder(placeholder, keepAsAccData);
    }

    public IntegerPlaceholder intPlaceholder(IntegerPlaceholder placeholder, boolean keepAsAccData) {
        return placeholder(placeholder, keepAsAccData);
    }

    private <T extends Placeholder, U> T placeholder(String key, U value, boolean keepAsAccData, BiFunction<String, U, T> buildFunc) {
        T placeholder = buildFunc.apply(key, value);
        addToAccountData(placeholder, keepAsAccData);
        return placeholder;
    }

    private <T extends Placeholder> T placeholder(T placeholder, boolean keepAsAccData) {
        addToAccountData(placeholder, keepAsAccData);
        return placeholder;
    }

    private void addToAccountData(Placeholder<?> placeholder, boolean keepAsAccData) {
        if (keepAsAccData) {
            accDataPlaceholders.put(placeholder.getKey(), placeholder);
        }
    }

    public Collection<Placeholder<?>> getAccDataPlaceholders() {
        return accDataPlaceholders.values();
    }
}

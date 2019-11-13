package com.tradisys.commons.waves.itest;

import com.wavesplatform.wavesj.DataEntry;

public class StringPlaceholder extends Placeholder<String> {

    public StringPlaceholder(String key, String value) {
        super(key, value);
    }

    @Override
    public DataEntry<?> toDataEntry() {
        return new DataEntry.StringEntry(getKey(), getValue());
    }
}

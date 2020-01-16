package com.tradisys.commons.waves.itest;

import com.wavesplatform.wavesj.DataEntry;

public class IntegerPlaceholder extends Placeholder<Number> {

    public IntegerPlaceholder(String key, Number value) {
        super(key, value);
    }

    @Override
    public DataEntry<?> toDataEntry() {
        return new DataEntry.LongEntry(getKey(), getValue().longValue());
    }
}

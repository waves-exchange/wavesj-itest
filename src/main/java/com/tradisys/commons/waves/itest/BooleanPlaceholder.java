package com.tradisys.commons.waves.itest;

import com.wavesplatform.wavesj.DataEntry;

public class BooleanPlaceholder extends Placeholder<Boolean> {

    public BooleanPlaceholder(String key, Boolean value) {
        super(key, value);
    }

    @Override
    public DataEntry<?> toDataEntry() {
        return new DataEntry.BooleanEntry(getKey(), getValue());
    }
}

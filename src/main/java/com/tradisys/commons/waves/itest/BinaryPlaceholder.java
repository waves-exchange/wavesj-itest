package com.tradisys.commons.waves.itest;

import com.wavesplatform.wavesj.ByteString;
import com.wavesplatform.wavesj.DataEntry;

public class BinaryPlaceholder extends Placeholder<ByteString> {

    public BinaryPlaceholder(String key, ByteString value) {
        super(key, value);
    }

    @Override
    public DataEntry<?> toDataEntry() {
        return new DataEntry.BinaryEntry(getKey(), getValue());
    }
}

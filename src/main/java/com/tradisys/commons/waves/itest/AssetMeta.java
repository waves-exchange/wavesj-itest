package com.tradisys.commons.waves.itest;

import com.tradisys.games.server.utils.FormatUtils;

import java.math.BigDecimal;

public class AssetMeta {

    private String name;
    private String assetId;
    private byte decimals;
    private long mult;

    public AssetMeta(String name, String assetId, byte decimals) {
        this.name = name;
        this.assetId = assetId;
        this.decimals = decimals;
        this.mult = (long) Math.pow(10, decimals);
    }

    public String getName() {
        return name;
    }

    public String getAssetId() {
        return assetId;
    }

    public byte getDecimals() {
        return decimals;
    }

    public long getMult() {
        return mult;
    }

    public long toRawAmt(long decAmt) {
        return FormatUtils.toBlkAmount(decAmt, (int) decimals);
    }

    public long toRawAmt(String decAmt) {
        return toRawAmt(new BigDecimal(decAmt));
    }

    public long toRawAmt(BigDecimal decAmt) {
        return FormatUtils.toBlkAmount(decAmt, (int) decimals);
    }

    public BigDecimal toDecAmt(long rawAmt) {
        return FormatUtils.toServerAmount(rawAmt, (int) decimals);
    }
}

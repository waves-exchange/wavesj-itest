package com.tradisys.commons.waves.itest;

import com.wavesplatform.wavesj.Asset;
import com.wavesplatform.wavesj.PublicKeyAccount;

import java.math.BigDecimal;
import java.util.Optional;

public class BalanceAssertion {
    private PublicKeyAccount acc;
    private String assetId;
    private String assertMsg;
    private long expectedAmount;

    public BalanceAssertion(PublicKeyAccount acc, long expectedAmount) {
        this.acc = acc;
        this.expectedAmount = expectedAmount;
    }

    public static BalanceAssertion of(PublicKeyAccount acc, long amt) {
        return new BalanceAssertion(acc, amt);
    }

    public static BalanceAssertion of(PublicKeyAccount acc, String amt) {
        return new BalanceAssertion(acc, Asset.toWavelets(new BigDecimal(amt)));
    }

    public BalanceAssertion withAsset(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public BalanceAssertion withMsg(String msg) {
        this.assertMsg = msg;
        return this;
    }

    public PublicKeyAccount getAcc() {
        return acc;
    }

    public Optional<String> getAssetId() {
        return Optional.ofNullable(assetId);
    }

    public Optional<String> getAssertMsg() {
        return Optional.ofNullable(assertMsg);
    }

    public long getExpectedAmount() {
        return expectedAmount;
    }
}

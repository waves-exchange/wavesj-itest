package com.tradisys.commons.waves.itest;

import com.tradisys.games.server.HttpClientConfig;
import com.tradisys.games.server.exception.BlkChTimeoutException;
import com.tradisys.games.server.integration.*;
import com.tradisys.games.server.utils.DefaultPredicates;
import com.tradisys.games.server.utils.FormatUtils;
import com.wavesplatform.wavesj.*;
import com.wavesplatform.wavesj.transactions.IssueTransaction;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.tradisys.commons.waves.itest.ConfigITest.*;
import static com.tradisys.games.server.utils.FormatUtils.toBlkMoney;
import static com.tradisys.games.server.utils.FormatUtils.toServerMoney;
import static com.wavesplatform.wavesj.PrivateKeyAccount.fromPrivateKey;

public abstract class BaseJUnitITest<CTX extends BaseJUnitITest.CustomCtx> {

    private static final Pattern COMMENT_REGEX = Pattern.compile("#.*");

    private static Map<String, MainContext> ctxByClass = Collections.synchronizedMap(new HashMap<>());
    private Class<CTX> customCtxType;

    public BaseJUnitITest(Class<CTX> customCtxType) {
        this(customCtxType, ConfigITest.ACCOUNT_BYTE);
    }

    public BaseJUnitITest(Class<CTX> customCtxType, byte chainId) {
        this(customCtxType, chainId, null);
    }

    public BaseJUnitITest(Class<CTX> customCtxType, byte chainId, NodeDecorator nodeUrl) {
        this(customCtxType, nodeUrl, chainId, null);
    }

    public BaseJUnitITest(Class<CTX> customCtxType, NodeDecorator node, byte chainId, PrivateKeyAccount benzAcc) {
        this.customCtxType = customCtxType;
        MainContext mainCtx = mainCtx();
        if (mainCtx == null) {
            mainCtx = new MainContext();
            ctxByClass.put(ctxKey(), mainCtx);

            mainCtx.chainId = chainId;
            mainCtx.wavesNode = node != null ? node : getDefaultNode(mainCtx.chainId);
            mainCtx.benzAcc = benzAcc != null ? benzAcc : fromPrivateKey(ConfigITest.BENZ_PRIVATE, chainId);
            mainCtx.cleanup = false;
            mainCtx.logger = LoggerFactory.getLogger(this.getClass());

            getLogger().info("Benz account address: {}", getBenzAcc().getAddress());
        }
    }

    @BeforeEach
    public void init() {
        MainContext mainCtx = mainCtx();
        if (mainCtx.customCtx == null && !mainCtx.customCtxInitError) {
            mainCtx.customCtx = _initCustomCtx();
        }
    }

    private CTX _initCustomCtx() {
        try {
            CTX ctx = initCustomCtx();
            getLogger().info("Custom context key={} hasCode={} has been initialized", ctxKey(), ctx.hashCode());
            return ctx;
        } catch (Exception ex) {
            getLogger().error("Error during custom context initialization", ex);
            mainCtx().customCtxInitError = true;
        }
        return null;
    }

    protected abstract CTX initCustomCtx() throws Exception;

    public final CTX ctx() {
        return customCtxType.cast(mainCtx().customCtx);
    }

    protected static WavesNodeDecorator getDefaultNode(byte chainId) {
        String url = ConfigITest.NODE_URL;
        try {
            return new WavesNodeDecorator(url, chainId, null, HttpClientConfig.getDefault(), null,
                    ConfigITest.NODE_API_AVG_BLOCK_DELAY, ConfigITest.NODE_API_RETRIES, DEFAULT_TIMEOUT, 3);
        } catch (URISyntaxException ex) {
            String msg = String.format("Invalid waves node url in %1$s - %2$s", getITestConfig(), url);
            throw new RuntimeException(msg, ex);
        }
    }

    protected final Logger getLogger() {
        return mainCtx().logger;
    }

    protected final NodeDecorator getNode() {
        return mainCtx().wavesNode;
    }

    protected final byte getChainId() {
        return mainCtx().chainId;
    }

    protected final PrivateKeyAccount getBenzAcc() {
        return mainCtx().benzAcc;
    }

    protected final void setCleanUp(boolean cleanUp) {
        mainCtx().cleanup = true;
    }

    protected final Set<PrivateKeyAccount> _getAllocatedAccounts() {
        return new HashSet<>(mainCtx().allocatedAccounts);
    }

    protected long getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    /**
     * Because of inheritance this methods will be always last
     */
    @Test
    public void n999_enableCleanUp() {
        setCleanUp(true);
    }

    @AfterEach
    public void cleanUp() throws Exception {
        cleanUpAllocatedAccountsBalance();
    }

    protected void cleanUpAllocatedAccountsBalance() throws Exception {
        if (mainCtx().cleanup) {
            if (mainCtx().allocatedAccounts.size() > 0) {
                Thread.sleep(10 * 1000);
                // always use 0.005 fee because - can be improved in future
                for (PrivateKeyAccount acc : mainCtx().allocatedAccounts) {
                    tryToReturnMoney(acc, new BigDecimal("0.005"));
                }
            } else {
                getLogger().info("There were no allocated accounts");
            }
        }
    }

    protected PrivateKeyAccount generateNewAccount(String name, boolean cleanUpAfterTests) {
        String seed = PrivateKeyAccount.generateSeed();
        PrivateKeyAccount acc = PrivateKeyAccount.fromSeed(seed, 0, mainCtx().chainId);
        getLogger().info(name + " generated: address={} public={} private={} seed={}", acc.getAddress(),
                Base58.encode(acc.getPublicKey()),
                Base58.encode(acc.getPrivateKey()), seed);
        if (cleanUpAfterTests) {
            mainCtx().allocatedAccounts.add(acc);
        }
        return acc;
    }

    protected void cleanUpAdditionalAccounts(PrivateKeyAccount ... accounts) {
        cleanUpAdditionalAccounts(Arrays.asList(accounts));
    }

    protected void cleanUpAdditionalAccounts(Collection<PrivateKeyAccount> accounts) {
        mainCtx().allocatedAccounts.addAll(accounts);
    }

    protected PrivateKeyAccount generateNewAccount(String name, boolean cleanUpAfterTests, BigDecimal initAmt)
            throws IOException, InterruptedException {
        PrivateKeyAccount acc = generateNewAccount(name, cleanUpAfterTests);
        String txId = mainCtx().wavesNode.transfer(mainCtx().benzAcc, acc.getAddress(), toBlkMoney(initAmt), toBlkMoney(TRANSFER_FEE), "");
        getLogger().info("Transferring initial funds to {} account: transferTxId={} accAddress={} initAmt={}",
                name, txId, acc.getAddress(), initAmt);
        getNode().waitMoneyOnBalance(acc.getAddress(), BigDecimal.ZERO, initAmt,
                DefaultPredicates.EQUALS, DEFAULT_TIMEOUT);
        return acc;
    }

    protected void tryToReturnMoney(PrivateKeyAccount acc, BigDecimal fee) {
        try {
            if (acc != null) {
                BalanceInfo balance = getNode().getBalanceInfo(acc.getAddress());
                BigDecimal available = toServerMoney(balance.getAvailable());
                getLogger().info("Returning waves: acc={} balance={}", acc.getAddress(), available);
                if (available.compareTo(fee) > 0) {
                    BigDecimal moneyToTransfer = available.subtract(fee);

                    Transaction tx = TransactionsFactory.makeTransferTx(acc, getBenzAcc().getAddress(), toBlkMoney(moneyToTransfer),
                            null, toBlkMoney(fee), null, null);
                    getNode().send(tx);
                }
            }
        } catch (Throwable ex) {
            getLogger().warn("Error to return money from {}: msg={}", acc.getAddress(), ex.getMessage());
        }
    }

    protected void assertWavesBalance(String message, PublicKeyAccount acc, BigDecimal expected) throws IOException {
        assertBalance(message, acc, null, toBlkMoney(expected));
    }

    protected void assertBalance(String message, PublicKeyAccount acc, String assetId, long expected) throws IOException {
        if (Asset.isWaves(assetId)) {
            BalanceInfo balance = getNode().getBalanceInfo(acc.getAddress());
            Assertions.assertEquals(expected, balance.getAvailable(), message);
        } else {
            AssetBalanceInfo balance = getNode().getAssetBalance(acc.getAddress(), assetId);
            Assertions.assertEquals(expected, balance.getBalance(), message);
        }
    }

    protected void assertBalance(BalanceAssertion ... balanceAssertions) throws IOException {
        for (BalanceAssertion b: balanceAssertions) {
            String msg = b.getAssertMsg().orElse(
                    b.getAcc().getAddress() + " must have " + FormatUtils.toServerMoney(b.getExpectedAmount())
                            + " " + b.getAssetId().orElse("Waves"));
            assertBalance(msg, b.getAcc(), b.getAssetId().orElse(null), b.getExpectedAmount());
        }
    }

    public void waitWavesBalance(PublicKeyAccount acc, long expectedRawBalance) throws IOException, InterruptedException {
        waitWavesBalance(acc, expectedRawBalance, getDefaultTimeout());
    }

    public void waitWavesBalance(PublicKeyAccount acc, long expectedRawBalance, long timeout) throws IOException, InterruptedException {
        waitWavesBalance(acc.getAddress(), expectedRawBalance, timeout);
    }

    public void waitWavesBalance(String address, long expectedRawBalance, long timeout) throws IOException, InterruptedException {
        waitAssetBalance(address, expectedRawBalance, null, timeout);
    }

    public void waitAssetBalance(PublicKeyAccount acc, long expectedRawBalance, String assetId) throws IOException, InterruptedException {
        waitAssetBalance(acc, expectedRawBalance, assetId, getDefaultTimeout());
    }

    public void waitAssetBalance(PublicKeyAccount acc, long expectedRawBalance, String assetId, long timeout) throws IOException, InterruptedException {
        waitAssetBalance(acc.getAddress(), expectedRawBalance, assetId, timeout);
    }

    public void waitAssetBalance(String address, long expectedRawBalance, String assetId, long timeout) throws IOException, InterruptedException {
        if (Asset.isWaves(assetId)) {
            whileInState(() -> getBalanceSilently(address),
                    b -> b.getAvailable() != expectedRawBalance, timeout,
                    ob -> ob.ifPresent(b -> getLogger()
                            .warn("Waiting balance for {} was stopped by timeout: actual={} expected={}",
                                    address, b.getAvailable(), expectedRawBalance))
                    );
        } else {
            whileInState(() ->  getAssetBalanceSilently(address, assetId),
                    b -> b.getBalance() != expectedRawBalance, timeout,
                    ob -> ob.ifPresent(b -> getLogger()
                            .warn("Waiting balance for {} was stopped by timeout: actual={} expected={}",
                                    address, b.getBalance(), expectedRawBalance)));
        }
    }

    public static String readScript(String resource, Placeholder<?>[] placeholders) throws IOException {
        String script = readFile(resource, false);
        for (Placeholder<?> placeholder: placeholders) {
            script = placeholder.apply(script);
        }
        return script;
    }

    protected String deployScript(PrivateKeyAccount toAcc, String script) throws IOException, InterruptedException {
        return deployScript(toAcc, script, 0);
    }

    protected IssueTransaction makeIssueTx(PrivateKeyAccount acc, long emission, byte decimals, String prefix, boolean reissuable) {
        prefix = prefix.length() < 10 ? prefix : prefix.substring(0, 10);
        String tokenName = prefix + UUID.randomUUID().toString().substring(0, 6);
        String desc = tokenName + " Test Token";

        return Transactions.makeIssueTx(
                acc, getChainId(), tokenName, desc, emission, decimals, reissuable, null, Fees.WAVES.ISSUE_FEE);
    }

    protected String issueAsset(PrivateKeyAccount acc, long emission, byte decimals, String prefix, boolean reissuable)
            throws IOException {
        IssueTransaction issueTx = makeIssueTx(acc, emission, decimals, prefix, reissuable);
        String txId = getNode().send(issueTx);
        getLogger().info("New {} has been issued: name={} decimals={} id={}",
                issueTx.getDescription(), issueTx.getName(), decimals, txId);
        return txId;
    }

    protected Optional<BalanceInfo> getBalanceSilently(String address) {
        BalanceInfo b = null;
        try {
            b = getNode().getBalanceInfo(address);
        } catch (IOException ex) {
            getLogger().warn("Something wrong during balance read: address={}", address, ex);
        }
        return Optional.ofNullable(b);
    }

    protected Optional<AssetBalanceInfo> getAssetBalanceSilently(String address, String assetId) {
        AssetBalanceInfo b = null;
        try {
            b = getNode().getAssetBalance(address, assetId);
        } catch (IOException ex) {
            getLogger().warn("Something wrong during balance read: address={}", address, ex);
        }
        return Optional.ofNullable(b);
    }

    protected <T> T whileInState(Supplier<Optional<T>> stateSupplier, Predicate<T> loopContinuePredicate, long timeout) throws InterruptedException, IOException {
        return whileInState(stateSupplier, loopContinuePredicate, timeout, o -> {});
    }

    protected <T> T whileInState(Supplier<Optional<T>> stateSupplier, Predicate<T> loopContinuePredicate,
                                 long timeout, Consumer<Optional<T>> timeoutCallback) throws InterruptedException, IOException {
        return whileInState(stateSupplier, loopContinuePredicate, 3, timeout, timeoutCallback);
    }

    protected <T> T whileInState(Supplier<Optional<T>> stateSupplier, Predicate<T> loopContinuePredicate, int confirmations,
                                 long timeout, Consumer<Optional<T>> timeoutCallback) throws InterruptedException, IOException {
        Optional<T> state = Optional.empty();
        long start = System.currentTimeMillis();
        int currConfirmations = 0;

        while (currConfirmations < confirmations) {
            try {
                state = stateSupplier.get();
                if (state.isPresent()) {
                    if (!loopContinuePredicate.test(state.get())) {
                        currConfirmations++;
                    }
                }
            } catch (Exception ex) {
                getLogger().warn("whileInState: error during state read");
            }

            if (System.currentTimeMillis() - start >= timeout) {
                timeoutCallback.accept(state);
                getLogger().error("Waiting state was stopped by timeout");
                throw new BlkChTimeoutException("Waiting state was stopped by timeout");
            }

            if (currConfirmations == 0) {
                Thread.sleep(1000);
            } else {
                Thread.sleep(100);
            }
        }

        return state.get();
    }

    protected String deployScript(PrivateKeyAccount toAcc, String script, long timeout) throws IOException, InterruptedException {
        Assertions.assertNotNull(script, "Script should be not null");
        getLogger().info("Script to be deployed:\n{}", script);

        String txId = getNode().setScript(toAcc, script, getChainId(), toBlkMoney(SCRIPT_SETUP_FEE));
        getLogger().info("Set script txId={}", txId);
        if (timeout > 0) {
            getNode().waitTransaction(txId, timeout);
        } else if (timeout == 0) {
            getNode().waitTransaction(txId, DEFAULT_TIMEOUT);
        }
        return txId;
    }

    public static String readFile(String filePath, boolean cleanComments) throws IOException {
        String content = IOUtils.toString(BaseJUnitITest.class.getClassLoader().getResourceAsStream(filePath));
        if (cleanComments) {
            content = COMMENT_REGEX.matcher(content).replaceAll("");
        }
        return content;
    }

    private String ctxKey() {
        return Integer.toString(this.hashCode());
    }

    private MainContext mainCtx() {
        return ctxByClass.get(ctxKey());
    }

    public interface CustomCtx {}

    public static class EmptyCustomCtx implements CustomCtx {}

    protected static class MainContext {

        private MainContext() {}

        private Logger logger;

        private byte chainId;
        private boolean cleanup;
        private NodeDecorator wavesNode;
        private PrivateKeyAccount benzAcc;
        private Set<PrivateKeyAccount> allocatedAccounts = new HashSet<>(10);

        private boolean customCtxInitError;
        private CustomCtx customCtx;
    }
}
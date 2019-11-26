package com.tradisys.commons.waves.itest;

import com.tradisys.games.server.HttpClientConfig;
import com.tradisys.games.server.exception.BlkChTimeoutException;
import com.tradisys.games.server.integration.*;
import com.tradisys.games.server.utils.DefaultPredicates;
import com.wavesplatform.wavesj.Base58;
import com.wavesplatform.wavesj.PrivateKeyAccount;
import com.wavesplatform.wavesj.Transaction;
import com.wavesplatform.wavesj.Transactions;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.tradisys.commons.waves.itest.ConfigITest.SCRIPT_SETUP_FEE;
import static com.tradisys.commons.waves.itest.ConfigITest.TRANSFER_FEE;
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
                    ConfigITest.NODE_API_AVG_BLOCK_DELAY, ConfigITest.NODE_API_RETRIES);
        } catch (URISyntaxException ex) {
            String msg = String.format("Invalid waves node url in %1$s - %2$s", ConfigITest.FILE_NAME, url);
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

    protected long getDefaultTimeout() {
        return ConfigITest.DEFAULT_TIMEOUT;
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
        if (mainCtx().cleanup) {
            if (mainCtx().allocatedAccounts.size() > 0) {
                Thread.sleep(10 * 1000);
                // always use 0.005 fee because - can be improved in future
                for (PrivateKeyAccount acc : mainCtx().allocatedAccounts) {
                    tryToReturnMoney(acc, new BigDecimal("0.005"));
                }
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

    protected PrivateKeyAccount generateNewAccount(String name, boolean cleanUpAfterTests, BigDecimal initAmt)
            throws IOException, InterruptedException {
        PrivateKeyAccount acc = generateNewAccount(name, cleanUpAfterTests);
        String txId = mainCtx().wavesNode.transfer(mainCtx().benzAcc, acc.getAddress(), toBlkMoney(initAmt), toBlkMoney(TRANSFER_FEE), "");
        getLogger().info("Transferring initial funds to {} account: transferTxId={} accAddress={} initAmt={}",
                name, txId, acc.getAddress(), initAmt);
        getNode().waitMoneyOnBalance(acc.getAddress(), BigDecimal.ZERO, initAmt,
                DefaultPredicates.EQUALS, ConfigITest.DEFAULT_TIMEOUT);
        return acc;
    }

    protected void tryToReturnMoney(PrivateKeyAccount acc, BigDecimal fee) {
        try {
            if (acc != null) {
                BalanceInfo balance = getNode().getBalanceInfo(acc.getAddress());
                BigDecimal available = toServerMoney(balance.getAvailable());
                if (available.compareTo(fee) > 0) {
                    BigDecimal moneyToTransfer = available.subtract(fee);

                    Transaction tx = TransactionsFactory.makeTransferTx(acc, getBenzAcc().getAddress(), toBlkMoney(moneyToTransfer),
                            null, toBlkMoney(fee), null, null);
                    getNode().send(tx);
                }
            }
        } catch (Throwable ex) {
            getLogger().warn("Error to execute money from {}: msg={}", acc.getAddress(), ex.getMessage());
        }
    }

    protected void assertMoneyOnAccount(String message, PrivateKeyAccount acc, BigDecimal expected) throws IOException {
        BalanceInfo balance = getNode().getBalanceInfo(acc.getAddress());
        Assertions.assertEquals(toBlkMoney(expected), balance.getAvailable(), message);
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

    protected <T> T whileInState(Supplier<Optional<T>> stateSupplier, Predicate<T> loopContinuePredicate, long timeout) throws InterruptedException, IOException {
        Optional<T> state = Optional.empty();
        long start = System.currentTimeMillis();

        while (!state.isPresent()
                || loopContinuePredicate.test(state.get())) {
            try {
                state = stateSupplier.get();
            } catch (Exception ex) {
                getLogger().warn("Error during shared state read");
            }
            Thread.sleep(1000);

            if (System.currentTimeMillis() - start >= timeout) {
                getLogger().error("Waiting state was stopped by timeout");
                throw new BlkChTimeoutException("Waiting state was stopped by timeout");
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
            getNode().waitTransaction(txId, ConfigITest.DEFAULT_TIMEOUT);
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
        return this.getClass().getName();
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
        private List<PrivateKeyAccount> allocatedAccounts = new ArrayList<>(10);

        private boolean customCtxInitError;
        private CustomCtx customCtx;
    }
}
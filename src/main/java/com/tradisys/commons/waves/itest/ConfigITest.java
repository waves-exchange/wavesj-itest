package com.tradisys.commons.waves.itest;

import com.tradisys.games.server.integration.NodeDecorator;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Properties;

public class ConfigITest {
    private static final String WAVES_ITEST_CONFIG_PARAM = "wavesITestConfig";
    private static final String FILE_NAME = "itest.properties";

    private static volatile ConfigITest instance;

    private final byte    accountByte;
    private final String  accountByteAlias;
    private final String  nodeUrl;
    private final String  benzPrivate;
    private final long    defaultTimeout;
    private final int     nodeApiRetries;
    private final long    nodeApiAvgBlockDelay;

    public static final BigDecimal TRANSFER_FEE     = new BigDecimal("0.001");
    public static final BigDecimal SCRIPT_SETUP_FEE = new BigDecimal("0.01");
    public static final BigDecimal SCRIPT_TX_FEE    = new BigDecimal("0.005");

    public static ConfigITest get() {
        if (instance == null) {
            synchronized (ConfigITest.class) {
                if (instance == null) {
                    instance = new ConfigITest();
                }
            }
        }
        return instance;
    }

    protected ConfigITest() {
        String configFile = getITestConfigFile();
        Properties props = new Properties();
        InputStream in = ConfigITest.class.getClassLoader().getResourceAsStream(configFile);
        try {
            props.load(in);
            accountByteAlias = props.getProperty("itest.account.byte");
            accountByte = resolveAccountByte(accountByteAlias);
            nodeUrl = props.getProperty("itest.account.node.address");
            benzPrivate = props.getProperty("itest.account.benz.private.key");
            defaultTimeout = Long.parseLong(props.getProperty("itest.timeout.default"));
            nodeApiRetries = Integer.parseInt(props.getProperty("itest.account.node.api.retries"));
            nodeApiAvgBlockDelay = Long.parseLong(props.getProperty("itest.account.node.api.avgBlockDelay"));
            init(props);
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible to find itest configuration file in classpath: filename=" + getITestConfigFile());
        }
    }

    protected void init(Properties props) {
    }

    public static String getITestConfigFile() {
        return System.getProperty(WAVES_ITEST_CONFIG_PARAM, FILE_NAME);
    }

    private static byte resolveAccountByte(String accountByte) {
        switch (accountByte.toLowerCase()) {
            case "testnet": return NodeDecorator.TESTNET;
            case "mainnet": return NodeDecorator.MAINNET;
            case "tradisysnet": return NodeDecorator.TRADISYSNET;
            default: return accountByte.getBytes()[0];
        }
    }

    public byte getAccountByte() {
        return accountByte;
    }

    public String getAccountByteAlias() {
        return accountByteAlias;
    }

    public String getNodeUrl() {
        return nodeUrl;
    }

    public String getBenzPrivate() {
        return benzPrivate;
    }

    public long getDefaultTimeout() {
        return defaultTimeout;
    }

    public int getNodeApiRetries() {
        return nodeApiRetries;
    }

    public long getNodeApiAvgBlockDelay() {
        return nodeApiAvgBlockDelay;
    }
}
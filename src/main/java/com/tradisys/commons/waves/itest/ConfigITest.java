package com.tradisys.commons.waves.itest;

import com.tradisys.games.server.integration.NodeDecorator;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Properties;

public class ConfigITest {
    public static final String FILE_NAME = "itest.properties";

    public static final byte    ACCOUNT_BYTE;
    public static final String  ACCOUNT_BYTE_ALIAS;
    public static final String  NODE_URL;
    public static final String  BENZ_PRIVATE;
    public static final long    DEFAULT_TIMEOUT;

    public static final BigDecimal TRANSFER_FEE     = new BigDecimal("0.001");
    public static final BigDecimal SCRIPT_SETUP_FEE = new BigDecimal("0.01");
    public static final BigDecimal SCRIPT_TX_FEE    = new BigDecimal("0.005");

    static {
        Properties props = new Properties();
        InputStream in = ConfigITest.class.getClassLoader().getResourceAsStream(FILE_NAME);
        try {
            props.load(in);
            ACCOUNT_BYTE_ALIAS = props.getProperty("itest.account.byte");
            ACCOUNT_BYTE = resolveAccountByte(ACCOUNT_BYTE_ALIAS);
            NODE_URL = props.getProperty("itest.account.node.address");
            BENZ_PRIVATE = props.getProperty("itest.account.benz.private.key");
            DEFAULT_TIMEOUT = Long.parseLong(props.getProperty("itest.timeout.default"));
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible to find itest configuration file in classpath: filename=" + FILE_NAME);
        }
    }

    private static byte resolveAccountByte(String accountByte) {
        switch (accountByte.toLowerCase()) {
            case "testnet": return NodeDecorator.TESTNET;
            case "mainnet": return NodeDecorator.MAINNET;
            case "tradisysnet": return NodeDecorator.TRADISYSNET;
            default: return accountByte.getBytes()[0];
        }
    }
}
package com.tradisys.commons.waves.itest;

import com.tradisys.games.server.integration.NodeDecorator;
import com.wavesplatform.wavesj.PrivateKeyAccount;

import javax.annotation.Nonnull;

public class SimpleBaseJUnitITest extends BaseJUnitITest<BaseJUnitITest.EmptyCustomCtx, BaseJUnitITest.EmptyCustomCtx> {

    public SimpleBaseJUnitITest() {
        super(BaseJUnitITest.EmptyCustomCtx.class);
    }

    public SimpleBaseJUnitITest(byte chainId) {
        super(BaseJUnitITest.EmptyCustomCtx.class, chainId);
    }

    public SimpleBaseJUnitITest(byte chainId, NodeDecorator nodeUrl) {
        super(BaseJUnitITest.EmptyCustomCtx.class, chainId, nodeUrl);
    }

    public SimpleBaseJUnitITest(NodeDecorator node, byte chainId, PrivateKeyAccount benzAcc) {
        super(BaseJUnitITest.EmptyCustomCtx.class, node, chainId, benzAcc);
    }

    @Override
    protected @Nonnull EmptyCustomCtx initCustomCtx(EmptyCustomCtx parentCtx) {
        return new EmptyCustomCtx();
    }
}

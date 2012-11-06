package org.agilewiki.jasocket.jid.agent.bounce;

import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jid.collection.flenc.AppJidFactory;
import org.agilewiki.jid.scalar.flens.integer.IntegerJidFactory;

public class BounceAgentFactory extends AppJidFactory {
    public final static BounceAgentFactory fac = new BounceAgentFactory();

    public BounceAgentFactory() {
        super("BounceAgent", IntegerJidFactory.fac);
    }

    @Override
    protected JLPCActor instantiateActor() throws Exception {
        return new BounceAgent();
    }
}

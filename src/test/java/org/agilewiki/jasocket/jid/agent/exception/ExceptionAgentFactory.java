package org.agilewiki.jasocket.jid.agent.exception;

import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jid.collection.flenc.AppJidFactory;

public class ExceptionAgentFactory extends AppJidFactory {
    public final static ExceptionAgentFactory fac = new ExceptionAgentFactory();

    public ExceptionAgentFactory() {
        super("ExceptionAgent");
    }

    @Override
    protected JLPCActor instantiateActor() throws Exception {
        return new ExceptionAgent();
    }
}

package org.agilewiki.jasocket.jid.agent.echo;

import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jid.collection.flenc.AppJidFactory;
import org.agilewiki.jid.scalar.vlens.string.StringJidFactory;

public class EchoAgentFactory extends AppJidFactory {
    public final static EchoAgentFactory fac = new EchoAgentFactory();

    public EchoAgentFactory() {
        super("EchoAgent", StringJidFactory.fac);
    }

    @Override
    protected JLPCActor instantiateActor() throws Exception {
        return new EchoAgent();
    }
}

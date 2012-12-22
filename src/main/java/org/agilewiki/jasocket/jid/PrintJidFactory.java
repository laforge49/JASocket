package org.agilewiki.jasocket.jid;

import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jid.collection.vlenc.BListJidFactory;
import org.agilewiki.jid.scalar.vlens.string.StringJidFactory;

public class PrintJidFactory extends BListJidFactory {
    final public static PrintJidFactory fac = new PrintJidFactory();

    public PrintJidFactory() {
        super(JASocketFactories.PRINT_JID_FACTORY, StringJidFactory.fac);
    }

    @Override
    protected JLPCActor instantiateActor() throws Exception {
        return new PrintJid();
    }
}

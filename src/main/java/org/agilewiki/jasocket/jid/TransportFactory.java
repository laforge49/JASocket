package org.agilewiki.jasocket.jid;

import org.agilewiki.jid.collection.flenc.TupleJidFactory;
import org.agilewiki.jid.scalar.flens.bool.BooleanJidFactory;
import org.agilewiki.jid.scalar.flens.lng.LongJidFactory;
import org.agilewiki.jid.scalar.vlens.actor.ActorJidFactory;

public class TransportFactory extends TupleJidFactory {
    public final static TransportFactory fac = new TransportFactory();

    public TransportFactory() {
        super("transportJid", BooleanJidFactory.fac, LongJidFactory.fac, ActorJidFactory.fac);
    }
}

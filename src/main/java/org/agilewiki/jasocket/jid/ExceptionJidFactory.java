package org.agilewiki.jasocket.jid;

import org.agilewiki.jactor.factory.ActorFactory;
import org.agilewiki.jid.JidFactories;

public class ExceptionJidFactory extends ActorFactory {
    final public static ExceptionJidFactory fac = new ExceptionJidFactory();
    public final static String EXCEPTION_FACTORY = "exceptionJid";

    /**
     * Create a JLPCActorFactory.
     */
    protected ExceptionJidFactory() {
        super(EXCEPTION_FACTORY);
    }

    /**
     * Create a JLPCActor.
     *
     * @return The new actor.
     */
    @Override
    final protected ExceptionJid instantiateActor()
            throws Exception {
        return new ExceptionJid();
    }
}

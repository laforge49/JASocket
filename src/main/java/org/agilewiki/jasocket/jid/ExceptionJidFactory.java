package org.agilewiki.jasocket.jid;

import org.agilewiki.jactor.factory.ActorFactory;
import org.agilewiki.jasocket.JASocketFactories;

public class ExceptionJidFactory extends ActorFactory {
    final public static ExceptionJidFactory fac = new ExceptionJidFactory();

    /**
     * Create a JLPCActorFactory.
     */
    protected ExceptionJidFactory() {
        super(JASocketFactories.EXCEPTION_FACTORY);
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

package org.agilewiki.jasocket;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

public class ProcessException extends Request<Object, ExceptionProcessor> {
    Exception exception;

    public ProcessException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof RawSocket;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((RawSocket) targetActor).processException(exception);
        rp.processResponse(null);
    }
}

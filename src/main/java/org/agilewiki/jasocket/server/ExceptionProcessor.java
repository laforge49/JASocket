package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.lpc.TargetActor;

public interface ExceptionProcessor extends TargetActor {
    public void processException(Exception ex);
}

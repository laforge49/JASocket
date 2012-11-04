package org.agilewiki.jasocket.jid.exception;

import org.agilewiki.jasocket.BytesProtocol;
import org.agilewiki.jasocket.server.SocketManager;

public class ExceptionSocketManager extends SocketManager {
    @Override
    protected BytesProtocol createServerOpened() throws Exception {
        ExceptionEcho exceptionEcho = new ExceptionEcho();
        exceptionEcho.initialize(getMailboxFactory().createAsyncMailbox(), this);
        return exceptionEcho;
    }
}

package org.agilewiki.jasocket.jid.exception;

import org.agilewiki.jasocket.BytesProtocol;
import org.agilewiki.jasocket.server.SocketAcceptor;

public class ExceptionSocketAcceptor extends SocketAcceptor {
    @Override
    protected BytesProtocol createServerOpened() throws Exception {
        ExceptionEcho exceptionEcho = new ExceptionEcho();
        exceptionEcho.initialize(getMailboxFactory().createAsyncMailbox(), this);
        return exceptionEcho;
    }
}
